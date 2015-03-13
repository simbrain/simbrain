package org.simbrain.workspace.actions;

import javax.swing.AbstractAction;

import org.simbrain.workspace.Workspace;

public abstract class WorkspaceAction extends AbstractAction {
    protected final Workspace workspace;

    protected WorkspaceAction(String name, Workspace workspace) {
        super(name);
        this.workspace = workspace;
    }
}
