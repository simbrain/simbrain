package org.simbrain.workspace;

import javax.swing.JMenuItem;

/**
 * Packages an object with a jmenu item to make it easy to pass them along
 * through action events.
 *
 */
public class CouplingMenuItem extends JMenuItem {

    /** Refrence to producing attribute. */
    private ProducingAttribute producingAttribute = null;

    /** Reference to consuming attribute. */
    private ConsumingAttribute consumingAttribute = null;

    /** Reference to a coupling container. */
    private CouplingContainer couplingContainer = null;

    /**
     * @param container
     */
    public CouplingMenuItem(final CouplingContainer container) {
        this.couplingContainer = container;
    }

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

    /**
     * @return the container
     */
    public CouplingContainer getCouplingContainer() {
        return couplingContainer;
    }

    /**
     * @param container the container to set
     */
    public void setCouplingContainer(CouplingContainer container) {
        this.couplingContainer = container;
    }

}
