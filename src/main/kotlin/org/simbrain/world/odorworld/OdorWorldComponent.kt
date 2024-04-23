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
package org.simbrain.world.odorworld;

import com.thoughtworks.xstream.XStream;
import org.simbrain.util.XStreamUtils;
import org.simbrain.util.piccolo.TileMap;
import org.simbrain.util.piccolo.TiledDataConverter;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * <b>WorldPanel</b> is the container for the world component. Handles toolbar
 * buttons, and serializing of world data. The main environment codes is in {@link OdorWorldPanel}.
 */
public class OdorWorldComponent extends WorkspaceComponent {

    /**
     * Reference to model world.
     */
    private OdorWorld world;

    /**
     * Default constructor.
     *
     * @param name
     */
    public OdorWorldComponent(String name) {
        super(name);
        world = new OdorWorld();
        init();
    }

    /**
     * Constructor used in deserializing.
     *
     * @param name  name of world
     * @param world model world
     */
    public OdorWorldComponent(String name, OdorWorld world) {
        super(name);
        this.world = world;
        init();
    }

    private void init() {
        world.getEvents().getEntityAdded().on(entity -> {
            fireAttributeContainerAdded(entity);
            setChangedSinceLastSave(true);
            entity.getEvents().getSensorAdded().on(this::fireAttributeContainerAdded);
            entity.getEvents().getEffectorAdded().on(this::fireAttributeContainerAdded);
            entity.getEvents().getSensorRemoved().on(this::fireAttributeContainerRemoved);
            entity.getEvents().getEffectorRemoved().on(this::fireAttributeContainerRemoved);
            setChangedSinceLastSave(true);
        });

        world.getEvents().getEntityRemoved().on(e -> {
            fireAttributeContainerRemoved(e);
            e.getSensors().forEach(this::fireAttributeContainerRemoved);
            e.getEffectors().forEach(this::fireAttributeContainerRemoved);
            setChangedSinceLastSave(true);
        });

    }

    public static XStream getOdorWorldXStream() {
        XStream xstream = XStreamUtils.getSimbrainXStream();
        xstream.processAnnotations(TileMap.class);
        xstream.registerConverter(new TiledDataConverter(xstream.getMapper(), xstream.getReflectionProvider()));
        xstream.registerConverter(XStreamUtils.createConstructorCallingConverter(List.of(OdorWorldEntity.class), xstream.getMapper(), xstream.getReflectionProvider()));
        return xstream;
    }

    @Override
    public String getXML() {
        return getOdorWorldXStream().toXML(world);
    }

    @Override
    public void save(OutputStream output, String format) {
        getOdorWorldXStream().toXML(world, output);
    }

    /**
     * Recreates an instance of this class from a saved component.
     */
    public static OdorWorldComponent open(InputStream input, String name, String format) {
        XStream xstream = getOdorWorldXStream();
        OdorWorld newWorld = (OdorWorld) xstream.fromXML(input);
        return new OdorWorldComponent(name, newWorld);
    }

    @Override
    public void update() {
        world.update();
    }

    public OdorWorld getWorld() {
        return world;
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> models = new ArrayList<>();
        for (OdorWorldEntity entity : world.getEntityList()) {
            models.add(entity);
            models.addAll(entity.getSensors());
            models.addAll(entity.getEffectors());
        }
        return models;
    }

    @Override
    public void start() {
        world.start();
    }

    @Override
    public void stop() {
        world.stopAnimation();
    }
}
