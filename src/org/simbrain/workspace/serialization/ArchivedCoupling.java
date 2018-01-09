package org.simbrain.workspace.serialization;

import org.simbrain.workspace.Coupling;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Class used to represent a coupling in the archive.
 *
 * @author Matt Watson
 */
@XStreamAlias("ArchivedCoupling")
final class ArchivedCoupling {

    /** The source attribute for the coupling. */
    private final ArchivedAttribute archivedProducer;

    /** The target attribute for the coupling. */
    private final ArchivedAttribute archivedConsumer;

    /**
     * Creates a new instance.
     *
     * @param parent The parent archive.
     * @param coupling The coupling this instance represents.
     */
    ArchivedCoupling(final ArchivedWorkspace parent,
            final Coupling<?> coupling) {

        this.archivedProducer = new ArchivedAttribute(parent, coupling.getProducer());
        this.archivedConsumer = new ArchivedAttribute(parent, coupling.getConsumer());
    }

    /**
     * @return the archivedProducer
     */
    public ArchivedAttribute getArchivedProducer() {
        return archivedProducer;
    }

    /**
     * @return the archivedConsumer
     */
    public ArchivedAttribute getArchivedConsumer() {
        return archivedConsumer;
    }

}