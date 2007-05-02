/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005-2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.workspace;

import java.util.Arrays;
import java.util.List;

import javax.swing.Action;

import org.simbrain.workspace.actions.ClearWorkspaceAction;
import org.simbrain.workspace.actions.ExportWorkspaceAction;
import org.simbrain.workspace.actions.ImportWorkspaceAction;
import org.simbrain.workspace.actions.NewConsoleAction;
import org.simbrain.workspace.actions.NewDataWorldAction;
import org.simbrain.workspace.actions.NewGameWorld2dAction;
import org.simbrain.workspace.actions.NewGaugeAction;
import org.simbrain.workspace.actions.NewNetworkAction;
import org.simbrain.workspace.actions.NewOdorWorldAction;
import org.simbrain.workspace.actions.NewTextWorldAction;
import org.simbrain.workspace.actions.NewVisionWorldAction;
import org.simbrain.workspace.actions.NewPlotAction;
import org.simbrain.workspace.actions.OpenDataWorldAction;
import org.simbrain.workspace.actions.OpenGaugeAction;
import org.simbrain.workspace.actions.OpenNetworkAction;
import org.simbrain.workspace.actions.OpenOdorWorldAction;
import org.simbrain.workspace.actions.OpenWorkspaceAction;
import org.simbrain.workspace.actions.QuitWorkspaceAction;
import org.simbrain.workspace.actions.SaveWorkspaceAction;
import org.simbrain.workspace.actions.SaveWorkspaceAsAction;
import org.simbrain.workspace.actions.WorkspaceHelpAction;

/**
 * Workspace action manager.
 *
 * <p>This class contains references to all the actions for
 * a Workspace.</p>
 *
 * <p>These references are contained here instead of in Workspace
 * simply to reduce the amount of code in Workspace.  Most but not
 * all actions hold a reference to the Workspace, passed in via
 * their constructor.</p>
 */
public class WorkspaceActionManager {

    /** New network action. */
    private final Action newNetworkAction;

    /** New odor world action. */
    private final Action newOdorWorldAction;

    /** New data world action. */
    private final Action newDataWorldAction;

    /** New game world 2d action. */
    private final Action newGameWorld2dAction;

    /** New text world action. */
    private final Action newTextWorldAction;

    /** New vision world action. */
    private final Action newVisionWorldAction;

    /** New gauge action. */
    private final Action newGaugeAction;
    
    /** New plot action. */
    private final Action newPlotAction;

    /** New console action. */
    private final Action newConsoleAction;

    /** Clear workspace action. */
    private final Action clearWorkspaceAction;

    /** Export workspace action. */
    private final Action exportWorkspaceAction;

    /** Import workspace action. */
    private final Action importWorkspaceAction;

    /** Open data world action. */
    private final Action openDataWorldAction;

    /** Open gauge action. */
    private final Action openGaugeAction;

    /** Open network action. */
    private final Action openNetworkAction;

    /** Open odor world action. */
    private final Action openOdorWorldAction;

    /** Open workspace action. */
    private final Action openWorkspaceAction;

    /** Save workspace action. */
    private final Action saveWorkspaceAction;

    /** Save workspace as action. */
    private final Action saveWorkspaceAsAction;

    /** Workspace help action. */
    private final Action workspaceHelpAction;

    /** Quit workspace action. */
    private final Action quitWorkspaceAction;

    /** Workspace reference. */
    private final Workspace workspace;

    /**
     * Create a new workspace action manager for the specified
     * workspace.
     *
     * @param workspace workspace, must not be null
     */
    public WorkspaceActionManager(final Workspace workspace) {
        if (workspace == null) {
            throw new IllegalArgumentException("workspace must not be null");
        }

        this.workspace = workspace;

        clearWorkspaceAction = new ClearWorkspaceAction(workspace);

        importWorkspaceAction = new ImportWorkspaceAction(workspace);
        exportWorkspaceAction = new ExportWorkspaceAction(workspace);

        openDataWorldAction = new OpenDataWorldAction(workspace);
        openGaugeAction = new OpenGaugeAction(workspace);
        openNetworkAction = new OpenNetworkAction(workspace);
        openOdorWorldAction = new OpenOdorWorldAction(workspace);

        openWorkspaceAction = new OpenWorkspaceAction(workspace);
        saveWorkspaceAction = new SaveWorkspaceAction(workspace);
        saveWorkspaceAsAction = new SaveWorkspaceAsAction(workspace);

        newNetworkAction = new NewNetworkAction(workspace);
        newGaugeAction = new NewGaugeAction(workspace);
        newConsoleAction = new NewConsoleAction(workspace);

        newDataWorldAction = new NewDataWorldAction(workspace);
        newGameWorld2dAction = new NewGameWorld2dAction(workspace);
        newOdorWorldAction = new NewOdorWorldAction(workspace);
        newTextWorldAction = new NewTextWorldAction(workspace);
        newVisionWorldAction = new NewVisionWorldAction(workspace);
        newPlotAction = new NewPlotAction(workspace);

        workspaceHelpAction = new WorkspaceHelpAction(workspace);

        quitWorkspaceAction = new QuitWorkspaceAction(workspace);
    }

