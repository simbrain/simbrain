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
package org.simbrain.workspace.gui;

import bsh.EvalError;
import bsh.Interpreter;
import org.simbrain.util.Utils;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.actions.*;
import org.simbrain.world.dataworld.DataWorldComponent;
import org.simbrain.world.imageworld.ImageWorldComponent;
import org.simbrain.world.odorworld.OdorWorldComponent;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Workspace action manager contains references to all the actions for a Workspace.
 */
public class WorkspaceActionManager {

    /**
     * Location of script menu directory.
     */
    private static final String SCRIPT_MENU_DIRECTORY = "scripts/scriptmenu";

    private static Action createComponentFactoryAction(Workspace workspace, String name, String icon) {
        return Utils.createAction(name, "Create " + name, icon,
                () -> workspace.getComponentFactory().createWorkspaceComponent(name));
    }

    /**
     * New network action.
     */
    private final Action newNetworkAction;

    /**
     * New console action.
     */
    private final Action newConsoleAction;

    /**
     * New document viewer.
     */
    private final Action newDocViewerAction;

    /**
     * Clear workspace action.
     */
    private final Action clearWorkspaceAction;

    /**
     * Open network action.
     */
    private final Action openNetworkAction;

    /**
     * Open workspace action.
     */
    private final Action openWorkspaceAction;

    /**
     * Save workspace action.
     */
    private final Action saveWorkspaceAction;

    /**
     * Save workspace as action.
     */
    private final Action saveWorkspaceAsAction;

    /**
     * Quit workspace action.
     */
    private final Action quitWorkspaceAction;

    /**
     * Global workspace update action.
     */
    private final Action iterateAction;

    /**
     * Global workspace run action.
     */
    private final Action runAction;

    /**
     * Global workspace stop action.
     */
    private final Action stopAction;

    /**
     * Opens the coupling manager.
     */
    private final Action openCouplingManagerAction;

    /**
     * Opens the coupling list.
     */
    private final Action openCouplingListAction;

    /**
     * Opens the list of workspace components.
     */
    private final Action openWorkspaceComponentListAction;

    /**
     * Show hide property tab.
     */
    private final Action propertyTabAction;

    /**
     * Open script editor action.
     */
    private final Action showScriptEditorAction;

    /**
     * Open script editor action.
     */
    private final Action showUpdaterDialog;

    /**
     * Reposition windows action.
     */
    private final Action repositionAllWindowsAction;

    /**
     * Resize windows action.
     */
    private final Action resizeAllWindowsAction;

    /**
     * List of actions which create charts.
     */
    private List<Action> newChartActions;

    /**
     * List of actions which open worlds.
     */
    private List<Action> openWorldActions;

    /**
     * List of actions which create new worlds.
     */
    private List<Action> newWorldActions;

    /**
     * Create a new workspace action manager for the specified workspace.
     *
     * @param desktop workspace, must not be null
     */
    public WorkspaceActionManager(SimbrainDesktop desktop) {
        Workspace workspace = desktop.getWorkspace();

        clearWorkspaceAction = new ClearWorkspaceAction(desktop);

        openNetworkAction = new OpenNetworkAction(workspace);
        openWorldActions = Arrays.asList(
                new OpenComponentAction<>(DataWorldComponent.class, "Data Table", "Table.png", workspace),
                new OpenComponentAction<>(OdorWorldComponent.class, "Odor World", "SwissIcon.png", workspace),
                new OpenComponentAction<>(ImageWorldComponent.class, "Image Display", "camera.png", workspace));

        openWorkspaceAction = new OpenWorkspaceAction(desktop);
        saveWorkspaceAction = new SaveWorkspaceAction(desktop);
        saveWorkspaceAsAction = new SaveWorkspaceAsAction(desktop);

        newNetworkAction = new NewNetworkAction(workspace);
        newConsoleAction = new NewConsoleAction(workspace);
        newDocViewerAction = new NewDocViewerAction(workspace);

        newChartActions = Arrays.asList(
                createComponentFactoryAction(workspace, "Bar Chart", "BarChart.png"),
                createComponentFactoryAction(workspace, "Histogram", "BarChart.png"),
                createComponentFactoryAction(workspace, "Pie Chart", "PieChart.png"),
                createComponentFactoryAction(workspace, "Projection Plot", "ProjectionIcon.png"),
                createComponentFactoryAction(workspace, "Raster Plot", "ScatterIcon.png"),
                createComponentFactoryAction(workspace, "Time Series", "CurveChart.png"));

        newWorldActions = Arrays.asList(
                createComponentFactoryAction(workspace, "Data Table", "Table.png"),
                createComponentFactoryAction(workspace, "Odor World", "SwissIcon.png"),
                createComponentFactoryAction(workspace, "3D World", "World.png"),
                createComponentFactoryAction(workspace, "Pixel Display", "PaintView.png"),
                createComponentFactoryAction(workspace, "Image World", "photo.png"),
                createComponentFactoryAction(workspace, "Text Display", "Text.png"),
                createComponentFactoryAction(workspace, "Text Reader", "Text.png"));
                //createComponentFactoryAction(workspace, "Device Interaction", "Text.png"));

        quitWorkspaceAction = new QuitWorkspaceAction(desktop);

        iterateAction = new WorkspaceIterateAction(workspace);
        runAction = Utils.createAction("Run", "Run Workspace", "Play.png", workspace::run);
        stopAction = Utils.createAction("Stop", "Stop Workspace", "Stop.png", workspace::stop);

        showScriptEditorAction = new ScriptEditorAction(desktop);
        showUpdaterDialog = new ShowWorkspaceUpdaterDialog(desktop);

        openCouplingManagerAction = new OpenCouplingManagerAction(desktop);
        openCouplingListAction = new OpenCouplingListAction(desktop);
        openWorkspaceComponentListAction = new OpenWorkspaceComponentListAction(desktop);

        propertyTabAction = new PropertyTabAction(desktop);

        repositionAllWindowsAction = new RepositionAllWindowsAction(desktop);
        resizeAllWindowsAction= new ResizeAllWindowsAction(desktop);
    }

