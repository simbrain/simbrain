package org.simbrain.world.odorworld.gui;

import org.piccolo2d.PNode;
import org.piccolo2d.nodes.PImage;
import org.simbrain.resource.ResourceManager;
import org.simbrain.world.odorworld.resources.OdorWorldResourceManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class WorldMapNode extends PNode {

    ArrayList<PImage> test = new ArrayList<>();

    {
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                PImage testimg = new PImage(OdorWorldResourceManager.getStaticImage("PlaceHolderTile.png"));
                testimg.offset(i * 32, j * 32);
                test.add(testimg);
                this.addChild(testimg);
            }
        }
    }
}
