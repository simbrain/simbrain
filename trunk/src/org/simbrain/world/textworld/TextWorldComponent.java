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

import org.simbrain.workspace.AttributeList;
import org.simbrain.workspace.AttributeType;
import org.simbrain.workspace.PotentialProducer;
import org.simbrain.workspace.WorkspaceComponent;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * <b>TextWorldComponent</b> is the container for the world component. Handles
 * toolbar buttons, and serializing of world data. The main environment code is
 * in {@link TextWorld}.
 */
public class TextWorldComponent extends WorkspaceComponent {

    /** Instance of world of type TextWorld. */
    private final TextWorld world;

    /** List of getters. */
    private AttributeList<Double> attributeList;

    /**
     * Creates a new frame of type TextWorld.
     *
     * @param name name of this component
     */
    public TextWorldComponent(String name) {
        super(name);
        world = new TextWorld();
        attributeList = new AttributeList<Double>(world.getInputCoding().length);
        addProducerType(new AttributeType(this, "Text", "getValue", Double.class,
                true));

        world.addListener(new TextListener() {
            /** {@inheritDoc} .*/
            public void textChanged() {
                for (int i = 0; i < world.getInputCoding().length; i++) {
                    attributeList.setVal(i, world.getInputCoding()[i]);
                }
            }

        });
    }

    @Override
    public List<PotentialProducer> getPotentialProducers() {
        List<PotentialProducer> returnList = new ArrayList<PotentialProducer>();
        for (AttributeType type : getVisibleProducerTypes()) {
            if (type.getTypeName().equalsIgnoreCase("Text")) {
                for (int i = 0; i < world.getInputCoding().length; i++) {
                    returnList.add(getAttributeManager()
                            .createPotentialProducer(
                                    attributeList.getGetterSetter(i), "getValue",
                                    double.class, "Text Component " + i));
                }
            }
        }
        return returnList;
    }

    /**
     * Returns a properly initialized xstream object.
     *
     * @return the XStream object
     */
    private static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        // omit fields
        return xstream;
    }

    /**
     * Recreates an instance of this class from a saved component.
     *
     * @param input
     * @param name
     * @param format
     * @return
     */
    public static TextWorldComponent open(InputStream input, String name,
            String format) {
        return (TextWorldComponent) getXStream().fromXML(input);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
        getXStream().toXML(output);
    }

    @Override
    public void closing() {
        // TODO Auto-generated method stub
    }

    @Override
    public void update() {
    }

    /**
     * @return the world
     */
    public TextWorld getWorld() {
        return world;
    }
}
