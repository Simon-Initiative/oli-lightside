/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.cmu.side.recipe;

import edu.cmu.side.RequestException;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.PredictionResult;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * loads a model trained using lightSIDE uses it to label new instances via the
 * web. TODO (maybe): allow classification of multiple instances at once, or by
 * multiple classifiers, upload new trained models (possible?)
 *
 * @author dadamson
 */
public class ServerOLI  extends SimpleChannelInboundHandler<FullHttpRequest> {

    protected static Map<String, Predictor> predictors = new HashMap<String, Predictor>();

    Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private boolean loadingModels = false;

    public ServerOLI() {
        long sleepTimeInMilli = 1000L * 10; // 5 seconds
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleWithFixedDelay(this::pollDirectories, sleepTimeInMilli, sleepTimeInMilli, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        logger.info("The url = " + request.uri());
        String answer = "There is no data, only zuul.";
        HttpResponseStatus status = HttpResponseStatus.OK;
        try {

            String target = request.uri();
            logger.info(request.method() + ": " + target);

            if (target.endsWith("/predict")) {
                answer = handlePredict(request);
            } else if (target.endsWith("/evaluate")) {
                answer = handleEvaluate(request);
            }
            if (answer == null) {
                throw new RequestException(HttpResponseStatus.NOT_FOUND, "There is no data, only zuul.");
            }

        } catch (RequestException e) {
            status = e.getStatus();
            answer = e.getLocalizedMessage();
            e.printStackTrace();
        }

        ByteBuf content = Unpooled.copiedBuffer(answer, CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        ctx.write(response);
        ctx.flush();
    }


    /**
     * @param modelPath
     * @return
     */
    protected  Predictor attachModel(String modelPath) {
        try {
            logger.info("attaching " + modelPath);
            return new Predictor(modelPath, "class");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected String handleEvaluate(FullHttpRequest request) throws RequestException {
        try {

            Map<String, String> attribs = attribs(request);

            String sample = attribs.get("sample").trim();
            String answer = "";
            String model = attribs.get("model").trim();
            logger.info("using model " + model + " on " + sample);

            Predictor predictor = null;
            try {
                predictor = checkModel(model);
            } catch (Exception ex) {
                throw new RequestException(HttpResponseStatus.BAD_REQUEST, ex.getLocalizedMessage());
            }

            PredictionResult prediction = predictor.predict(new DocumentList(sample));
            Map<String, Double> scores = prediction.getDistributionMapForInstance(0);
            for (String label : scores.keySet()) {
                answer += label + ":" + scores.get(label) + ", ";
            }
            answer = answer.trim();

            if (answer.isEmpty()) {
                throw new RequestException(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Empty answer");
            }

            return answer;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RequestException(HttpResponseStatus.BAD_REQUEST, "could not handle request: " + request.uri() +
                    "\n(urls should be of the form /evaluate)");
        }
    }

    private Map<String, String> attribs(FullHttpRequest request){
        HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(request);
        List<InterfaceHttpData> data = postDecoder.getBodyHttpDatas();
        Map<String, String> attribs = new HashMap<String, String>();
        if (data != null) {
            for (InterfaceHttpData datum : data) {
                if (datum.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                    Attribute attribute = (Attribute)datum;
                    try {
                        attribs.put(attribute.getName(), attribute.getString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return attribs;
    }

    protected String handlePredict(FullHttpRequest request) throws RequestException {
        try {
            Map<String, String> attribs = attribs(request);

            String sample = attribs.get("sample").trim();
            String answer = "";
            String model = attribs.get("model").trim();

            logger.info("using model " + model + " on " + sample);
            Predictor predictor = null;
            try {
                predictor = checkModel(model);
            } catch (Exception ex) {
                throw new RequestException(HttpResponseStatus.BAD_REQUEST, ex.getLocalizedMessage());
            }

            List<String> instances = new ArrayList<String>();
            instances.add(sample);
            for (Comparable label : predictor.predict(instances)) {
                answer += label + " ";
            }

            answer = answer.trim();

            if (answer.isEmpty()) {
                throw new RequestException(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Empty answer");
            }

            return answer;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RequestException(HttpResponseStatus.BAD_REQUEST, "could not handle request: " + request.uri() +
                    "\n(urls should be of the form /predict)");
        }
    }

    protected Predictor checkModel(String model) {    // attempt to attach a local model
        File ft = new File(model);
        model = ft.getName();
        if (predictors.containsKey(model)) {
            return predictors.get(model);
        }

        File f = new File("/models", model);
        Predictor attached = null;
        if (f.exists()) {
            attached = attachModel(f.getAbsolutePath());
            if (attached == null) {
                throw new RuntimeException("could not load existing model for '" + model + "' -- was it trained on the latest version of LightSide?");

            }
            predictors.put(model, attached);
        } else {
            logger.info("no model available named " + model);
            throw new RuntimeException("no model available named " + model);
        }
        return attached;
    }

    private void pollDirectories() {
        File modelsFolder = new File("/models");
        if (modelsFolder.exists() && !loadingModels) {
            File[] models = modelsFolder.listFiles();
            loadingModels = true;
            try {
                for (File model : models) {
                    if (model.isFile()) {
                        try {
                            checkModel(model.getName());
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, e.getLocalizedMessage());
                        }
                    }
                }
            } finally {
                loadingModels = false;
            }
        }
    }
}
