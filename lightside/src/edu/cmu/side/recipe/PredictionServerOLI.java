/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.cmu.side.recipe;

import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.PredictionResult;
import java.io.File;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.simpleframework.http.Part;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.Server;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * loads a model trained using lightSIDE uses it to label new instances via the
 * web. TODO (maybe): allow classification of multiple instances at once, or by
 * multiple classifiers, upload new trained models (possible?)
 *
 * @author dadamson
 */
public class PredictionServerOLI implements Container {

    protected static Map<String, Predictor> predictors = new HashMap<String, Predictor>();

    private final Executor executor;

    public static void serve(int port, int threads) throws Exception {
        Container container = new PredictionServerOLI(threads);

        Server server = new ContainerServer(container);
        Connection connection = new SocketConnection(server);
        SocketAddress address = new InetSocketAddress(port);

        connection.connect(address);
        System.out.println("Started server on port " + port + ".");
    }

    @Override
    public void handle(final Request request, final Response response) {
        executor.execute(new Runnable() {

            @Override
            public void run() {
                handleRequest(request, response);
            }

        });
    }

    public void handleRequest(Request request, Response response) {
        try {
            PrintStream body = response.getPrintStream();
            long time = System.currentTimeMillis();

            String target = request.getTarget();
            System.out.println(request.getMethod() + ": " + target);

            String answer = null;

            response.setValue("Content-Type", "text/plain");
            response.setValue("Server", "HelloWorld/1.0 (Simple 4.0)");
            response.setDate("Date", time);
            response.setDate("Last-Modified", time);

            if (target.endsWith("/predict")) {
                answer = handlePredict(request, response);
            } else if (target.endsWith("/evaluate")) {
                answer = handleEvaluate(request, response);
            }
            if (answer == null) {
                response.setCode(404);
                System.out.println("There is no data, only zuul.");
                body.println("There is no data, only zuul.");
            } else {
                body.println(answer);
            }

            int code = response.getCode();
            if (code != 200) {
                body.println("HTTP Code " + code);
                System.out.println("HTTP Code " + code);
            }

            body.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PredictionServerOLI(int size) {
        this.executor = Executors.newFixedThreadPool(size);
    }

    protected String handleEvaluate(Request request, Response response) throws IOException {
        try {
            Part part = request.getPart("sample");
            String sample = part.getContent().trim();
            String answer = "";
            part = request.getPart("model");
            String model = part.getContent().trim();

            Predictor predictor = null;
            try {
                predictor = checkModel(model);
            } catch (Exception ex) {
                response.setCode(400);
                return ex.getLocalizedMessage();
            }
            System.out.println("using model " + model + " on " + sample);
            PredictionResult prediction = predictor.predict(new DocumentList(sample));
            Map<String, Double> scores = prediction.getDistributionMapForInstance(0);
            for (String label : scores.keySet()) {
                answer += label + ":" + scores.get(label) + ", ";
            }
            answer = answer.trim();

            if (answer.isEmpty()) {
                response.setCode(500);
            }

            return answer;

        } catch (Exception e) {
            e.printStackTrace();
            response.setCode(400);
            return "could not handle request: " + request.getTarget() + "\n(urls should be of the form /evaluate/)";
        }
    }

    protected String handlePredict(Request request, Response response) throws IOException {
        try {
            Part part = request.getPart("sample");
            String sample = part.getContent().trim();
            String answer = "";
            part = request.getPart("model");
            String model = part.getContent().trim();

            System.out.println("using model " + model + " on " + sample);
            Predictor predictor = null;
            try {
                predictor = checkModel(model);
            } catch (Exception ex) {
                response.setCode(400);
                return ex.getLocalizedMessage();
            }

            List<String> instances = new ArrayList<String>();
            instances.add(sample);
            for (Comparable label : predictor.predict(instances)) {
                answer += label + " ";
            }

            answer = answer.trim();

            if (answer.isEmpty()) {
                response.setCode(500);
            }

            return answer;

        } catch (Exception e) {
            e.printStackTrace();
            response.setCode(400);
            return "could not handle request: " + request.getTarget() + "\n(urls should be of the form /predict)";
        }
    }

    protected Predictor checkModel(String model) {	// attempt to attach a local model
        File ft = new File(model);
        model = ft.getName();
        if(this.predictors.containsKey(model)){
            return predictors.get(model);
        }

        File f = new File("/models", model);
        Predictor attached = null;
        if (f.exists()) {
            attached = attachModel(f.getAbsolutePath());
            if (attached == null) {
//                response.setCode(418);
                throw new RuntimeException("could not load existing model for '" + model + "' -- was it trained on the latest version of LightSide?");

            }
            this.predictors.put(model, attached);
        } else {
//            response.setCode(404);
            System.out.println("no model available named " + model);
            throw new RuntimeException("no model available named " + model);
        }
        return attached;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            printUsage();
        }

//		initSIDE();
        int port = 8000;

        //int start = 0;
        if (args.length > 0 && !args[0].contains(":")) {
            try {
                //start = 1;
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                printUsage();
            }
        }
        serve(port, 5);

    }

    /**
     *
     */
    protected static void printUsage() {
        System.out.println("usage: lightserv [port]");
    }

    /**
     * @param modelPath
     * @return
     */
    protected static Predictor attachModel(String modelPath) {
        try {
            System.out.println("attaching " + modelPath);
            Predictor predict = new Predictor(modelPath, "class");
            //predictors.put(nick, predict);
            return predict;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
