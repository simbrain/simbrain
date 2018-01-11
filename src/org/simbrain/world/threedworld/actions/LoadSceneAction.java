package org.simbrain.world.threedworld.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.simbrain.world.threedworld.ThreeDWorld;
import org.simbrain.world.threedworld.engine.ThreeDEngine;

/**
 * LoadSceneAction displays a JFileChooser for selecting a j3o
 * scene from the assets directory and on submission loads the
 * scene into the ThreeDWorld.
 */
public class LoadSceneAction extends AbstractAction {
    private static final long serialVersionUID = -1555371103072097299L;

    private ThreeDWorld world;
    private JFileChooser fileChooser;

    /**
     * Construct a new LoadSceneAction.
     * @param world The world in which to load a new scene.
     */
    public LoadSceneAction(ThreeDWorld world) {
        super("Load Scene");
        this.world = world;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("jME3 Geometry", "j3o"));
        fileChooser.setCurrentDirectory(new File(world.getEngine().getAssetDirectory(), "/Scenes"));
        fileChooser.setSelectedFile(new File(world.getScene().getName()));
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            File assetDirectory = new File(world.getEngine().getAssetDirectory());
            String sceneName = assetDirectory.toURI().relativize(file.toURI()).toString();
            if (!sceneName.equals(world.getScene())) {
                world.getEngine().queueState(ThreeDEngine.State.SystemPause, true);
                world.getScene().setName(sceneName);
                world.getScene().load(world.getEngine());
                world.getEngine().queueState(ThreeDEngine.State.RunAll, false);
            }
        }
    }
}
