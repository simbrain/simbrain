package org.simbrain.network.gui.actions.neuronarray;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.simbrain.network.core.NeuronArray;
import org.simbrain.network.groups.NeuronCollection;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;

public class NeuronArrayConnection implements EditableObject, AttributeContainer {

    private NeuronArrayAdapter source;

    private NeuronArrayAdapter target;

    private INDArray weightMatrix;

    public void init() {
        weightMatrix = Nd4j.create(source.size(), target.size());
        INDArray id = Nd4j.eye(Math.min(source.size(), target.size()));
        weightMatrix.get(NDArrayIndex.createCoveringShape(id.shape())).assign(id);
    }

    public void update() {
        // TODO: only linear rule for now
        target.setActivations(source.getActivations().mmul(weightMatrix));
    }

    public NeuronArrayConnection(NeuronArray source, NeuronArray target) {
        this.source = new NeuronArrayAdapter(source);
        this.target = new NeuronArrayAdapter(target);
        init();
    }

    public NeuronArrayConnection(NeuronArray source, NeuronCollection target) {
        this.source = new NeuronArrayAdapter(source);
        this.target = new NeuronArrayAdapter(target);
        init();
    }

    public NeuronArrayConnection(NeuronArray source, NeuronGroup target) {
        this.source = new NeuronArrayAdapter(source);
        this.target = new NeuronArrayAdapter(target);
        init();
    }

    public NeuronArrayConnection(NeuronCollection source, NeuronArray target) {
        this.source = new NeuronArrayAdapter(source);
        this.target = new NeuronArrayAdapter(target);
        init();
    }

    public NeuronArrayConnection(NeuronCollection source, NeuronCollection target) {
        this.source = new NeuronArrayAdapter(source);
        this.target = new NeuronArrayAdapter(target);
        init();
    }

    public NeuronArrayConnection(NeuronCollection source, NeuronGroup target) {
        this.source = new NeuronArrayAdapter(source);
        this.target = new NeuronArrayAdapter(target);
        init();
    }

    public NeuronArrayConnection(NeuronGroup source, NeuronArray target) {
        this.source = new NeuronArrayAdapter(source);
        this.target = new NeuronArrayAdapter(target);
        init();
    }

    public NeuronArrayConnection(NeuronGroup source, NeuronCollection target) {
        this.source = new NeuronArrayAdapter(source);
        this.target = new NeuronArrayAdapter(target);
        init();
    }

    public NeuronArrayConnection(NeuronGroup source, NeuronGroup target) {
        this.source = new NeuronArrayAdapter(source);
        this.target = new NeuronArrayAdapter(target);
        init();
    }


    public static class NeuronArrayAdapter {

        private NeuronArray neuronArray;

        private NeuronGroup neuronGroup;

        private NeuronCollection neuronCollection;

        NeuronArrayAdapter(NeuronArray neuronArray) {
            this.neuronArray = neuronArray;
        }

        NeuronArrayAdapter(NeuronGroup neuronGroup) {
            this.neuronGroup = neuronGroup;
        }

        NeuronArrayAdapter(NeuronCollection neuronCollection) {
            this.neuronCollection = neuronCollection;
        }

        public INDArray getActivations() {
            if (neuronArray != null) {
                return neuronArray.getNeuronArray();
            } else if (neuronCollection != null) {
                float[] floatActivation = new float[neuronCollection.getActivations().length];
                for (int i = 0; i < neuronCollection.getActivations().length; i++) {
                    floatActivation[i] = (float) neuronCollection.getActivations()[i];
                }
                return Nd4j.create(new int[]{floatActivation.length}, floatActivation);
            } else {
                float[] floatActivation = new float[neuronGroup.getActivations().length];
                for (int i = 0; i < floatActivation.length; i++) {
                    floatActivation[i] = (float) neuronGroup.getActivations()[i];
                }
                return Nd4j.create(new int[]{floatActivation.length}, floatActivation);
            }
        }

        public void setActivations(INDArray activations) {
            if (neuronArray != null) {
                neuronArray.setNeuronArray(activations);
                neuronArray.update();
            } else if (neuronCollection != null) {
                neuronCollection.setActivations(activations.toDoubleVector());
            } else {
                neuronGroup.setActivations(activations.toDoubleVector());
            }
        }

        public long size() {
            if (neuronArray != null) {
                return neuronArray.getNeuronArray().length();
            } else if (neuronCollection != null) {
                return neuronCollection.size();
            } else {
                return neuronGroup.size();
            }
        }

    }

}
