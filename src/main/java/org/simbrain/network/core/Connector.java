package org.simbrain.network.core;

import org.simbrain.network.NetworkModel;
import org.simbrain.network.events.ConnectorEvents2;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import smile.math.matrix.Matrix;

/**
 * Superclass for classes that inherit from {@link Layer}, i.e. that either produce or consume vectors of values.
 * Dense connectors, i.e. weight matrices, are the most obvious example. More information is in the layer javadocs.
 */
public abstract class Connector extends NetworkModel implements EditableObject, AttributeContainer {

    /**
     * Reference to network this neuron is part of.
     */
    protected final Network parent;

    /**
     * The source for this connector.
     */
    protected Layer source;

    /**
     * The target for this connector.
     */
    protected Layer target;

    /**
     * Event support.
     */
    private transient ConnectorEvents2 events = new ConnectorEvents2();

    /**
     * Whether to render an image of this entity.
     */
    private boolean enableRendering = true;

    /**
     * Construct a connector and initialize events.
     */
    public Connector(Layer source, Layer target, Network net) {
        this.source = source;
        this.target = target;
        this.parent = net;
        initEvents();
    }

    /**
     * Returns the output of this connector
     */
    public abstract Matrix getOutput();

    protected void initEvents() {

        // When the parents of the matrix are deleted, delete the matrix
        source.getEvents().getDeleted().on(null, true, m -> {
            delete();
        });
        target.getEvents().getDeleted().on(null, true, m -> {
            delete();
        });
    }

    @Override
    public void postOpenInit() {
        if (events == null) {
            events = new ConnectorEvents2();
        }
        initEvents();
    }

    @Override
    public void delete() {
        source.removeOutgoingConnector(this);
        target.removeIncomingConnector(this);
        getEvents().getDeleted().fireAndForget(this);
    }

    public Layer getSource() {
        return source;
    }

    public Layer getTarget() {
        return target;
    }

    public boolean isEnableRendering() {
        return enableRendering;
    }

    public void setEnableRendering(boolean enableRendering) {
        this.enableRendering = enableRendering;
    }

    @Override
    public ConnectorEvents2 getEvents() {
        return events;
    }

    /**
     * Helper class for creating new Connectors.
     */
    public static class ConnectorCreator implements EditableObject {

        @UserParameter(label = "Connector type", order = 30)
        private Connector.ConnectorEnum connectorType = ConnectorEnum.DENSE;

        @Override
        public String getName() {
            return "Connector";
        }
    }

    /**
     * Enum for creation dialog.
     */
    public enum ConnectorEnum {
        DENSE {
            @Override
            public String toString() {
                return "Dense matrix";
            }
        },
        ZOE {
            @Override
            public String toString() {
                return "Zoe's template";
            }
        };
    }
}
