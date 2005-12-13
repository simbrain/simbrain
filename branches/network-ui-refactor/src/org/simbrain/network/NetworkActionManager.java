
package org.simbrain.network;

import java.util.List;
import java.util.Arrays;

import javax.swing.Action;

import org.simbrain.network.actions.ClearNeuronsAction;
import org.simbrain.network.actions.DeleteNeuronsAction;
import org.simbrain.network.actions.PanBuildModeAction;
import org.simbrain.network.actions.RandomizeObjectsAction;
import org.simbrain.network.actions.RunNetworkAction;
import org.simbrain.network.actions.StopNetworkAction;
import org.simbrain.network.actions.ZoomInBuildModeAction;
import org.simbrain.network.actions.ZoomOutBuildModeAction;
import org.simbrain.network.actions.BuildBuildModeAction;
import org.simbrain.network.actions.SelectionBuildModeAction;
import org.simbrain.network.actions.NetworkToWorldInteractionModeAction;
import org.simbrain.network.actions.WorldToNetworkInteractionModeAction;
import org.simbrain.network.actions.NeitherWayInteractionModeAction;
import org.simbrain.network.actions.BothWaysInteractionModeAction;
import org.simbrain.network.actions.NewNeuronAction;
import org.simbrain.network.actions.SelectAllAction;
import org.simbrain.network.actions.ClearSelectionAction;
import org.simbrain.network.actions.IterateNetworkAction;
import org.simbrain.network.actions.ShowHelpAction;

/**
 * Network action manager.
 */
final class NetworkActionManager {

    /** Pan build mode action. */
    private final Action panBuildModeAction;

    /** Zoom in build mode action. */
    private final Action zoomInBuildModeAction;

    /** Zoom out build mode action. */
    private final Action zoomOutBuildModeAction;

    /** Build build mode action. */
    private final Action buildBuildModeAction;

    /** Selection build mode action. */
    private final Action selectionBuildModeAction;

    /** Network to world interaction mode action. */
    private final Action networkToWorldInteractionModeAction;

    /** World to network interaction mode action. */
    private final Action worldToNetworkInteractionModeAction;

    /** Neither way interaction mode action. */
    private final Action neitherWayInteractionModeAction;

    /** Both ways interaction mode action. */
    private final Action bothWaysInteractionModeAction;

    /** New neuron action. */
    private final Action newNeuronAction;

    /** Delete neurons action. */
    private final Action deleteNeuronsAction;

    /** Clear neurons action. */
    private final Action clearNeuronsAction;

    /** Randomize objects action. */
    private final Action randomizeObjectsAction;

    /** Select all action. */
    private final Action selectAllAction;

    /** Clear selection action. */
    private final Action clearSelectionAction;

    /** Iterate network action. */
    private final Action iterateNetworkAction;

    /** Iterate network action. */
    private final Action runNetworkAction;

    /** Iterate network action. */
    private final Action stopNetworkAction;

    /** Show help action. */
    private final Action showHelpAction;
    

    /**
     * Create a new network action manager for the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    NetworkActionManager(final NetworkPanel networkPanel) {

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        panBuildModeAction = new PanBuildModeAction(networkPanel);
        zoomInBuildModeAction = new ZoomInBuildModeAction(networkPanel);
        zoomOutBuildModeAction = new ZoomOutBuildModeAction(networkPanel);
        buildBuildModeAction = new BuildBuildModeAction(networkPanel);
        selectionBuildModeAction = new SelectionBuildModeAction(networkPanel);

        networkToWorldInteractionModeAction = new NetworkToWorldInteractionModeAction(networkPanel);
        worldToNetworkInteractionModeAction = new WorldToNetworkInteractionModeAction(networkPanel);
        neitherWayInteractionModeAction = new NeitherWayInteractionModeAction(networkPanel);
        bothWaysInteractionModeAction = new BothWaysInteractionModeAction(networkPanel);

        newNeuronAction = new NewNeuronAction(networkPanel);
        deleteNeuronsAction = new DeleteNeuronsAction(networkPanel);
        clearNeuronsAction = new ClearNeuronsAction(networkPanel);
        randomizeObjectsAction = new RandomizeObjectsAction(networkPanel);

        selectAllAction = new SelectAllAction(networkPanel);
        clearSelectionAction = new ClearSelectionAction(networkPanel);

        iterateNetworkAction = new IterateNetworkAction(networkPanel);
        runNetworkAction = new RunNetworkAction(networkPanel);
        stopNetworkAction = new StopNetworkAction(networkPanel);
        
        showHelpAction = new ShowHelpAction();
    }


    /**
     * Return the pan build mode action.
     *
     * @return the pan build mode action
     */
    public Action getPanBuildModeAction() {
        return panBuildModeAction;
    }

