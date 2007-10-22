package org.simbrain.workspace.gui;

import javax.swing.JMenuItem;

import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Packages an object with a jmenu item to make it easy to pass them along
 * through action events.
 *
 */
public class CouplingMenuItem extends JMenuItem {

    private static final long serialVersionUID = 1L;

    /** Reference to producing attribute. */
    private ProducingAttribute<?> producingAttribute = null;

    /** Reference to consuming attribute. */
    private ConsumingAttribute<?> consumingAttribute = null;

    /** Reference to a coupling container. */
    private WorkspaceComponent component = null;
    
    /**
     * The type of menu item being created.  These items can be used to draw
     * information from a single producer or consumer, or lists of either.
     */
    public enum EventType { SINGLE_PRODUCER, SINGLE_CONSUMER, PRODUCER_LIST, CONSUMER_LIST}

    /** The event type for this event. */
    private final EventType eventType; 
    
    /**
     * @param container
     */
    public CouplingMenuItem(final WorkspaceComponent component, final EventType type) {
        this.component = component;
        this.eventType = type; 
    }

    /**
     * @param consumingAttribute
     */
    public CouplingMenuItem(final ConsumingAttribute<?> consumingAttribute) {
        super(consumingAttribute.getAttributeDescription());
        this.eventType = EventType.SINGLE_CONSUMER;
        this.consumingAttribute = consumingAttribute;
    }

    /**
     * @param producingAttribute
     */
    public CouplingMenuItem(final ProducingAttribute<?> producingAttribute) {
        super(producingAttribute.getAttributeDescription());
        this.eventType = EventType.SINGLE_PRODUCER;
        this.producingAttribute = producingAttribute;
    }

    /**
     * @return the consumingAttribute
     */
    public ConsumingAttribute<?> getConsumingAttribute() {
        return consumingAttribute;
    }

    /**
     * @param consumingAttribute the consumingAttribute to set
     */
    public void setConsumingAttribute(final ConsumingAttribute<?> consumingAttribute) {
        this.consumingAttribute = consumingAttribute;
    }

    /**
     * @return the producingAttribute
     */
    public ProducingAttribute<?> getProducingAttribute() {
        return producingAttribute;
    }

    /**
     * @param producingAttribute the producingAttribute to set
     */
    public void setProducingAttribute(final ProducingAttribute<?> producingAttribute) {
        this.producingAttribute = producingAttribute;
    }

    /**
     * @return the container
     */
    public WorkspaceComponent<?> getWorkspaceComponent() {
        return component;
    }

	/**
	 * @return the eventType
	 */
	public EventType getEventType() {
		return eventType;
	}

}
