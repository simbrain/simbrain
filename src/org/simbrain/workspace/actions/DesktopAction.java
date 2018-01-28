package org.simbrain.workspace.actions;

import org.simbrain.workspace.gui.SimbrainDesktop;

import javax.swing.*;

public abstract class DesktopAction extends AbstractAction {
    protected final SimbrainDesktop desktop;

    DesktopAction(String name, SimbrainDesktop desktop) {
        super(name);
        this.desktop = desktop;
    }
}
