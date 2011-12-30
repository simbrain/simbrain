package org.simbrain.network.groups;

import org.simbrain.network.interfaces.UpdateAction;

public interface UpdatableGroup {
    
    public boolean getEnabled();

    public void setEnabled(boolean enabled);
    
    public void update();
    
}
