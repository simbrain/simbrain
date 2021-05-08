package org.simbrain.network.smile;

import org.simbrain.network.core.Network;
import org.simbrain.network.events.LocationEvents;
import org.simbrain.network.matrix.NeuronArray;
import smile.classification.Classifier;
import smile.classification.SVM;
import smile.math.kernel.PolynomialKernel;

public class SmileClassifier extends NeuronArray {

    /**
     * Event support.
     */
    private transient LocationEvents events = new LocationEvents(this);

    /**
     * The classifier object.
     */
    private Classifier<double[]> classifier;

    /**
     * Construct a neuron array.
     *
     * @param net  parent net
     * @param size number of components in the array
     */
    public SmileClassifier(Network net, int size) {
        super(net, size);
        var kernel = new PolynomialKernel(2);
        // TODO: Add training stuff

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
        var result = classifier.predict(getInputs());
        setOneHot(result == -1 ? 0 : 1);
    }


    @Override
    public String toString() {
        return "SVM classifier " + getLabel();
    }
}
