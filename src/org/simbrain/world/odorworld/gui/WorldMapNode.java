package org.simbrain.world.odorworld.gui;

import org.piccolo2d.PNode;
import org.piccolo2d.nodes.PImage;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.piccolo.TileMap;
import org.simbrain.world.odorworld.resources.OdorWorldResourceManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class WorldMapNode extends PNode {

    ArrayList<PImage> test;

    {
        TileMap map = new TileMap("sample.tmx");
        test = map.render();
        for (PImage i : test) {
            this.addChild(i);
        }
    }
}
