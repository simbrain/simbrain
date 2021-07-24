package org.simbrain.network.matrix;

import org.simbrain.network.core.Layer;
import org.simbrain.network.core.Network;
import org.simbrain.util.NumberKt;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;
import smile.classification.SVM;
import smile.math.kernel.PolynomialKernel;
import smile.math.matrix.Matrix;

import java.awt.geom.Rectangle2D;

public class Classifier extends Layer implements EditableObject {

    /**
     * The classifier object.
     */
    private smile.classification.Classifier classifier;

    // TODO
    private double[][] trainingInputs;
    private int[] targets;


    @UserParameter(label = "Kernel Degree")
    private int kernelDegree = 2;


    private PolynomialKernel kernel = new PolynomialKernel(kernelDegree);

    int result = 0;

    int outputSize;

    Network net;

    /**
     * Collects inputs from other network models using arrays.
     */
    private Matrix inputs;

    /**
     * Construct a classifier.
     */
    public Classifier(Network net, int inputSize, int outputSize) {
        this.net = net;
        inputs = new Matrix(inputSize, 1);
        int initialNumRows = 20;
        trainingInputs = new double[initialNumRows][inputSize];
        targets = new int[initialNumRows];

        this.outputSize = outputSize;

        setLabel(net.getIdManager().getProposedId(Classifier.class));

    }

    public void train(double[][] inputs, int[] targets) {
        try {
            classifier = SVM.fit(inputs,targets, kernel, 1000, 1E-3);

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update() {
        result = classifier.predict(getInputs().col(0));
    }

    public smile.classification.Classifier getClassifier() {
        return classifier;
    }

    public void setClassifier(smile.classification.Classifier classifier) {
        this.classifier = classifier;
    }

    public double[][] getTrainingInputs() {
        return trainingInputs;
    }

    public void setTrainingInputs(double[][] trainingInputs) {
        this.trainingInputs = trainingInputs;
    }

    public int[] getTargets() {
        return targets;
    }

    public void setTargets(int[] targets) {
        this.targets = targets;
    }

    @Override
    public String toString() {
        return "SVM classifier " + getLabel();
    }

    @Override
    public Matrix getInputs() {
        return inputs;
    }

    @Override
    public void addInputs(Matrix newInputs) {
        inputs.add(newInputs);
    }

    @Override
    public Matrix getOutputs() {
        return NumberKt.getOneHot(result, outputSize, 1.0);
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Network getNetwork() {
        return net;
    }

    @Override
    public Rectangle2D getBound() {
        return null;
    }
}
