package org.simbrain.workspace;

/**
 * WorkspaceComponentAdapter provides a default null implementation for each of the WorkspaceComponent
 * events. Use this to listen to just a few events.
 */
public class WorkspaceComponentAdapter implements WorkspaceComponentListener {
    public void componentUpdated() {}

    public void guiToggled() {}

    public void componentOnOffToggled() {}

    public void componentClosing() {}

    public void modelAdded(Object addedModel) {}

    public void modelRemoved(Object removedModel) {}

    public void modelChanged(Object changedModel) {}

}