    /**
     * @return Open and save workspace actions.
     */
    public List getOpenSaveWorkspaceActions() {
        return Arrays.asList(new Action[] {openWorkspaceAction,
                                           saveWorkspaceAction,
                                           saveWorkspaceAsAction});
    }

    /**
     * @return Open worlds actions.
     */
    public List getOpenWorldActions() {
        return Arrays.asList(new Action[] {openDataWorldAction,
                                           openOdorWorldAction});
    }

    /**
     * @return New worlds actions.
     */
    public List getNewWorldActions() {
        return Arrays.asList(new Action[] {newDataWorldAction,
                                           newGameWorld2dAction,
                                           newOdorWorldAction,
                                           newTextWorldAction,
                                           newVisionWorldAction});
    }

    /**
     * @return Import/Export workspace actions.
     */
    public List getImportExportActions() {
        return Arrays.asList(new Action[] {importWorkspaceAction,
                                           exportWorkspaceAction});
    }

    /**
     * @return new network action.
     */
    public Action getNewNetworkAction() {
        return newNetworkAction;
    }

    /**
     * @return the newConsoleAction.
     */
    public Action getNewConsoleAction() {
        return newConsoleAction;
    }

    /**
     * @return the newDataWorldAction.
     */
    public Action getNewDataWorldAction() {
        return newDataWorldAction;
    }

    /**
     * @return the newGameWorld2dAction.
     */
    public Action getNewGameWorld2dAction() {
        return newGameWorld2dAction;
    }

    /**
     * @return the newGaugeAction.
     */
    public Action getNewGaugeAction() {
        return newGaugeAction;
    }

    /**
     * @return the newOdorWorldAction.
     */
    public Action getNewOdorWorldAction() {
        return newOdorWorldAction;
    }

    /**
     * @return the newTextWorldAction.
     */
    public Action getNewTextWorldAction() {
        return newTextWorldAction;
    }

    /**
     * @return the newVisionWorldAction.
     */
    public Action getNewVisionWorldAction() {
        return newVisionWorldAction;
    }

    /**
     * @return the clearWorkspaceAction.
     */
    public Action getClearWorkspaceAction() {
        return clearWorkspaceAction;
    }

    /**
     * @return the exportWorkspaceAction.
     */
    public Action getExportWorkspaceAction() {
        return exportWorkspaceAction;
    }

    /**
     * @return the importWorkspaceAction.
     */
    public Action getImportWorkspaceAction() {
        return importWorkspaceAction;
    }

    /**
     * @return the openDataWorldAction.
     */
    public Action getOpenDataWorldAction() {
        return openDataWorldAction;
    }

    /**
     * @return the openGaugeAction.
     */
    public Action getOpenGaugeAction() {
        return openGaugeAction;
    }

    /**
     * @return the openNetworkAction.
     */
    public Action getOpenNetworkAction() {
        return openNetworkAction;
    }

    /**
     * @return the openOdorWorldAction.
     */
    public Action getOpenOdorWorldAction() {
        return openOdorWorldAction;
    }

    /**
     * @return the openWorkspaceAction.
     */
    public Action getOpenWorkspaceAction() {
        return openWorkspaceAction;
    }

    /**
     * @return the saveWorkspaceAction.
     */
    public Action getSaveWorkspaceAction() {
        return saveWorkspaceAction;
    }

    /**
     * @return the saveWorkspaceAsAction.
     */
    public Action getSaveWorkspaceAsAction() {
        return saveWorkspaceAsAction;
    }

    /**
     * @return the workspaceHelpAction.
     */
    public Action getWorkspaceHelpAction() {
        return workspaceHelpAction;
    }

    /**
     * @return the quitWorkspaceAction.
     */
    public Action getQuitWorkspaceAction() {
        return quitWorkspaceAction;
    }

	public Action getNewPlotAction() {
		return newPlotAction;
	}
}
