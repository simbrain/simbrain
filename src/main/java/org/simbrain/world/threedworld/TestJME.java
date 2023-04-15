package org.simbrain.world.threedworld;


import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;

import javax.swing.*;
import java.awt.*;

/**
 * Temporary app for learning about JME.
 */
public class TestJME extends SimpleApplication {

    public static void main(String[] args) {

        JFrame window = new JFrame("Swing Application");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new FlowLayout()); // a panel
        // add all your Swing components ...
        panel.add(new JButton("Some Swing Component"));

        AppSettings settings = new AppSettings(true);
        settings.setWidth(640);
        settings.setHeight(480);

        TestJME canvasApplication = new TestJME();
        canvasApplication.setSettings(settings);
        canvasApplication.createCanvas(); // create canvas!
        JmeCanvasContext ctx = (JmeCanvasContext) canvasApplication.getContext();
        ctx.setSystemListener(canvasApplication);
        Dimension dim = new Dimension(640, 480);
        ctx.getCanvas().setPreferredSize(dim);

        // add the JME canvas
        panel.add(ctx.getCanvas());

        window.add(panel);
        window.pack();
        window.setVisible(true);

        canvasApplication.startCanvas();

        // TestJME app = new TestJME();
        // app.start(); // start the game
    }

//    @Override
//    public void simpleInitApp() {
//        Box b = new Box(1, 1, 1); // create cube shape
//        Geometry geom = new Geometry("Box", b);  // create cube geometry from the shape
//        Material mat = new Material(assetManager,
//            "Common/MatDefs/Misc/Unshaded.j3md");  // create a simple material
//        mat.setColor("Color", ColorRGBA.Blue);   // set color of material to blue
//        geom.setMaterial(mat);                   // set the cube's material
//        rootNode.attachChild(geom);              // make the cube appear in the scene
//    }

    @Override
    public void simpleInitApp() {

        /** create a blue box at coordinates (1,-1,1) */
        Box box1 = new Box(1,1,1);
        Geometry blue = new Geometry("Box", box1);
        blue.setLocalTranslation(new Vector3f(1,-1,1));
        Material mat1 = new Material(assetManager,
            "Common/MatDefs/Misc/Unshaded.j3md");
        mat1.setColor("Color", ColorRGBA.Blue);
        blue.setMaterial(mat1);

        /** create a red box straight above the blue one at (1,3,1) */
        Box box2 = new Box(1,1,1);
        Geometry red = new Geometry("Box", box2);
        red.setLocalTranslation(new Vector3f(1,3,1));
        Material mat2 = new Material(assetManager,
            "Common/MatDefs/Misc/Unshaded.j3md");
        mat2.setColor("Color", ColorRGBA.Red);
        red.setMaterial(mat2);

        /** Create a pivot node at (0,0,0) and attach it to the root node */
        Node pivot = new Node("pivot");
        rootNode.attachChild(pivot); // put this node in the scene

        /** Attach the two boxes to the *pivot* node. (And transitively to the root node.) */
        pivot.attachChild(blue);
        pivot.attachChild(red);
        /** Rotate the pivot node: Note that both boxes have rotated! */
        pivot.rotate(.4f,.4f,0f);

        String assetDirectory = "src/main/resources/threedworld/assets";
        getAssetManager().registerLocator(assetDirectory, FileLocator.class);

//        Spatial model = getAssetManager().loadModel("Scenes/BlueRoom.j3o");
//        getRootNode().att

        AmbientLight ambientLight = new AmbientLight();
        ambientLight.setColor(ColorRGBA.White);
        rootNode.addLight(ambientLight);
    }

    @Override
    public void simpleUpdate(float tpf) {
        System.out.println(tpf);
        // make the player rotate:
        ///player.rotate(0, 2*tpf, 0);
    }
}