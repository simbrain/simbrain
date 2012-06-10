package org.simbrain.workspace.actions;

import javax.swing.AbstractAction;

import org.simbrain.workspace.gui.SimbrainDesktop;

public abstract class DesktopAction extends AbstractAction {
    protected final SimbrainDesktop desktop;

    DesktopAction(String name, SimbrainDesktop desktop) {
        super(name);
        this.desktop = desktop;
    }
}
