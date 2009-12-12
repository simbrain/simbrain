/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.gui;

import java.util.Arrays;
import java.util.List;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;

import org.simbrain.network.gui.actions.AlignHorizontalAction;
import org.simbrain.network.gui.actions.AlignVerticalAction;
import org.simbrain.network.gui.actions.ClampNeuronsAction;
import org.simbrain.network.gui.actions.ClampWeightsAction;
import org.simbrain.network.gui.actions.WandEditModeAction;
import org.simbrain.network.gui.actions.ZeroSelectedObjectsAction;
import org.simbrain.network.gui.actions.CopyAction;
import org.simbrain.network.gui.actions.CutAction;
import org.simbrain.network.gui.actions.DeleteAction;
import org.simbrain.network.gui.actions.GroupAction;
import org.simbrain.network.gui.actions.IterateNetworkAction;
import org.simbrain.network.gui.actions.NewActorCriticNetworkAction;
import org.simbrain.network.gui.actions.NewBackpropNetworkAction;
import org.simbrain.network.gui.actions.NewCompetitiveNetworkAction;
import org.simbrain.network.gui.actions.NewElmanNetworkAction;
import org.simbrain.network.gui.actions.NewHopfieldNetworkAction;
import org.simbrain.network.gui.actions.NewKwtaNetworkAction;
import org.simbrain.network.gui.actions.NewLMSNetworkAction;
import org.simbrain.network.gui.actions.NewNeuronAction;
import org.simbrain.network.gui.actions.NewSOMNetworkAction;
import org.simbrain.network.gui.actions.NewStandardNetworkAction;
import org.simbrain.network.gui.actions.NewWTANetworkAction;
import org.simbrain.network.gui.actions.PanEditModeAction;
import org.simbrain.network.gui.actions.PasteAction;
import org.simbrain.network.gui.actions.RandomizeObjectsAction;
import org.simbrain.network.gui.actions.RunNetworkAction;
import org.simbrain.network.gui.actions.SelectAllAction;
import org.simbrain.network.gui.actions.SelectAllNeuronsAction;
import org.simbrain.network.gui.actions.SelectAllWeightsAction;
import org.simbrain.network.gui.actions.SelectIncomingWeightsAction;
import org.simbrain.network.gui.actions.SelectOutgoingWeightsAction;
import org.simbrain.network.gui.actions.SelectionEditModeAction;
import org.simbrain.network.gui.actions.SetAutoZoomAction;
import org.simbrain.network.gui.actions.SetNeuronPropertiesAction;
import org.simbrain.network.gui.actions.SetSourceNeuronsAction;
import org.simbrain.network.gui.actions.SetSynapsePropertiesAction;
import org.simbrain.network.gui.actions.SetTextPropertiesAction;
import org.simbrain.network.gui.actions.ShowClampToolBarAction;
import org.simbrain.network.gui.actions.ShowDebugAction;
import org.simbrain.network.gui.actions.ShowEditToolBarAction;
import org.simbrain.network.gui.actions.ShowGUIAction;
import org.simbrain.network.gui.actions.ShowHelpAction;
import org.simbrain.network.gui.actions.ShowIOInfoAction;
import org.simbrain.network.gui.actions.ShowMainToolBarAction;
import org.simbrain.network.gui.actions.ShowNetworkPreferencesAction;
import org.simbrain.network.gui.actions.ShowWeightsAction;
import org.simbrain.network.gui.actions.SpaceHorizontalAction;
import org.simbrain.network.gui.actions.SpaceVerticalAction;
import org.simbrain.network.gui.actions.StopNetworkAction;
import org.simbrain.network.gui.actions.TextEditModeAction;
import org.simbrain.network.gui.actions.UngroupAction;
import org.simbrain.network.gui.actions.ZoomEditModeAction;
import org.simbrain.network.gui.actions.connection.ShowConnectDialogAction;

