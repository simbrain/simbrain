package org.simbrain.network.matrix;

import org.simbrain.network.core.Layer;
import org.simbrain.network.core.Network;
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

    private double[][] trainingInputs = {{0, 0}, {0, 1}, {1, 0}, {1, 1}};

    private int[] targets = {-1, 1, -1, 1};

    /**
     * Construct a neuron array.
     *
     * @param net  parent net
     * @param size number of components in the array
     */
    public Classifier(Network net, int size) {

        var kernel = new PolynomialKernel(2);
        setLabel(net.getIdManager().getProposedId(Classifier.class));

        try {

            var x = new double[][] {{0,0},{0,1},{1,0},{1,1}};
            var y = new int[] {-1,1,1,-1};

            classifier = SVM.fit(x,y, kernel, 1000, 1E-3);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update() {
        // TODO
        var result = classifier.predict(getInputs().col(0));
        // setOneHot(result == -1 ? 0 : 1);
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
        return null;
    }

    @Override
    public void addInputs(Matrix inputs) {

    }

    @Override
    public Matrix getOutputs() {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Network getNetwork() {
        return null;
    }

    @Override
    public Rectangle2D getBound() {
        return null;
    }
}
