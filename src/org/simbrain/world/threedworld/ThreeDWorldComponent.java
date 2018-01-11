package org.simbrain.world.threedworld;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.threedworld.engine.ThreeDEngine;
import org.simbrain.world.threedworld.engine.ThreeDEngineConverter;
import org.simbrain.world.threedworld.entities.*;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * ThreeDWorldComponent is a workspace component to extract some serialization and attribute
 * management from the ThreeDWorld.
 */
public class ThreeDWorldComponent extends WorkspaceComponent {
    /**
     * @return A newly constructed xstream for serializing a ThreeDWorld.
     */
    public static XStream getXStream() {
        XStream stream = new XStream(new DomDriver());
        stream.registerConverter(new ThreeDEngineConverter());
        stream.registerConverter(new BoxEntityXmlConverter());
        stream.registerConverter(new ModelEntityXmlConverter());
        return stream;
    }

    /**
     * Open a saved ThreeDWorldComponent from an XML input stream.
     * @param input The input stream to read.
     * @param name The name of the new world component.
     * @param format The format of the input stream. Should be xml.
     * @return A deserialized ThreeDWorldComponent with a valid ThreeDWorld.
     */
    public static ThreeDWorldComponent open(InputStream input, String name, String format) {
        ThreeDWorld world = (ThreeDWorld) getXStream().fromXML(input);
        return new ThreeDWorldComponent(name, world);
    }

    private ThreeDWorld world;

    /**
     * Construct a new ThreeDWorldComponent.
     * @param name The name of the new component.
     */
    public ThreeDWorldComponent(String name) {
        super(name);
        world = new ThreeDWorld();
    }

    /**
     * Construct a ThreeDWorldComponent with an existing ThreeDWorld.
     * @param name The name of the new component.
     * @param world The world.
     */
    private ThreeDWorldComponent(String name, ThreeDWorld world) {
        super(name);
        this.world = world;
    }

    /**
     * @return The ThreeDWorld for this workspace component.
     */
    public ThreeDWorld getWorld() {
        return world;
    }

    @Override
    public List<Object> getModels() {
        List<Object> models = new ArrayList<Object>();
        models.add(world);
        for (Entity entity : world.getEntities()) {
            models.add(entity);
            if (entity instanceof Agent) {
                Agent agent = (Agent) entity;
                models.addAll(agent.getSensors());
                models.addAll(agent.getEffectors());
            }
        }
        return models;
    }

    @Override
    public void save(OutputStream output, String format) {
        ThreeDEngine.State previousState = world.getEngine().getState();
        world.getEngine().queueState(ThreeDEngine.State.SystemPause, true);
        getXStream().toXML(world, output);
        world.getEngine().queueState(previousState, false);
    }

    @Override
    public Object getObjectFromKey(String objectKey) {
        if (objectKey == null || objectKey.isEmpty()) {
            return null;
        }
        String[] parsedKey = objectKey.split(":");
        Entity entity = getWorld().getEntity(parsedKey[0]);
        if (parsedKey.length == 1) {
            return entity;
        } else if (entity instanceof Agent) {
            String objectType = parsedKey[1];
            Agent agent = (Agent) entity;
            if ("sensor".equalsIgnoreCase(objectType)) {
                String sensorType = parsedKey[2];
                return agent.getSensor(sensorType);
            } else if ("effector".equalsIgnoreCase(objectType)) {
                String effectorType = parsedKey[2];
                return agent.getEffector(effectorType);
            }
        }
        return null;
    }

    @Override
    protected void closing() {
        world.getEngine().stop(true);
    }

    @Override
    public void update() {
        world.getEngine().updateSync();
    }
}
