#!/bin/bash

#MAXHEAP="4g"
##OTHER_ARGS=""
#OTHER_ARGS="-XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode"

#MAIN_CLASS="edu.cmu.side.recipe.PredictionServerOLI"
MAIN_CLASS="edu.cmu.side.AppServer

CLASSPATH="bin:lib/*:lib/xstream/*:wekafiles/packages/chiSquaredAttributeEval/chiSquaredAttributeEval.jar:wekafiles/packages/bayesianLogisticRegression/bayesianLogisticRegression.jar:wekafiles/packages/LibLINEAR/lib/liblinear-1.8.jar:wekafiles/packages/LibLINEAR/LibLINEAR.jar:wekafiles/packages/LibSVM/lib/libsvm.jar:wekafiles/packages/LibSVM/LibSVM.jar:plugins/genesis.jar"
    
java -classpath $CLASSPATH $MAIN_CLASS $@