/**
 * Network action manager.
 * 
 * <p>
 * This class contains references to all the actions for a NetworkPanel. In some
 * cases, related actions are grouped together, see e.g.
 * <code>getNetworkModeActions()</code>.
 * </p>
 * 
 * <p>
 * These references are contained here instead of in NetworkPanel simply to
 * reduce the amount of code in NetworkPanel. Most but not all actions hold a
 * reference to the NetworkPanel, passed in via their constructor.
 * </p>
 * 
 * Wait, no, not just for that? Also so there is just one instance that can be
 * shared? That way changes to the action are reflected everywhere... right?
 * 
 */
public final class NetworkActionManager {

    /** Pan edit mode action. */
    private final Action panEditModeAction;

    /** Zoom in edit mode action. */
    private final Action zoomInEditModeAction;

    /** Selection edit mode action. */
    private final Action selectionEditModeAction;

    /** Text edit mode action. */
    private final Action textEditModeAction;

    /** New neuron action. */
    private final Action newNeuronAction;

    /** Clear neurons action. */
    private final Action zeroSelectedObjectsAction;

    /** Randomize objects action. */
    private final Action randomizeObjectsAction;

    /** Select all action. */
    private final Action selectAllAction;

    /** Clear action. */
    private final Action deleteAction;

    /** Copy action. */
    private final Action copyAction;

    /** Cut action. */
    private final Action cutAction;

    /** Paste action. */
    private final Action pasteAction;

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

    /** Clamp neurons action. */
    private final Action clampNeuronsAction;

    /** Show IO information action. */
    private final Action showIOInfoAction;

    /** Set auto zoom action. */
    private final Action setAutoZoomAction;

    /** Set neuron properties action. */
    private final Action setNeuronPropertiesAction;

    /** Set synapse properties action. */
    private final Action setSynapsePropertiesAction;

    /** Select all weights action. */
    private final Action selectAllWeightsAction;

    /** Select all neurons action. */
    private final Action selectAllNeuronsAction;

    /** New actor critic action. */
    private final Action newActorCriticNetworkAction;

    /** New backprop network action. */
    private final Action newBackpropNetworkAction;

    /** New competitive network action. */
    private final Action newCompetitiveNetworkAction;

    /** New elman network action. */
    private final Action newElmanNetworkAction;

    /** New discrete hopfield network action. */
    private final Action newHopfieldNetworkAction;

    /** New LMS network action. */
    private final Action newLMSNetworkAction;

    /** New winner take all network action. */
    private final Action newWTANetworkAction;

    /** New Self-organizing Map network action. */
    private final Action newSOMNetworkAction;

    /** New standard network action. */
    private final Action newStandardNetworkAction;

    /** New Kwta network action. */
    private final Action newKwtaNetworkAction;

    /** Determines if main tool bar is to be shown. */
    private final Action showMainToolBarAction;

    /** Determines if edit tool bar is to be shown. */
    private final Action showEditToolBarAction;

    /** Determines if clamp tool bar is to be shown. */
    private final Action showClampToolBarAction;

    /** Sets the source neurons for neuron connections. */
    private Action setSourceNeuronsAction;

    /**
     * Sets the GUI to be used while running networks. Note that the action is
     * wrapped in CheckBoxMenuItem.
     */
    private final JCheckBoxMenuItem showGUIAction;

    /** Sets the nodes to be shown in GUI. Note that the action is wrapped in CheckBoxMenuItem. */
    private final JCheckBoxMenuItem showNodesAction;

    /** Select all incoming synapses. */
    private final Action selectIncomingWeightsAction;

    /** Select all outgoing synapses. */
    private final Action selectOutgoingWeightsAction;

    /** Show the connect neurons dialog. */
    private final Action showConnectDialogAction;

    /** Sets the text object properties. */
    private final Action setTextPropertiesAction;

    /** Groups selected nodes together. */
    private final Action groupAction;

    /** Ungroups selected nodes. */
    private final Action ungroupAction;

    /** Reference to NetworkPanel. */
    private final NetworkPanel networkPanel;


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

        this.networkPanel = networkPanel;

        panEditModeAction = new PanEditModeAction(networkPanel);
        zoomInEditModeAction = new ZoomEditModeAction(networkPanel);
        selectionEditModeAction = new SelectionEditModeAction(networkPanel);
        textEditModeAction = new TextEditModeAction(networkPanel);

