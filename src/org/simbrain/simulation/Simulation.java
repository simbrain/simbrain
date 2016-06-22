package org.simbrain.simulation;

import java.util.Hashtable;

import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.workspace.AttributeManager;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.PotentialConsumer;
import org.simbrain.workspace.PotentialProducer;
import org.simbrain.workspace.UmatchedAttributesException;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.entities.RotatingEntity;

//TODO: Document everything
public class Simulation {

    final SimbrainDesktop desktop;
    final Workspace workspace;

    /**
     * @param desktop
     */
    public Simulation(SimbrainDesktop desktop) {
        super();
        this.desktop = desktop;
        this.workspace = desktop.getWorkspace();
    }

    /**
     * @return the desktop
     */
    public SimbrainDesktop getDesktop() {
        return desktop;
    }
    

    // Neurons to agent.  So far just one to one
    public void couple(NeuronGroup ng, RotatingEntity entity) {
        AttributeManager producers = netMap.get(ng.getParentNetwork()).getAttributeManager();          
        PotentialProducer straightProducer = producers.createPotentialProducer(ng.getNeuronList().get(0), "getActivation", double.class); 

        AttributeManager consumers = odorMap.get(entity.getParentWorld()).getAttributeManager();          
        PotentialConsumer straightConsumer = producers.createPotentialConsumer(entity, "goStraight", double.class); 

        Coupling straightCoupling = new Coupling(straightProducer, straightConsumer);
        try {
            workspace.getCouplingManager().addCoupling(straightCoupling);
        } catch (UmatchedAttributesException e) {
            e.printStackTrace();
        }        
    }
    
    Hashtable<Network, NetworkComponent> netMap = new Hashtable();
    Hashtable<OdorWorld, OdorWorldComponent> odorMap = new Hashtable();

    // Agent to neurons
    public void couple(RotatingEntity entity, NeuronGroup ng) {
        
    }

    // Associate networks with networkcomponents
    // Associate entities with odorworldcomponents 
    
    public NetBuilder addNetwork(int x, int y, int width, int height,
            String name) {
        NetworkComponent networkComponent = new NetworkComponent(name);
        workspace.addWorkspaceComponent(networkComponent);
        desktop.getDesktopComponent(networkComponent).getParentFrame()
                .setBounds(x, y, width, height);
        netMap.put(networkComponent.getNetwork(), networkComponent);
        return new NetBuilder(networkComponent);
    }

    public OdorWorldBuilder addOdorWorld(int x, int y, int width, int height,
            String name) {
        OdorWorldComponent odorWorldComponent = new OdorWorldComponent(name);
        workspace.addWorkspaceComponent(odorWorldComponent);
        desktop.getDesktopComponent(odorWorldComponent).getParentFrame()
                .setBounds(x, y, width, height);
        odorMap.put(odorWorldComponent.getWorld(), odorWorldComponent);
        return new OdorWorldBuilder(odorWorldComponent);
    }

    // TODO: Same kind of thing as above for odorworld, plots, etc.

    /**
     * @return the workspace
     */
    public Workspace getWorkspace() {
        return workspace;
    }

}
