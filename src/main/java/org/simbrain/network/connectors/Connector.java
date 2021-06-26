package org.simbrain.network.connectors;

import org.simbrain.network.NetworkModel;
import org.simbrain.network.core.Network;
import org.simbrain.network.events.ConnectorEvents;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import smile.math.matrix.Matrix;

/**
 * Superclass for classes that inherit from {@link Layer}, i.e.. that either produce or consume
 * vectors of values. Weight matrices are the most obvious examples; they can be used to connect a neuron group or
 * array producing a vector values with another neuron group or array consuming a vector of values.
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
     * The target for this connetor.
     */
    protected Layer target;

    /**
     * Event support.
     */
    private transient ConnectorEvents events = new ConnectorEvents(this);

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
        source.getEvents().onDeleted(m -> {
            delete();
        });
        target.getEvents().onDeleted(m -> {
            delete();
        });
    }

    @Override
    public void postUnmarshallingInit() {
        if (events == null) {
            events = new ConnectorEvents(this);
        }
        initEvents();
    }

    @Override
    public void delete() {
        source.removeOutgoingConnector(this);
        target.removeIncomingConnector(this);
        getEvents().fireDeleted();
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
    public ConnectorEvents getEvents() {
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
