package org.simbrain.workspace.updater;

import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.gui.WorkspaceDelayActionDialog;

/**
 * SynchronizedTaskUpdateAction executes AWT-driven events which may have been queued during the workspace update.
 */
public class WorkspaceDelayAction implements UpdateAction {
    private Workspace workspace;

    public WorkspaceDelayAction(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public String getDescription() {
        return "Workspace Update Delay";
    }

    @Override
    public String getLongDescription() {
        return "Delays workspace updates by a fixed duration per timestep.";
    }

    @Override
    public void invoke() {
        if (workspace.getUpdateDelay() > 0) {
            try {
                Thread.sleep(workspace.getUpdateDelay());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void showDialog() {
        WorkspaceDelayActionDialog dialog = new WorkspaceDelayActionDialog(workspace);
        dialog.setVisible(true);
    }
}
