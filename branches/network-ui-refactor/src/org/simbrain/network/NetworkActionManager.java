
package org.simbrain.network;

import java.util.List;
import java.util.Arrays;

import javax.swing.Action;

import org.simbrain.network.actions.*;

/**
 * Network action manager.
 */
final class NetworkActionManager {

    /** Pan edit mode action. */
    private final Action panEditModeAction;

    /** Zoom in edit mode action. */
    private final Action zoomInEditModeAction;

    /** Zoom out edit mode action. */
    private final Action zoomOutEditModeAction;

    /** Build edit mode action. */
    private final Action buildEditModeAction;

    /** Selection edit mode action. */
    private final Action selectionEditModeAction;

    /** Network to world interaction mode action. */
    private final Action networkToWorldInteractionModeAction;

    /** World to network interaction mode action. */
    private final Action worldToNetworkInteractionModeAction;

    /** Neither way interaction mode action. */
    private final Action neitherWayInteractionModeAction;

    /** Both ways interaction mode action. */
    private final Action bothWaysInteractionModeAction;

    /** Increment objects up. */
    private final Action incrementUpAction;

    /** Increment objects down. */
    private final Action incrementDownAction;

    /** New neuron action. */
    private final Action newNeuronAction;

    /** Delete neurons action. */
    private final Action deleteSelectedObjectsAction;

    /** Clear neurons action. */
    private final Action clearNeuronsAction;

    /** Randomize objects action. */
    private final Action randomizeObjectsAction;

    /** Select all action. */
    private final Action selectAllAction;

    /** Clear selection action. */
    private final Action clearSelectionAction;

    /** Copy selected objects action. */
    private final Action copySelectedObjectsAction;

    /** Paste objects action. */
    private final Action pasteObjectsAction;

    /** Iterate network action. */
    private final Action iterateNetworkAction;

    /** Run network action. */
    private final Action runNetworkAction;

    /** Stop network action. */
    private final Action stopNetworkAction;

    /** Show help action. */
    private final Action showHelpAction;

    /** Show debug. */
    private final Action showDebugAction;

    /** Show network preferences action. */
    private final Action showNetworkPreferencesAction;

    /** Open network action. */
    private final Action openNetworkAction;

    /** Save network action. */
    private final Action saveNetworkAction;

    /** Save as network action. */
    private final Action saveAsNetworkAction;

    /** Close network action. */
    private final Action closeNetworkAction;

    /** Align vertical action. */
    private final Action alignVerticalAction;

    /** Align horizontal action. */
    private final Action alignHorizontalAction;

    /** Space vertical action. */
    private final Action spaceVerticalAction;

    /** Space horizontal action. */
    private final Action spaceHorizontalAction;

    /** Clamp weights action. */
    private final Action clampWeightsAction;

    /** Show IO information action. */
    private final Action showIOInfoAction;

    /** Set auto zoom action. */
    private final Action setAutoZoomAction;

    /** Set show subnet outline action. */
    private final Action setShowSubnetOutlineAction;

    /** Set neuron properties action. */
    private final Action setNeuronPropertiesAction;

    /** Set synapse properties action. */
    private final Action setSynapsePropertiesAction;

    /** Select all weights action. */
    private final Action selectAllWeightsAction;

    /** Select all neurons action. */
    private final Action selectAllNeuronsAction;

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

        panEditModeAction = new PanEditModeAction(networkPanel);
        zoomInEditModeAction = new ZoomInEditModeAction(networkPanel);
        zoomOutEditModeAction = new ZoomOutEditModeAction(networkPanel);
        buildEditModeAction = new BuildEditModeAction(networkPanel);
        selectionEditModeAction = new SelectionEditModeAction(networkPanel);

        networkToWorldInteractionModeAction = new NetworkToWorldInteractionModeAction(networkPanel);
        worldToNetworkInteractionModeAction = new WorldToNetworkInteractionModeAction(networkPanel);
        neitherWayInteractionModeAction = new NeitherWayInteractionModeAction(networkPanel);
        bothWaysInteractionModeAction = new BothWaysInteractionModeAction(networkPanel);

        incrementUpAction = new IncrementUpAction(networkPanel);
        incrementDownAction = new IncrementDownAction(networkPanel);

