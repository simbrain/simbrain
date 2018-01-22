package org.simbrain.world.threedworld.actions;

import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import org.simbrain.world.threedworld.ThreeDWorld;

public class ActionManager {
    public static Map<String, AbstractAction> createActions(ThreeDWorld world) {
        Map<String, AbstractAction> actions = new HashMap<String, AbstractAction>();
        putAction(actions, new AddEntityAction(world));
        putAction(actions, new AddBlockAction(world));
        putAction(actions, new AddAgentAction(world));
        putAction(actions, new AddMouseAction(world));
        putAction(actions, new ControlAgentAction(world));
        putAction(actions, new ReleaseAgentAction(world));
        putAction(actions, new EditEntityAction(world, false));
        putAction(actions, new DeleteSelectionAction(world));
        putAction(actions, new LoadSceneAction(world));
        putAction(actions, new EditCameraControllerAction(world));
        putAction(actions, new ToggleUpdateSyncAction(world));
        putAction(actions, new ToggleRunAction(world));
        putAction(actions, new SnapTransformsAction(world));
        putAction(actions, new CameraHomeAction(world));
        putAction(actions, new SelectAllAction(world));
        putAction(actions, new CopySelectionAction(world));
        putAction(actions, new PasteSelectionAction(world));
        return actions;
    }
    
    private static void putAction(Map<String, AbstractAction> actions, AbstractAction action) {
        actions.put((String)action.getValue(AbstractAction.NAME), action);
    }
}
