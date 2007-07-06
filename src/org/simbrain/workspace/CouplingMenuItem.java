package org.simbrain.workspace;

import javax.swing.JMenuItem;

/**
 * Packages an attribute with a jmenu item to make it easy to pass them along
 * through action events.
 *
 */
public class CouplingMenuItem extends JMenuItem {

    /** Refrence to producing attribute. */
    private ProducingAttribute producingAttribute = null;

    /** Reference to consuming attribute. */
    private ConsumingAttribute consumingAttribute = null;

    /**
     * @param consumingAttribute
     */
    public CouplingMenuItem(final ConsumingAttribute consumingAttribute) {
        super(consumingAttribute.getName());
        this.consumingAttribute = consumingAttribute;
    }

    /**
     * @param producingAttribute
     */
    public CouplingMenuItem(final ProducingAttribute producingAttribute) {
        super(producingAttribute.getName());
        this.producingAttribute = producingAttribute;
    }

    /**
     * @return the consumingAttribute
     */
    public ConsumingAttribute getConsumingAttribute() {
        return consumingAttribute;
    }

    /**
     * @param consumingAttribute the consumingAttribute to set
     */
    public void setConsumingAttribute(final ConsumingAttribute consumingAttribute) {
        this.consumingAttribute = consumingAttribute;
    }

    /**
     * @return the producingAttribute
     */
    public ProducingAttribute getProducingAttribute() {
        return producingAttribute;
    }

    /**
     * @param producingAttribute the producingAttribute to set
     */
    public void setProducingAttribute(final ProducingAttribute producingAttribute) {
        this.producingAttribute = producingAttribute;
    }

}