        newNeuronAction = new NewNeuronAction(networkPanel);
        deleteSelectedObjectsAction = new DeleteSelectedObjects(networkPanel);
        clearNeuronsAction = new ClearNeuronsAction(networkPanel);
        randomizeObjectsAction = new RandomizeObjectsAction(networkPanel);

        selectAllAction = new SelectAllAction(networkPanel);
        clearSelectionAction = new ClearSelectionAction(networkPanel);

        copySelectedObjectsAction = new CopySelectedObjectsAction(networkPanel);
        pasteObjectsAction = new PasteObjectsAction(networkPanel);

        iterateNetworkAction = new IterateNetworkAction(networkPanel);
        runNetworkAction = new RunNetworkAction(networkPanel);
        stopNetworkAction = new StopNetworkAction(networkPanel);

        showHelpAction = new ShowHelpAction();
        showDebugAction = new ShowDebugAction(networkPanel);

        showNetworkPreferencesAction = new ShowNetworkPreferencesAction(networkPanel);

        openNetworkAction = new OpenNetworkAction(networkPanel);
        saveNetworkAction = new SaveNetworkAction(networkPanel);
        saveAsNetworkAction = new SaveAsNetworkAction(networkPanel);
        closeNetworkAction = new CloseNetworkAction(networkPanel);

        alignVerticalAction = new AlignVerticalAction(networkPanel);
        alignHorizontalAction = new AlignHorizontalAction(networkPanel);
        spaceVerticalAction = new SpaceVerticalAction(networkPanel);
        spaceHorizontalAction = new SpaceHorizontalAction(networkPanel);

        clampWeightsAction = new ClampWeightsAction(networkPanel);

        showIOInfoAction = new ShowIOInfoAction(networkPanel);
        setAutoZoomAction = new SetAutoZoomAction(networkPanel);
        setShowSubnetOutlineAction = new SetShowSubnetOutlineAction(networkPanel);

