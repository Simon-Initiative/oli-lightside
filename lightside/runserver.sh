#!/bin/bash

#MAXHEAP="4g"
##OTHER_ARGS=""
#OTHER_ARGS="-XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode"

#MAIN_CLASS="edu.cmu.side.recipe.PredictionServerOLI"
MAIN_CLASS="edu.cmu.side.AppServer"

CLASSPATH="bin:lib/*:lib/xstream/*:wekafiles/packages/chiSquaredAttributeEval/chiSquaredAttributeEval.jar:wekafiles/packages/bayesianLogisticRegression/bayesianLogisticRegression.jar:wekafiles/packages/LibLINEAR/lib/liblinear-1.8.jar:wekafiles/packages/LibLINEAR/LibLINEAR.jar:wekafiles/packages/LibSVM/lib/libsvm.jar:wekafiles/packages/LibSVM/LibSVM.jar:plugins/genesis.jar"
    
java -classpath $CLASSPATH  -Dcom.sun.management.jmxremote.ssl=false \
 -Dio.netty.leakDetectionLevel=advanced \
 -Dcom.sun.management.jmxremote.authenticate=false \
 -Dcom.sun.management.jmxremote.port=9010 \
 -Dcom.sun.management.jmxremote.rmi.port=9011 \
 -Djava.rmi.server.hostname=localhost \
 -Dcom.sun.management.jmxremote.local.only=false $MAIN_CLASS $@

