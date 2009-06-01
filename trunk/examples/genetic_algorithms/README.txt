SETUP

To compile and run these examples, you will have to download JGAP (http://jgap.sourceforge.net/) and place the jgap.jar file in this directory.

(If you want to use this outside of this directory, you can use Network.jar and XStream.jar.)

I have included build and run shell scripts, but no .bat files.  I leave it to someone else to write those.  When you do delete this line for this readme!

The build and run scripts assume that Simbrain.jar is in the ../dist folder.   If the dist directory does not exist, it can be created using 

EXAMPLE 1: Evolve a neural network

Currently there is one example here, the EvolveNeuralNetwork class.

This  evolves a neural network according to the specifications given in NetworkEvaluationFunction.java.  The default is to evolve a neural network, 85 percent of whose neurons are active.

Quite a bit can be customized, fairly easily.   To get started, try changing the settings in EvolveNeuralNetwork.java and 
NetworkEvaluationFunction.java.