        selectAllWeightsAction = new SelectAllWeightsAction(networkPanel);
        selectAllNeuronsAction = new SelectAllNeuronsAction(networkPanel);
        setNeuronPropertiesAction = new SetNeuronPropertiesAction(networkPanel);
        setSynapsePropertiesAction = new SetSynapsePropertiesAction(networkPanel);
    }


    /**
     * Return the pan edit mode action.
     *
     * @return the pan edit mode action
     */
    public Action getPanEditModeAction() {
        return panEditModeAction;
    }

    /**
     * Return the zoom in edit mode action.
     *
     * @return the zoom in edit mode action
     */
    public Action getZoomInEditModeAction() {
        return zoomInEditModeAction;
    }

    /**
     * Return the zoom out edit mode action.
     *
     * @return the zoom out edit mode action
     */
    public Action getZoomOutEditModeAction() {
        return zoomOutEditModeAction;
    }

    /**
     * Return the build edit mode action.
     *
     * @return the build edit mode action
     */
    public Action getBuildEditModeAction() {
        return buildEditModeAction;
    }

    /**
     * Return the selection edit mode action.
     *
     * @return the selection edit mode action
     */
    public Action getSelectionEditModeAction() {
        return selectionEditModeAction;
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
        return Arrays.asList(new Action[] {bothWaysInteractionModeAction,
                                            networkToWorldInteractionModeAction,
                                            worldToNetworkInteractionModeAction,
                                            neitherWayInteractionModeAction });
    }

    /**
     * Return a list of network mode actions.
     *
     * @return a list of network mode actions
     */
    public List getNetworkModeActions() {
        return Arrays.asList(new Action[] {zoomInEditModeAction,
                                           zoomOutEditModeAction,
                                           panEditModeAction,
                                           buildEditModeAction,
                                           selectionEditModeAction });
    }

    /**
     * Return a list of network control actions.
     *
     * @return a list of network control actions
     */
    public List getNetworkControlActions() {
        return Arrays.asList(new Action[] {runNetworkAction,
                                           stopNetworkAction });
    }

    /**
     * Return open and save actions.
     *
     * @return a list of open / save actions
     */
    public List getOpenCloseActions() {
        return Arrays.asList(new Action[] {openNetworkAction,
                                           saveNetworkAction,
                                           saveAsNetworkAction});
    }
    
    /**
     * Return clipboard actions.
     *
     * @return a list of clipboard actions
     */
    public List getClipboardActions() {
        return Arrays.asList(new Action[] {copySelectedObjectsAction,
                                           pasteObjectsAction});
    }

    /**
     * Return a list of network editing actions.
     *
     * @return a list of network editing actions
     */
    public List getNetworkEditingActions() {
        return Arrays.asList(new Action[] {newNeuronAction,
                                           deleteSelectedObjectsAction });
    }

    /**
     * Return the increment objects up action.
     *
     * @return the increment objects up action
     */
    public Action getIncrementUpAction() {
        return incrementUpAction;
    }

    /**
     * Return the increment objects down action.
     *
     * @return the increment objects down action
     */
    public Action getIncrementDownAction() {
        return incrementDownAction;
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
    public Action getDeleteSelectedObjectsAction() {
        return deleteSelectedObjectsAction;
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
     * @return Returns the showDebugAction.
     */
    public Action getShowDebugAction() {
        return showDebugAction;
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

    /**
     * Return the show network preferences action.
     *
     * @return the network preferences action
     */
    public Action getShowNetworkPreferencesAction() {
        return showNetworkPreferencesAction;
    }


    /**
     * Return the open network action.
     *
     * @return the open network action
     */
    public Action getOpenNetworkAction() {
        return openNetworkAction;
    }


    /**
     * Return the save as network action.
     *
     * @return the save as network action
     */
    public Action getSaveAsNetworkAction() {
        return saveAsNetworkAction;
    }


    /**
     * Return the save network action.
     *
     * @return the save network action
     */
    public Action getSaveNetworkAction() {
        return saveNetworkAction;
    }


    /**
     * Return the close network action.
     *
     * @return the close network action
     */
    public Action getCloseNetworkAction() {
        return closeNetworkAction;
    }


    /**
     * Return the copy selected objects action.
     *
     * @return the copy selected objects action
     */
    public Action getCopySelectedObjectsAction() {
        return copySelectedObjectsAction;
    }


    /**
     * Return the paste objects action.
     *
     * @return the paste objects action.
     */
    public Action getPasteObjectsAction() {
        return pasteObjectsAction;
    }


    /**
     * Return the align horizontal action.
     *
     * @return the align horizontal action
     */
    public Action getAlignHorizontalAction() {
        return alignHorizontalAction;
    }


    /**
     * Return the align vertical action.
     *
     * @return the align vertical action
     */
    public Action getAlignVerticalAction() {
        return alignVerticalAction;
    }


    /**
     * Return the space horizontal action.
     *
     * @return the space horizontal action
     */
    public Action getSpaceHorizontalAction() {
        return spaceHorizontalAction;
    }


    /**
     * Return the space vertical action.
     *
     * @return the space vertical action
     */
    public Action getSpaceVerticalAction() {
        return spaceVerticalAction;
    }


    /**
     * Return the clamp weight action.
     *
     * @return the clamp weight action
     */
    public Action getClampWeightsAction() {
        return clampWeightsAction;
    }


    /**
     * Return the show IO information action.
     *
     * @return the show IO information action
     */
    public Action getShowIOInfoAction() {
        return showIOInfoAction;
    }


    /**
     * Return the set auto zoom action.
     *
     * @return the set auto zoom action
     */
    public Action getSetAutoZoomAction() {
        return setAutoZoomAction;
    }


    /**
     * Return the show subnet outline action.
     *
     * @return the show subnet outline action
     */
    public Action getSetShowSubnetOutlineAction() {
        return setShowSubnetOutlineAction;
    }


    /**
     * Return the neuron properties action.
     *
     * @return the neuron properties action
     */
    public Action getSetNeuronPropertiesAction() {
        return setNeuronPropertiesAction;
    }


    /**
     * Return the synapse properties action.
     *
     * @return the synapse properties action
     */
    public Action getSetSynapsePropertiesAction() {
        return setSynapsePropertiesAction;
    }


    /**
     * Return the select all neurons action.
     *
     * @return the select all neurons action
     */
    public Action getSelectAllNeuronsAction() {
        return selectAllNeuronsAction;
    }


    /**
     * Return the select all weights action.
     *
     * @return the select all weights action.\
     */
    public Action getSelectAllWeightsAction() {
        return selectAllWeightsAction;
    }
}