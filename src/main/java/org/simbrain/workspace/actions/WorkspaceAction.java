package org.simbrain.workspace.actions;

import org.simbrain.workspace.Workspace;

import javax.swing.*;

public abstract class WorkspaceAction extends AbstractAction {
    protected final Workspace workspace;

    protected WorkspaceAction(String name, Workspace workspace) {
        super(name);
        this.workspace = workspace;
    }
}
