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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.workspace.AttributeType;
import org.simbrain.workspace.PotentialProducer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.textworld.TextWorld.TextItem;

/**
 * <b>TextWorldComponent</b> is the container for the world component. Handles
 * toolbar buttons, and serializing of world data. The main environment code is
 * in {@link TextWorld}.
 */
public class ReaderComponent extends WorkspaceComponent {

    /** Instance of world of type TextWorld. */
    private final ReaderWorld world;

    /**
     * Creates a new frame of type TextWorld.
     *
     * @param name name of this component
     */
    public ReaderComponent(String name) {
        super(name);
        world = new ReaderWorld();
        init();
    }

    /**
     * Construct a component from an existing world; used in deserializing.
     *
     * @param name name of component
     * @param newWorld provided world
     */
    public ReaderComponent(String name, ReaderWorld newWorld) {
        super(name);
        world = newWorld;
        init();
    }

    /**
     * Initialize attribute types.
     */
    private void init() {
        addProducerType(new AttributeType(this, "Letters", double.class, true));
        addProducerType(new AttributeType(this, "Words", double.class, false));
        world.addListener(new TextListener() {
            public void textChanged() {
            }

            public void dictionaryChanged() {
                ReaderComponent.this.firePotentialAttributesChanged();
            }

            public void positionChanged() {
            }

            public void currentItemChanged(TextItem newItem) {
            }
        });
    }

    @Override
    public List<PotentialProducer> getPotentialProducers() {
        List<PotentialProducer> returnList = new ArrayList<PotentialProducer>();
        for (AttributeType type : getVisibleProducerTypes()) {
            if (type.getTypeName().equalsIgnoreCase("Letters")) {
                char letter;
                for (letter = 'a'; letter <= 'z'; letter++) {
                    returnList.add(new PotentialProducer(this, world,
                            "matchCurrentLetter", double.class, char.class,
                            letter, "Letter " + letter));
                }
            }
            if (type.getTypeName().equalsIgnoreCase("Words")) {
                for (String word : world.getDictionary()) {
                    returnList.add(new PotentialProducer(this, world,
                            "matchCurrentItem", double.class, String.class,
                            word, word));
                }

            }
        }
        return returnList;
    }

    /**
     * {@inheritDoc}
     */
    public static ReaderComponent open(InputStream input, String name,
            String format) {
        ReaderWorld newWorld = (ReaderWorld) ReaderWorld.getXStream().fromXML(
                input);
        return new ReaderComponent(name, newWorld);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
        ReaderWorld.getXStream().toXML(world, output);
    }

    @Override
    public void closing() {
        // TODO Auto-generated method stub
    }

    @Override
    public void update() {
        world.update();
    }

    /**
     * @return the world
     */
    public ReaderWorld getWorld() {
        return world;
    }

    @Override
    public Object getObjectFromKey(String objectKey) {
        return world;
    }
}