        newNeuronAction = new NewNeuronAction(networkPanel);
        zeroSelectedObjectsAction = new ZeroSelectedObjectsAction(networkPanel);
        randomizeObjectsAction = new RandomizeObjectsAction(networkPanel);

        selectAllAction = new SelectAllAction(networkPanel);

        deleteAction = new DeleteAction(networkPanel);
        copyAction = new CopyAction(networkPanel);
        cutAction = new CutAction(networkPanel);
        pasteAction = new PasteAction(networkPanel);

        iterateNetworkAction = new IterateNetworkAction(networkPanel);
        runNetworkAction = new RunNetworkAction(networkPanel);
        stopNetworkAction = new StopNetworkAction(networkPanel);

        showHelpAction = new ShowHelpAction();
        showDebugAction = new ShowDebugAction(networkPanel);

        showNetworkPreferencesAction = new ShowNetworkPreferencesAction(networkPanel);

        alignVerticalAction = new AlignVerticalAction(networkPanel);
        alignHorizontalAction = new AlignHorizontalAction(networkPanel);
        spaceVerticalAction = new SpaceVerticalAction(networkPanel);
        spaceHorizontalAction = new SpaceHorizontalAction(networkPanel);

        clampNeuronsAction = new ClampNeuronsAction(networkPanel);
        clampWeightsAction = new ClampWeightsAction(networkPanel);

        showMainToolBarAction = new ShowMainToolBarAction(networkPanel);
        showEditToolBarAction = new ShowEditToolBarAction(networkPanel);
        showClampToolBarAction = new ShowClampToolBarAction(networkPanel);
        ShowGUIAction theShowGUIAction = new ShowGUIAction(networkPanel);
        showGUIAction = new JCheckBoxMenuItem(theShowGUIAction);
        ShowWeightsAction theShowNodesAction = new ShowWeightsAction(networkPanel);
        showNodesAction = new JCheckBoxMenuItem(theShowNodesAction);

        showIOInfoAction = new ShowIOInfoAction(networkPanel);
        setAutoZoomAction = new SetAutoZoomAction(networkPanel);

        selectAllWeightsAction = new SelectAllWeightsAction(networkPanel);
        selectAllNeuronsAction = new SelectAllNeuronsAction(networkPanel);
        selectIncomingWeightsAction = new SelectIncomingWeightsAction(networkPanel);
        selectOutgoingWeightsAction = new SelectOutgoingWeightsAction(networkPanel);

        setNeuronPropertiesAction = new SetNeuronPropertiesAction(networkPanel);
        setSynapsePropertiesAction = new SetSynapsePropertiesAction(networkPanel);
        setTextPropertiesAction = new SetTextPropertiesAction(networkPanel);

        newActorCriticNetworkAction = new NewActorCriticNetworkAction(networkPanel);
        newBackpropNetworkAction = new NewBackpropNetworkAction(networkPanel);
        newCompetitiveNetworkAction = new NewCompetitiveNetworkAction(networkPanel);
        newElmanNetworkAction = new NewElmanNetworkAction(networkPanel);
        newHopfieldNetworkAction = new NewHopfieldNetworkAction(networkPanel);
        newLMSNetworkAction = new NewLMSNetworkAction(networkPanel);
        newWTANetworkAction = new NewWTANetworkAction(networkPanel);
        newSOMNetworkAction = new NewSOMNetworkAction(networkPanel);
        newStandardNetworkAction = new NewStandardNetworkAction(networkPanel);
        newKwtaNetworkAction = new NewKwtaNetworkAction(networkPanel);

        showConnectDialogAction = new ShowConnectDialogAction(networkPanel);

