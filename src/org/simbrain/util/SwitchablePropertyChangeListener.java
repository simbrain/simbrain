package org.simbrain.util;

import java.beans.PropertyChangeListener;

public abstract class SwitchablePropertyChangeListener implements
        PropertyChangeListener {

    private boolean enabled = true;

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