    /**
     * Return a list of run control actions.
     */
    public List<Action> getRunControlActions() {
        return Arrays.asList(runAction, stopAction);
    }

    /**
     * @return Open and save workspace actions.
     */
    public List<Action> getOpenSaveWorkspaceActions() {
        return Arrays.asList(openWorkspaceAction, saveWorkspaceAction, saveWorkspaceAsAction);
    }

    /**
     * @return Open worlds actions.
     */
    public List<Action> getOpenWorldActions() {
        return openWorldActions;
    }

    /**
     * @return New worlds actions.
     */
    public List<Action> getNewWorldActions() {
        // These should be in alphabetical order in the resulting menus
        return newWorldActions;
    }

    /**
     * @return Simbrain gauge actions.
     */
    public List<Action> getPlotActions() {
        return newChartActions;
    }

    /**
     * Make a list of script actions by iterating through script menu
     * directory.
     *
     * @param desktop workspace reference
     * @return script action
     */
    public List<ScriptAction> getScriptActions(final SimbrainDesktop desktop) {
        ArrayList<ScriptAction> list = new ArrayList();
        File dir = new File(SCRIPT_MENU_DIRECTORY);
        if (!dir.isDirectory()) {
            return null; // Throw exception instead?
        }
        // TODO: look for other endings and invoke relevant script types
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                continue;
            }
            // TODO: Maybe try sourcing additional files here.  Maybe those in a subdir.
            if (file.getName().endsWith(".bsh")) {
                list.add(new ScriptAction(desktop, file.getName()));
            }
        }
        Collections.sort(list);
        return list;
    }

    public Action getNewNetworkAction() {
        return newNetworkAction;
    }

    public Action getNewConsoleAction() {
        return newConsoleAction;
    }

    public Action getNewDocViewerAction() {
        return newDocViewerAction;
    }

    public Action getClearWorkspaceAction() {
        return clearWorkspaceAction;
    }

    public Action getOpenNetworkAction() {
        return openNetworkAction;
    }

    public Action getShowScriptEditorAction() {
        return showScriptEditorAction;
    }

    public Action getOpenWorkspaceAction() {
        return openWorkspaceAction;
    }

    public Action getSaveWorkspaceAction() {
        return saveWorkspaceAction;
    }

    public Action getQuitWorkspaceAction() {
        return quitWorkspaceAction;
    }

    public Action getIterateAction() {
        return iterateAction;
    }

    public Action getRunAction() {
        return runAction;
    }

    public Action getStopAction() {
        return stopAction;
    }

    public Action getOpenCouplingManagerAction() {
        return openCouplingManagerAction;
    }

    public Action getOpenCouplingListAction() {
        return openCouplingListAction;
    }

    public Action getOpenWorkspaceComponentListAction() {
        return openWorkspaceComponentListAction;
    }

    public Action getPropertyTabAction() {
        return propertyTabAction;
    }

    public Action getShowUpdaterDialog() {
        return showUpdaterDialog;
    }

    public Action getRepositionAllWindowsAction() {
        return repositionAllWindowsAction;
    }
    public Action getResizeAllWindowsAction() {
        return resizeAllWindowsAction;
    }

    /**
     * Create an action based on the name of a script.
     */
    public final class ScriptAction extends WorkspaceAction implements Comparable<ScriptAction> {

        /**
         * Name of script for use in actions (e.g. menu items).
         */
        private String scriptName;

        /**
         * Reference to workspace.
         */
        private Workspace workspace;

        /**
         * Reference to Simbrain Desktop.
         */
        private SimbrainDesktop desktop;

        /**
         * Create a new add gauge action with the specified workspace.
         *
         * @param desktop    Simbrain desktop
         * @param scriptName name of script
         */
        public ScriptAction(final SimbrainDesktop desktop,
                            final String scriptName) {
            super(scriptName, desktop.getWorkspace());
            // putValue(SHORT_DESCRIPTION, name);
            this.scriptName = scriptName;
            this.desktop = desktop;
            this.workspace = desktop.getWorkspace();
        }

        /**
         * @param event
         * @see AbstractAction
         */
        public void actionPerformed(final ActionEvent event) {

            Interpreter interpreter = new Interpreter();

            try {
                interpreter.set("workspace", workspace);
                interpreter.set("desktop", desktop);
                interpreter.source(SCRIPT_MENU_DIRECTORY + '/' + scriptName);
            } catch (FileNotFoundException e) {
                System.out.println("File not found");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("IO Exception");
                e.printStackTrace();
            } catch (EvalError e) {
                System.out.println("Evaluation error");
                e.printStackTrace();
            }
        }

        @Override
        public int compareTo(ScriptAction scriptAction) {
            return this.scriptName.toLowerCase().compareTo(scriptAction.scriptName.toLowerCase());
        }
    }

}
