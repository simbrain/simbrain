package org.simbrain.custom_sims.helper_classes;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.simbrain.custom_sims.helper_classes.OdorWorldXML.EntityDescription;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.entities.BasicEntity;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;

/**
 * A wrapper for a OdorWorldComponent that makes it easy to add stuff to an odor
 * world.
 *
 * TODO: Not sure this is the best name. Use it a while then decide.
 */
public class OdorWorldBuilder {

    private final OdorWorldComponent odorWorldComponent;

    private final OdorWorld world;

    public OdorWorldBuilder(OdorWorldComponent odorWorldComponent) {
        this.odorWorldComponent = odorWorldComponent;
        world = odorWorldComponent.getWorld();
    }

    public RotatingEntity addAgent(int x, int y, String type) {
        // TODO: Put in check for type
        RotatingEntity agent = new RotatingEntity(world);
        world.addAgent(agent);
        agent.setLocation(x, y); // TODO: Note that setCenterLocation fails
                                 // here. Problem in OdorWorld
        return agent;
    }

    public OdorWorldEntity addEntity(int x, int y, String imageName) {
        BasicEntity entity = new BasicEntity(imageName, world);
        entity.setLocation(x, y);
        entity.setSmellSource(new SmellSource(6));
        world.addEntity(entity);
        return entity;
    }

    public OdorWorldEntity addEntity(int x, int y, String imageName,
            double[] stimulus) {
        BasicEntity entity = new BasicEntity(imageName, world);
        entity.setLocation(x, y);
        entity.setSmellSource(new SmellSource(stimulus));
        world.addEntity(entity);
        return entity;
    }

    /**
     * Load a set of odorworld entities from a simple xml representation (see
     * {@link OdorWorldXML} for an example.) This is not the same as the xml
     * used in odor world, but is rather a reduced representation that is easy
     * to write by hand.
     *
     * We tried for json but it was a pain to configure with jaxb.
     *
     * @param xmlFile the file to load
     * @return a list of entities (not necessarily needed, this method loads the
     *         entites in to the world).
     */
    public List<OdorWorldEntity> loadWorld(File xmlFile) {
        ArrayList<OdorWorldEntity> worldEntities = new ArrayList<OdorWorldEntity>();
        try {
            JAXBContext jc = JAXBContext.newInstance(OdorWorldXML.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            OdorWorldXML xml = (OdorWorldXML) unmarshaller.unmarshal(xmlFile);
            // TODO: Deal with null checks / remove redundant code
            for (EntityDescription desc : xml.getAgents()) {
                RotatingEntity agent = this.addAgent(desc.x, desc.y,
                        desc.imageId);
                agent.setName(desc.name);
                agent.setHeading(desc.heading);
                worldEntities.add(agent);
            }
            for (EntityDescription desc : xml.getEntities()) {
                OdorWorldEntity entity = this.addEntity(desc.x, desc.y,
                        desc.imageId);
                entity.setName(desc.name);
                //entity.getSmellSource().setStimulus(desc.stim);
                entity.getSmellSource().setDispersion(desc.dispersion);
                worldEntities.add(entity);
            }
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return worldEntities;
    }

    public OdorWorld getWorld() {
        return world;
    }

    public OdorWorldComponent getOdorWorldComponent() {
        return odorWorldComponent;
    }

}
