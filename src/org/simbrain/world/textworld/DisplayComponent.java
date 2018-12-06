/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.world.textworld;

import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.textworld.TextWorld.TextItem;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <b>DisplayComponent</b> is a component which wraps a display world with
 * consumers.
 */
public class DisplayComponent extends WorkspaceComponent {

    /**
     * Instance of world of type DisplayWorld.
     */
    private DisplayWorld world;

    /**
     * Default number of string reader attributes to add.
     */
    private int DEFAULT_NUM_STRING_READERS = 10;

    /**
     * Creates a new frame of type TextWorld.
     *
     * @param name name of this component
     */
    public DisplayComponent(String name) {
        super(name);
        world = new DisplayWorld();
        init();
    }

    /**
     * Construct a component from an existing world; used in deserializing.
     *
     * @param name     name of component
     * @param newWorld provided world
     */
    public DisplayComponent(String name, DisplayWorld newWorld) {
        super(name);
        world = newWorld;
        init();
    }

    /**
     * Initialize attribute types.
     */
    private void init() {
        this.world = world;
        //        addConsumerType(new AttributeType(this, "DisplayClosestWord",
        //                double[].class, true));
        //        addConsumerType(new AttributeType(this, "DisplayString", String.class,
        //                false));
        //        addConsumerType(new AttributeType(this, "DisplayWord", double.class,
        //                true));
        world.addListener(new TextListener() {

            public void textChanged() {
            }

            public void dictionaryChanged() {
                //                DisplayComponent.this.firePotentialAttributesChanged();
            }

            public void positionChanged() {
            }

            public void currentItemChanged(TextItem newItem) {
            }

            public void preferencesChanged() {
            }

        });
    }

    //    @Override
    //    public List<PotentialConsumer> getPotentialConsumers() {
    //        List<PotentialConsumer> returnList = new ArrayList<PotentialConsumer>();
    //        for (AttributeType type : getVisibleConsumerTypes()) {
    //            if (type.getTypeName().equalsIgnoreCase("DisplayString")) {
    //                for (int i = 0; i < DEFAULT_NUM_STRING_READERS; i++) {
    //                    String description = "String reader " + (i + 1);
    //                    PotentialConsumer consumer = getStringConsumer();
    //                    consumer.setCustomDescription(description);
    //                    returnList.add(consumer);
    //                }
    //            }
    //            if (type.getTypeName().equalsIgnoreCase("DisplayWord")) {
    //                for (String word : world.getTokenDictionary()) {
    //                    PotentialConsumer consumer = getAttributeManager()
    //                            .createPotentialConsumer(
    //                                    world,
    //                                    "addTextIfAboveThreshold",
    //                                    new Class<?>[] { double.class, String.class },
    //                                    new Object[] { word });
    //                    consumer.setCustomDescription(word);
    //                    returnList.add(consumer);
    //                }
    //            }
    //            if (type.getTypeName().equalsIgnoreCase("DisplayClosestWord")) {
    //                PotentialConsumer consumer = getAttributeManager()
    //                        .createPotentialConsumer(world, "displayClosestWord",
    //                                double[].class);
    //                consumer.setCustomDescription("Vector reader");
    //                returnList.add(consumer);
    //
    //            }
    //        }
    //        return returnList;
    //    }

    /**
     * {@inheritDoc}.
     */
    public static DisplayComponent open(InputStream input, String name, String format) {
        DisplayWorld newWorld = (DisplayWorld) DisplayWorld.getXStream().fromXML(input);
        return new DisplayComponent(name, newWorld);
    }

    @Override
    public void save(final OutputStream output, final String format) {
        world.preSaveInit();
        DisplayWorld.getXStream().toXML(world, output);
    }

    @Override
    public void closing() {
    }

    @Override
    public void update() {
        world.update();
    }

    /**
     * @return the world
     */
    public DisplayWorld getWorld() {
        return world;
    }

    @Override
    public AttributeContainer getObjectFromKey(String objectKey) {
        return world;
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> retList = new ArrayList<>();
        retList.add(world);
        //retList.addAll(world.get);
        return retList;
    }

    /**
     * Returns a String consumer, which reads in from a text producer and sends
     * that text straight to the display world. Convenient for external calls
     * from scripts.
     *
     * @return the string consumer.
     */
    //    public PotentialConsumer getStringConsumer() {
    //        return this.getAttributeManager().createPotentialConsumer(world,
    //                "addText", String.class);
    //    }

}
