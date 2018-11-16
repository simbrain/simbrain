package org.simbrain.workspace;

/**
 * WorkspaceComponentAdapter provides a default null implementation for each of the WorkspaceComponent
 * events. Use this to listen to just a few events.
 */
public class WorkspaceComponentAdapter implements WorkspaceComponentListener {

    @Override
    public void componentUpdated() {
    }

    @Override
    public void guiToggled() {
    }

    @Override
    public void componentOnOffToggled() {
    }

    @Override
    public void componentClosing() {
    }

    @Override
    public void attributeContainerAdded(AttributeContainer addedModel) {
    }

    @Override
    public void attributeContainerRemoved(AttributeContainer removedModel) {
    }

    @Override
    public void attributeContainerChanged(AttributeContainer changedModel) {
    }

}