    /**
     * Return the zoom in build mode action.
     *
     * @return the zoom in build mode action
     */
    public Action getZoomInBuildModeAction() {
        return zoomInBuildModeAction;
    }

    /**
     * Return the zoom out build mode action.
     *
     * @return the zoom out build mode action
     */
    public Action getZoomOutBuildModeAction() {
        return zoomOutBuildModeAction;
    }

    /**
     * Return the build build mode action.
     *
     * @return the build build mode action
     */
    public Action getBuildBuildModeAction() {
        return buildBuildModeAction;
    }

    /**
     * Return the selection build mode action.
     *
     * @return the selection build mode action
     */
    public Action getSelectionBuildModeAction() {
        return selectionBuildModeAction;
    }

    /**
     * Return the network to world interaction mode action.
     *
     * @return the network to world interaction mode action
     */
    public Action getNetworkToWorldInteractionModeAction() {
        return networkToWorldInteractionModeAction;
    }

    /**
     * Return the world to network interaction mode action.
     *
     * @return the world to network interaction mode action
     */
    public Action getWorldToNetworkInteractionModeAction() {
        return worldToNetworkInteractionModeAction;
    }

    /**
     * Return the neither way interaction mode action.
     *
     * @return the neither way interaction mode action
     */
    public Action getNeitherWayInteractionModeAction() {
        return neitherWayInteractionModeAction;
    }

    /**
     * Return the both ways interaction mode action.
     *
     * @return the both ways interaction mode action
     */
    public Action getBothWaysInteractionModeAction() {
        return bothWaysInteractionModeAction;
    }

    /**
     * Return a list of interaction mode actions.
     *
     * @return a list of interaction mode actions
     */
    public List getInteractionModeActions() {
        return Arrays.asList(new Action[] { bothWaysInteractionModeAction,
                                            networkToWorldInteractionModeAction,
                                            worldToNetworkInteractionModeAction,
                                            neitherWayInteractionModeAction });
    }
    
    /**
     * Return a list of network control actions.
     *
     * @return a list of network control actions
     */
    public List getNetworkControlActions() {
        return Arrays.asList(new Action[] { runNetworkAction,
                                            stopNetworkAction});
    }
    
    /**
     * Return a list of network editing actions.
     *
     * @return a list of network editing actions
     */
    public List getNetworkEditingActions() {
        return Arrays.asList(new Action[] { newNeuronAction,
                                            deleteNeuronsAction});
    }

    /**
     * Return the new neuron action.
     *
     * @return the new neuron action
     */
    public Action getNewNeuronAction() {
        return newNeuronAction;
    }

    /**
     * Return the delete neurons action.
     *
     * @return the delete neurons action
     */
    public Action getDeleteNeuronsAction() {
        return deleteNeuronsAction;
    }

    /**
     * Return the clear neurons action.
     *
     * @return the clear neurons action
     */
    public Action getClearNeuronsAction() {
        return clearNeuronsAction;
    }

    /**
     * Return the randomize objects action.
     *
     * @return the randomize objects action
     */
    public Action getRandomizeObjectsAction() {
        return randomizeObjectsAction;
    }

    
    /**
     * Return the select all action.
     *
     * @return the select all action
     */
    public Action getSelectAllAction() {
        return selectAllAction;
    }

    /**
     * Return the clear selection action.
     *
     * @return the clear selection action
     */
    public Action getClearSelectionAction() {
        return clearSelectionAction;
    }

    /**
     * Return the iterate network action.
     *
     * @return the iterate network action
     */
    public Action getIterateNetworkAction() {
        return iterateNetworkAction;
    }
    
    /**
     * Return the run network action.
     *
     * @return the run network action
     */
    public Action getRunNetworkAction() {
        return runNetworkAction;
    }
    
    /**
     * Return the stop network action.
     *
     * @return the stop network action
     */
    public Action getStopNetworkAction() {
        return stopNetworkAction;
    }

    /**
     * Return the show help action.
     *
     * @return the show help action
     */
    public Action getShowHelpAction() {
        return showHelpAction;
    }
}