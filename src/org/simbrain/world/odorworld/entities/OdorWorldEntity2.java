package org.simbrain.world.odorworld.entities;

import org.piccolo2d.PNode;
import org.piccolo2d.event.PBasicInputEventHandler;
import org.piccolo2d.event.PInputEvent;
import org.piccolo2d.nodes.PImage;
import org.piccolo2d.nodes.PText;
import org.simbrain.network.gui.nodes.ScreenElement;
import org.simbrain.util.SceneGraphBrowser;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;
import org.simbrain.world.odorworld.resources.OdorWorldResourceManager;

import javax.swing.*;

/**
 * TODO: Separate model and view. But can leave merged if easier for now.
 */
public class OdorWorldEntity2 extends PNode {

    private PImage image;

    private PText sampleText = new PText("Sample");

    public OdorWorldEntity2(String imageName) {

        // TODO: Get rid of "static"?
        this.image = new PImage(OdorWorldResourceManager.getImage("static/" + imageName));
        this.addChild(image);
//        this.setBounds(image.getBounds());
        this.offset(Math.random()*100, Math.random()*100);
        this.addChild(sampleText);
        sampleText.offset(-20,40);

        this.addInputEventListener(new PBasicInputEventHandler() {
            public void mouseClicked(final PInputEvent event) {

                if (event.getClickCount() == 2) {
                    event.setHandled(true);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            System.out.println("Test");
                        }
                    });
                }
            }
        });


    }


}
