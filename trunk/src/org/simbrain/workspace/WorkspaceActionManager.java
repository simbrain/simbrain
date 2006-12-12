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

import javax.swing.Action;

import org.simbrain.workspace.actions.*;

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

    /** New console action. */
    private final Action newConsoleAction;

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


        newNetworkAction = new NewNetworkAction(workspace);
        newGaugeAction = new NewGaugeAction(workspace);
        newConsoleAction = new NewConsoleAction(workspace);

        newDataWorldAction = new NewDataWorldAction(workspace);
        newGameWorld2dAction = new NewGameWorld2dAction(workspace);
        newOdorWorldAction = new NewOdorWorldAction(workspace);
        newTextWorldAction = new NewTextWorldAction(workspace);
        newVisionWorldAction = new NewVisionWorldAction(workspace);

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
}