        groupAction = new GroupAction(networkPanel);
        ungroupAction = new UngroupAction(networkPanel, networkPanel.getViewGroupNode());
    }


    /**
     * Return the text edit mode action.
     *
     * @return the text edit mode action
     */
    public Action getTextEditModeAction() {
        return textEditModeAction;
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
     * Return a list of network mode actions.
     *
     * @return a list of network mode actions
     */
    public List<Action> getNetworkModeActions() {
        return Arrays.asList(new Action[] {zoomInEditModeAction,
                                           panEditModeAction,
                                           selectionEditModeAction,
                                           textEditModeAction,
                                           new WandEditModeAction(networkPanel)});
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
     * Return clipboard actions.
     *
     * @return a list of clipboard actions
     */
    public List<Action> getClipboardActions() {
        return Arrays.asList(new Action[] {copyAction, cutAction, pasteAction});
    }

    /**
     * Return a list of network editing actions.
     *
     * @return a list of network editing actions
     */
    public List<Action> getNetworkEditingActions() {
        return Arrays.asList(new Action[] {newNeuronAction, deleteAction });
    }

    /**
     * @return a list of the network types.
     */
    public List getNewNetworkActions() {
        return Arrays.asList(new Action[] {newActorCriticNetworkAction,
                newBackpropNetworkAction, newCompetitiveNetworkAction,
                newElmanNetworkAction, newHopfieldNetworkAction,
                newKwtaNetworkAction, newLMSNetworkAction, newSOMNetworkAction,
                newStandardNetworkAction, newWTANetworkAction });
    }

//    public List<JToggleButton> getClampBarActions() {
//        return Arrays.asList(new JToggleButton[] {getClampNeuronsBarItem(), getClampWeightsBarItem()});
//    }

    /**
     * Return the new neuron action.
     *
     * @return the new neuron action
     */
    public Action getNewNeuronAction() {
        return newNeuronAction;
    }

    /**
     * Return the clear neurons action.
     *
     * @return the clear neurons action
     */
    public Action getZeroSelectedObjectsAction() {
        return zeroSelectedObjectsAction;
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
     * Return the clear action.
     *
     * @return the clear action
     */
    public Action getDeleteAction() {
        return deleteAction;
    }

    /**
     * Return the copy action.
     *
     * @return the copy action
     */
    public Action getCopyAction() {
        return copyAction;
    }

    /**
     * Return the cut action.
     *
     * @return the cut action
     */
    public Action getCutAction() {
        return cutAction;
    }

    /**
     * Return the paste action.
     *
     * @return the paste action
     */
    public Action getPasteAction() {
        return pasteAction;
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
     * Return the show IO information check box menu item.
     *
     * @return the show IO information check box menu item
     */
    public JCheckBoxMenuItem getShowIOInfoMenuItem() {
        // TODO: Creating the wrapper in the getter is problematic.  Remove where it occurs. 
        JCheckBoxMenuItem actionWrapper = new JCheckBoxMenuItem(showIOInfoAction);
        actionWrapper.setSelected(networkPanel.getInOutMode());
        return actionWrapper;
    }

    /**
     * Return the set auto zoom check box menu item.
     *
     * @return the set auto zoom check box menu item
     */
    public JCheckBoxMenuItem getSetAutoZoomMenuItem() {
        JCheckBoxMenuItem actionWrapper = new JCheckBoxMenuItem(setAutoZoomAction);
        actionWrapper.setSelected(networkPanel.getInOutMode());
        return actionWrapper;
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
     * @return the select all weights action.
     */
    public Action getSelectAllWeightsAction() {
        return selectAllWeightsAction;
    }

    /**
     * Return the new actor critic action.
     *
     * @return the new actor critic action
     */
    public Action getNewActorCriticNetworkAction() {
        return newActorCriticNetworkAction;
    }

    /**
     * Return the new backprop network action.
     *
     * @return the new backprop network action
     */
    public Action getNewBackpropNetworkAction() {
        return newBackpropNetworkAction;
    }

    /**
     * Return the new competitive network action.
     *
     * @return the new competitive network action
     */
    public Action getNewCompetitiveNetworkAction() {
        return newCompetitiveNetworkAction;
    }

    /**
     * Return the new elman network action.
     *
     * @return the new elman network action
     */
    public Action getNewElmanNetworkAction() {
        return newElmanNetworkAction;
    }

    /**
     * Return the new discrete hopfield network action.
     *
     * @return the new discrete hopfield network action
     */
    public Action getNewHopfieldNetworkAction() {
        return newHopfieldNetworkAction;
    }

    /**
     * Return the new winner take all network action.
     *
     * @return the new winner take all network action
     */
    public Action getNewWTANetworkAction() {
        return newWTANetworkAction;
    }

    /**
     * Return the new LMSNetwork network action.
     *
     * @return the new LMSNetwork network action
     */
    public Action getNewLMSNetworkAction() {
        return newLMSNetworkAction;
    }

    /**
     * Return the new SOM network action.
     *
     * @return the new SOM network action.
     */
    public Action getNewSOMNetworkAction() {
        return newSOMNetworkAction;
        }

    /**
     * Return the clamp neurons action.
     *
     * @return the clamp neurons action
     */
    public Action getClampNeuronsAction() {
        return clampNeuronsAction;
    }

    /**
     * Return the standard network action.
     *
     * @return the standard network action
     */
    public Action getNewStandardNetworkAction() {
        return newStandardNetworkAction;
    }

    /**
     * Return the K Winner Take All network action.
     *
     * @return the K Winner Take All network action
     */
    public Action getNewKwtaNetworkAction() {
        return newKwtaNetworkAction;
    }

    /**
     * Return the show edit tool bar menu item.
     *
     * @return the show edit tool bar menu item
     */
    public JCheckBoxMenuItem getShowEditToolBarMenuItem() {
        JCheckBoxMenuItem actionWrapper = new JCheckBoxMenuItem(showEditToolBarAction);
        actionWrapper.setSelected(networkPanel.getEditToolBar().isVisible());
        return actionWrapper;
    }

    /**
     * Return the show main tool bar menu item.
     *
     * @return the show main tool bar menu item
     */
    public JCheckBoxMenuItem getShowMainToolBarMenuItem() {
        JCheckBoxMenuItem actionWrapper = new JCheckBoxMenuItem(showMainToolBarAction);
        actionWrapper.setSelected(networkPanel.getMainToolBar().isVisible());
        return actionWrapper;
    }

    /**
     * Return the show clamp tool bar menu item.
     *
     * @return the show clamp tool bar menu item
     */
    public JCheckBoxMenuItem getShowClampToolBarMenuItem() {
        JCheckBoxMenuItem actionWrapper = new JCheckBoxMenuItem(showClampToolBarAction);
        actionWrapper.setSelected(networkPanel.getMainToolBar().isVisible());
        return actionWrapper;
    }


    /**
     * Return the set source neurons action.
     *
     * @return set source neurons action
     */
    public Action getSetSourceNeuronsAction() {
        setSourceNeuronsAction = new SetSourceNeuronsAction(networkPanel);
        return setSourceNeuronsAction;
    }


    /**
     * Return the show GUI action.
     *
     * TODO: This is not an action.
     *
     * @return show GUI action
     */
    public JCheckBoxMenuItem getShowGUIAction() {
        showGUIAction.setSelected(networkPanel.isGuiOn());
        return showGUIAction;
    }


    /**
     * @return the showNodesAction
     */
    public JCheckBoxMenuItem getShowNodesAction() {
        showNodesAction.setSelected(networkPanel.isSynapseNodesOn());
        return showNodesAction;
    }


    /**
     * @return the selectIncomingWeightsAction
     */
    public Action getSelectIncomingWeightsAction() {
        return selectIncomingWeightsAction;
    }


    /**
     * @return the selectOutgoingWeightsAction
     */
    public Action getSelectOutgoingWeightsAction() {
        return selectOutgoingWeightsAction;
    }


    /**
     * @return the showConnectDialogAction.
     */
    public Action getShowConnectDialogAction() {
        return showConnectDialogAction;
    }


    /**
     * @return the setTextPropertiesAction.
     */
    public Action getSetTextPropertiesAction() {
        return setTextPropertiesAction;
    }


	/**
	 * @return the ungroupAction
	 */
	public Action getUngroupAction() {
		return ungroupAction;
	}


	/**
	 * @return the groupAction
	 */
	public Action getGroupAction() {
		return groupAction;
	}
}
