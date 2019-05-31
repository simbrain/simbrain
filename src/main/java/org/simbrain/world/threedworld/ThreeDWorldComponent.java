package org.simbrain.world.threedworld;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.threedworld.engine.ThreeDEngine;
import org.simbrain.world.threedworld.engine.ThreeDEngineConverter;
import org.simbrain.world.threedworld.entities.Agent;
import org.simbrain.world.threedworld.entities.BoxEntityXmlConverter;
import org.simbrain.world.threedworld.entities.Entity;
import org.simbrain.world.threedworld.entities.ModelEntityXmlConverter;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
     *
     * @param input  The input stream to read.
     * @param name   The name of the new world component.
     * @param format The format of the input stream. Should be xml.
     * @return A deserialized ThreeDWorldComponent with a valid ThreeDWorld.
     */
    public static ThreeDWorldComponent open(InputStream input, String name, String format) {
        ThreeDWorld world = (ThreeDWorld) getXStream().fromXML(input);
        world.getEngine().queueState(ThreeDEngine.State.RenderOnly, false);
        return new ThreeDWorldComponent(name, world);
    }

    public static ThreeDWorldComponent create(Workspace workspace, String name) {
        if (workspace.getComponentList(ThreeDWorldComponent.class).isEmpty()) {
            return new ThreeDWorldComponent(name);
        } else {
            throw new RuntimeException("Only one 3D World component is supported.");
        }
    }

    private ThreeDWorld world;

    /**
     * Construct a new ThreeDWorldComponent.
     *
     * @param name The name of the new component.
     */
    public ThreeDWorldComponent(String name) {
        super(name);
        world = new ThreeDWorld();
        world.addListener(new ThreeDWorld.Listener() {
            @Override
            public void onWorldInitialize(ThreeDWorld world) {
            }

            @Override
            public void onWorldUpdate(ThreeDWorld world) {
            }

            @Override
            public void onWorldClosing(ThreeDWorld world) {
                close();
            }
        });
    }

    /**
     * Construct a ThreeDWorldComponent with an existing ThreeDWorld.
     *
     * @param name  The name of the new component.
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
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> models = new ArrayList<>();
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
    public AttributeContainer getObjectFromKey(String objectKey) {
        if (objectKey == null || objectKey.isEmpty()) {
            return null;
        }
        String[] parsedKey = objectKey.split(":");

        Optional<Entity> entity = getWorld().getEntity(parsedKey[0]);
        if (entity.isPresent()) {
            if (parsedKey.length == 1) {
                return entity.get();
            } else {
                String objectType = parsedKey[1];
                Agent agent = (Agent) entity.get();
                if (objectType.toLowerCase().contains("sensor")) {
                    return agent.getSensor(objectType).get(); // TODO: Added the get() in a refactor may cause problems
                } else if (objectType.toLowerCase().contains("effector")) {
                    return agent.getEffector(objectType).get();
                }
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    protected void closing() {
        world.getEngine().stop(false);
    }

    @Override
    public void update() {
        world.getEngine().updateSync();
    }
}
