package org.simbrain.world.threedworld.entities;

import org.simbrain.workspace.AttributeContainer;

public interface Effector extends AttributeContainer {
    String getName();

    Agent getAgent();

    void update(float tpf);

    void delete();

    EffectorEditor getEditor();
}
