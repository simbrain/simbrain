package org.simbrain.custom_sims.helper_classes;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.desktop.NetworkDesktopComponent;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.NeuronNode;

/**
 * A {@link NetworkWrapper}, with additional access to GUI stuff, e.g.
 * org.simbrain.network.gui.NetworkPanel} which can be used to set the position of a window in the Simbrain desktop.
 */
// TODO: Consider removing this
@Deprecated
public class NetworkDesktopWrapper extends NetworkWrapper {

    /**
     * The desktop component with full graphical access.
     */
    private NetworkDesktopComponent desktopComponent;

    /**
     * Create an instance of the wrapper.
     */
    public NetworkDesktopWrapper(NetworkDesktopComponent desktopComponent) {
        super(desktopComponent.getWorkspaceComponent());
        this.desktopComponent = desktopComponent;
    }

    /**
     * Get a reference to a graphical {@link NeuronNode}.
     */
    public NeuronNode getNode(Neuron neuron) {
        // TODO
        return null;
    }

    @Deprecated
    public NetworkPanel getNetworkPanel() {
        return desktopComponent.getNetworkPanel();
    }


}
