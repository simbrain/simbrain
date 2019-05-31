package org.simbrain.custom_sims.simulations.simpleNeuroevolution;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.network.NetworkComponent;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import javax.swing.*;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SimpleNeuroevolution extends RegisteredSimulation {

    long seed = 0;
    Random globalRand;
    int borderSize = 5;    // TODO: Find out the real panel size
    int imageSize = 32;


    boolean run = false;


    List<NetBuilder> net = new ArrayList<>();
    List<NetWorldPair> nwp = new ArrayList<>();


    List<OdorWorldBuilder> world = new ArrayList<>();
    List<OdorWorldEntity> mouse = new ArrayList<>();
    List<OdorWorldEntity> cheese = new ArrayList<>();
    List<OdorWorldEntity> poison = new ArrayList<>();
    List<NetWorldPairAttribute> attribute = new ArrayList<>();
    ControlPanel cp, dp;

    boolean componentMinimized = false;

    public SimpleNeuroevolution(SimbrainDesktop desktop) {
        super(desktop);
    }

    public SimpleNeuroevolution() {
        super();
    }


    private void setUpWorkSpace() {
        int frameHeight = (int) sim.getDesktop().getHeight();
        int frameWidth = (int) sim.getDesktop().getWidth();
        int cpWidth = cp.getWidth() + borderSize * 2;
        int netLayOutColumn = (int) Math.ceil(Math.sqrt(EvolveNet.netSize));
        int netLayOutRow = (int) Math.ceil((double) EvolveNet.netSize / netLayOutColumn);
        int netLayOutColumnPx = (frameWidth - cpWidth) / netLayOutColumn;
        int netLayOutRowPx = frameHeight / netLayOutRow;

        SwingUtilities.invokeLater(() -> {
            for (int i = 0, currentX = cpWidth, currentY = 0, index = 0; i < netLayOutRow; i++) {
                currentY = netLayOutRowPx * i;

                for (int j = 0; j < netLayOutColumn && index + 1 < EvolveNet.netSize; j++) {
                    index = i * netLayOutColumn + j;
                    currentX = cpWidth + netLayOutColumnPx * j;
                    int width = netLayOutColumnPx / 2;
                    int height = netLayOutRowPx;

                    initializeNetwork(index, currentX, currentY, width, height);
                    addWorld(index, (currentX + width), currentY, width, height);
                }

            }


            for (int i = 0; i < net.size(); i++) {
                ((EvolveNet) net.get(i).getNetwork()).init();
                ((EvolveNet) net.get(i).getNetwork()).newSynapseMutation();
                setUpCoupling(i);
            }

        });


    }

    private void deleteWorkspacePair(int netIndex) {
        System.out.println("Removing " + netIndex);
        NetBuilder netToDelete = net.get(netIndex);
        OdorWorldBuilder worldToDelete = world.get(netIndex);
        sim.getWorkspace().removeWorkspaceComponent(netToDelete.getNetworkComponent());
        sim.getWorkspace().removeWorkspaceComponent(worldToDelete.getOdorWorldComponent());
        net.set(netIndex, null);
        world.set(netIndex, null);
        mouse.set(netIndex, null);
        cheese.set(netIndex, null);
        poison.set(netIndex, null);
        attribute.set(netIndex, null);
        //		synapseIndex.set(netIndex, null);
    }


    // TODO: Configurable distance
    // TODO: Use event
    private void updateFitnessScore() {
        for (int i = 0; i < net.size(); i++) {
            int cheeseDistance = (int) SimbrainMath.distance(mouse.get(i).getCenterLocation(), cheese.get(i).getCenterLocation());
            if (cheeseDistance < 16) {
                attribute.get(i).addFitness();
                randomizeEntityLocation(i, 400, 400, cheese.get(i));
            }
        }
    }

    private double finalizeFitnessScore(int netIndex) {
        int cheeseDistance = (int) SimbrainMath.distance(mouse.get(netIndex).getCenterLocation(), cheese.get(netIndex).getCenterLocation());
        double delta = (double) (128 - cheeseDistance) / 128;
        if (delta > 0) {
            attribute.get(netIndex).addFitness(delta);
        }
        return attribute.get(netIndex).getFitnessScore();
    }

    private ArrayList<NetWorldPairAttribute> sortFitness() {
        ArrayList<NetWorldPairAttribute> acp = new ArrayList<>(attribute);
        Collections.sort(acp, new FitnessComparator());
        return acp;
    }

    private void randomizeEntityLocation(int netIndex, int width, int height, OdorWorldEntity e) {
        int x, y;
        long thisSeed = ((EvolveNet) net.get(netIndex).getNetwork()).getSeed();
        Random rand = new Random(thisSeed);
        x = rand.nextInt(width);
        y = rand.nextInt(height);
        e.setLocation(x + (imageSize / 2), y + (imageSize / 2));
        ((EvolveNet) (net.get(netIndex).getNetwork())).setSeed(rand.nextLong());

    }

    private void newGeneration() {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < net.size(); i++) {
                finalizeFitnessScore(i);
                System.out.println("Fitness " + i + ": " + attribute.get(i).getFitnessScore());
            }
            EvolveNet.increaseGeneration();
            ArrayList<NetWorldPairAttribute> performance = sortFitness();
            // TODO: may be ceil?
            int accidentCount = 1 + (int) ((EvolveNet.accidentRate / 100) * EvolveNet.netSize);

            for (int i = 0; i < accidentCount; i++) {
                Collections.swap(performance, globalRand.nextInt(EvolveNet.netSize), globalRand.nextInt(EvolveNet.netSize));
            }


            int eliminationCount = EvolveNet.netSize * EvolveNet.eliminationRate / 100;
            for (int i = 0; i < eliminationCount; i++) {
                int parentIndex = performance.get(globalRand.nextInt(EvolveNet.netSize - eliminationCount) + eliminationCount).getNetIndex();
                int currentNetIndex = performance.get(i).getNetIndex();
                System.out.println("CurrentIndex: " + currentNetIndex);
                System.out.println("Current Generation: " + EvolveNet.generation);

                int x = performance.get(i).getWindowLocationX();
                int y = performance.get(i).getWindowLocationY();
                int w = performance.get(i).getWindowWidth();
                int h = performance.get(i).getWindowHeight();

                //	    		ArrayList<Object> allStuff = new ArrayList<>();
                //	    		for(NeuronGroup ng : net.get(parentIndex).getNetwork().getFlatNeuronGroupList()) {
                //	    			allStuff.add(ng);
                //	    		}
                //	    		for(Synapse s : net.get(parentIndex).getNetwork().getFlatSynapseList()) {
                //	    			allStuff.add(s);
                //	    		}

                deleteWorkspacePair(currentNetIndex);
                //	    		ArrayList<?> newItem =
                //	    				CopyPaste.getCopy(net.get(currentNetIndex).getNetwork(), allStuff);
                //	    		initializeNetwork(currentNetIndex, net.get(parentIndex).getNetworkComponent(), x, y, w, h); // cannot understand
                EvolveNet parentNet = (EvolveNet) (net.get(parentIndex).getNetworkComponent().getNetwork()).copy();
                initializeNetwork(currentNetIndex, parentNet, x, y, w, h);
                //	    		((EvolveNet) net.get(currentNetIndex).getNetwork()).init();
                //	    		((EvolveNet) net.get(currentNetIndex).getNetwork()).copy();
                addWorld(currentNetIndex, (x + w), y, w, h);
                //	    		setUpCoupling(i);
                //	    		System.out.println("Coupling set for " + i);
            }


            for (int i = 0; i < eliminationCount; i++) {
                setUpCoupling(i);
                System.out.println("Coupling set for " + net.get(i).getNetworkComponent().getName());
            }

            for (int i = 0; i < net.size(); i++) {
                attribute.get(i).resetFitness();
                try {
                    sim.getDesktop().getDesktopComponent(net.get(i).getNetworkComponent()).getParentFrame().setIcon(componentMinimized);
                    sim.getDesktop().getDesktopComponent(world.get(i).getOdorWorldComponent()).getParentFrame().setIcon(componentMinimized);
                } catch (PropertyVetoException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        });
    }

    private void initializeNetwork(int netIndex, int x, int y, int width, int height) {
        EvolveNet newNet = new EvolveNet();
        NetworkComponent newNC = new NetworkComponent("Place Holder", newNet);

        initializeNetwork(netIndex, newNC, x, y, width, height);
    }

    private void initializeNetwork(int netIndex, EvolveNet newNet, int x, int y, int width, int height) {
        NetworkComponent newNC = new NetworkComponent("Place Holder", newNet);

        initializeNetwork(netIndex, newNC, x, y, width, height);
    }


    private void initializeNetwork(int netIndex, NetworkComponent nc, int x, int y, int width, int height) {
        Random rand = new Random(seed + netIndex);
        long thisSeed = rand.nextLong();

        int prefixDigit = (int) (Math.log10(EvolveNet.netSize) + 1);
        int prefix = (int) ((EvolveNet.generation + 1) * Math.pow(10, prefixDigit));

        System.out.println("NetSize: " + net.size());
        if (netIndex < net.size()) {
            System.out.println("set " + netIndex);

            attribute.set(netIndex, new NetWorldPairAttribute(prefix + netIndex, netIndex, x, y, width, height));

            ((EvolveNet) nc.getNetwork()).setSeed(thisSeed);

            nc.setName("N" + attribute.get(netIndex).getNetID());

            net.set(netIndex, sim.addNetwork(x, y, width, height, nc));
        } else {
            System.out.println("add");
            attribute.add(new NetWorldPairAttribute(prefix + netIndex, netIndex, x, y, width, height));

            ((EvolveNet) nc.getNetwork()).setSeed(thisSeed);

            nc.setName("N" + attribute.get(netIndex).getNetID());

            net.add(netIndex, sim.addNetwork(x, y, width, height, nc));
        }
    }


    private void addWorld(int worldIndex, int x, int y, int width, int height) {
        if (worldIndex < world.size()) {
            world.set(worldIndex, sim.addOdorWorld(x, y, width, height, "W" + attribute.get(worldIndex).getNetID()));
            OdorWorldBuilder currentWorld = world.get(worldIndex);
            currentWorld.getWorld().setObjectsBlockMovement(false);
            int worldHeight = currentWorld.getWorld().getHeight();
            int worldWidth = currentWorld.getWorld().getWidth();
            //			int worldHeight = 133; // TODO: use real value
            //			int worldWidth  = 164;
            cheese.set(worldIndex, currentWorld.addEntity(worldWidth / 6 * 5 - (imageSize / 2), worldHeight / 3 - (imageSize / 2), "Swiss.gif", new double[]{1, 0.1, 0.2}));

            poison.set(worldIndex, currentWorld.addEntity(worldWidth / 6 * 5 - (imageSize / 2), worldHeight / 3 * 2 - (imageSize / 2), "Poison.gif", new double[]{0.2, 0, 1}));

            mouse.set(worldIndex, currentWorld.addAgent(worldWidth / 6 - (imageSize / 2), worldHeight / 2 - (imageSize / 2), "Mouse"));
            // Print world size for debug
            System.out.println("I: " + worldIndex + "; (" + worldWidth + ", " + worldHeight + ")");
        } else {
            world.add(sim.addOdorWorld(x, y, width, height, "W" + attribute.get(worldIndex).getNetID()));
            OdorWorldBuilder currentWorld = world.get(worldIndex);
            currentWorld.getWorld().setObjectsBlockMovement(false);
            int worldHeight = currentWorld.getWorld().getHeight();
            int worldWidth = currentWorld.getWorld().getWidth();
            //			int worldHeight = 133; // TODO: use real value
            //			int worldWidth  = 164;
            // TODO: Make configurable
            cheese.add(currentWorld.addEntity(worldWidth / 6 * 5 - (imageSize / 2), worldHeight / 3 - (imageSize / 2), "Swiss.gif", new double[]{1, 0.1, 0.2}));

            poison.add(currentWorld.addEntity(worldWidth / 6 * 5 - (imageSize / 2), worldHeight / 3 * 2 - (imageSize / 2), "Poison.gif", new double[]{0.2, 0, 1}));

            mouse.add(currentWorld.addAgent(worldWidth / 6 - (imageSize / 2), worldHeight / 2 - (imageSize / 2), "Mouse"));
            // Print world size for debug
            System.out.println("I: " + worldIndex + "; (" + worldWidth + ", " + worldHeight + ")");
        }

    }


    private void setUpCoupling(int netIndex) {
        // TODO: make flexible
        //		for(int k = 0; k < 3; k++) {
        //			sim.couple((SmellSensor) mouse.get(netIndex).getSensor("Smell-Left"), k,
        //	                input.get(netIndex).getLooseNeuron(k));
        //		}
        //		for(int k = 3; k < 6; k++) {
        //			sim.couple((SmellSensor) mouse.get(netIndex).getSensor("Smell-Center"), k,
        //	                input.get(netIndex).getLooseNeuron(k));
        //		}
        //		for(int k = 6; k < 9; k++) {
        //			sim.couple((SmellSensor) mouse.get(netIndex).getSensor("Smell-Right"), k,
        //	                input.get(netIndex).getLooseNeuron(k));
        //		}
        int sensoorSize = mouse.get(netIndex).getSensors().size();
        // TODO: Couple sensor to input.
        mouse.get(netIndex).getSensors().get(1).getId();


        sim.couple((SmellSensor) mouse.get(netIndex).getSensor("Smell-Left"), ((EvolveNet) (net.get(netIndex).getNetwork())).getInput(0)    // should be called inputGroup
        );
        sim.couple((SmellSensor) mouse.get(netIndex).getSensor("Smell-Center"), ((EvolveNet) (net.get(netIndex).getNetwork())).getInput(1));
        sim.couple((SmellSensor) mouse.get(netIndex).getSensor("Smell-Right"), ((EvolveNet) (net.get(netIndex).getNetwork())).getInput(2));
        //		sim.couple((SmellSensor) mouse.get(netIndex).getSensors().get(0), 0, inputFlat.get(netIndex).get(0));


        sim.couple(((EvolveNet) (net.get(netIndex).getNetwork())).getOutput().getNeuron(0), mouse.get(netIndex).getEffectors().get(0));
        sim.couple(((EvolveNet) (net.get(netIndex).getNetwork())).getOutput().getNeuron(1), mouse.get(netIndex).getEffectors().get(1));
        sim.couple(((EvolveNet) (net.get(netIndex).getNetwork())).getOutput().getNeuron(2), mouse.get(netIndex).getEffectors().get(2));
    }


    private void setUpControlPanel() {
        cp = ControlPanel.makePanel(sim, "Control Panel", 0, 0);
        JButton createBtn = cp.addButton("Create " + EvolveNet.netSize + " Networks", () -> {
            int result = 0;
            if (net.size() != 0) {
                result = JOptionPane.showConfirmDialog(null, "Current networks will be deleted. Are you sure?",    // TODO: implement delete, reset generation
                        "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            }
            if (result == 0) {
                setUpWorkSpace();
                for (int i = 0; i < net.size(); i++) {
                    ((EvolveNet) net.get(i).getNetwork()).newSynapseMutation();
                }
            }
        });

        JTextField networkCountTF = cp.addTextField("Network Count", "" + EvolveNet.netSize);
        cp.addButton("Set Network Size", () -> {
            try {
                EvolveNet.setNetSize(Integer.parseInt(networkCountTF.getText()));
                createBtn.setText("Create " + EvolveNet.netSize + " Networks");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Network Count must be an integer");
                networkCountTF.setText("" + EvolveNet.netSize);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage());
                networkCountTF.setText("1");
            }
        });


        // Auto mutation control stuff are not that important at this moment.
        // When implement, each network will have its mutation rate adjusted base on performance.
        // At this moment all networks use the global `mutationRate`.

        JLabel currentMutationRateLabel = cp.addLabel("Current Mutation %", "" + EvolveNet.mutationRate);
        JTextField currentMutationRateTF = cp.addTextField("Set mutation rate to", "" + EvolveNet.mutationRate);
        JButton currentMutationRateTFBtn = cp.addButton("Set Mutation Rate", () -> {
            try {
                EvolveNet.setMutationRate(Double.parseDouble(currentMutationRateTF.getText()));
                currentMutationRateLabel.setText("" + EvolveNet.mutationRate);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Mutation rate must be an number");
                currentMutationRateTF.setText("" + EvolveNet.mutationRate);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage());
                currentMutationRateTF.setText("" + EvolveNet.mutationRate);
            }
        });

        JLabel currentMaxMutationRateLabel = cp.addLabel("Current Max Mutation %", "" + EvolveNet.maxMutationRate);
        JTextField currentMaxMutationRateTF = cp.addTextField("Set mutation rate to", "" + EvolveNet.maxMutationRate);
        cp.addButton("Set Max Mutation Rate", () -> {
            try {
                EvolveNet.setMaxMutationRate(Double.parseDouble(currentMaxMutationRateTF.getText()));
                currentMaxMutationRateLabel.setText("" + EvolveNet.maxMutationRate);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Mutation rate must be an number");
                currentMaxMutationRateTF.setText("" + EvolveNet.maxMutationRate);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage());
                currentMaxMutationRateTF.setText("" + EvolveNet.maxMutationRate);
            }
        });

        JLabel currentMinMutationRateLabel = cp.addLabel("Current Min Mutation %", "" + EvolveNet.minMutationRate);
        JTextField currentMinMutationRateTF = cp.addTextField("Set mutation rate to", "" + EvolveNet.minMutationRate);
        cp.addButton("Set Min Mutation Rate", () -> {
            try {
                EvolveNet.setMinMutationRate(Double.parseDouble(currentMinMutationRateTF.getText()));
                currentMinMutationRateLabel.setText("" + EvolveNet.minMutationRate);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Mutation rate must be an number");
                currentMinMutationRateTF.setText("" + EvolveNet.minMutationRate);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage());
                currentMinMutationRateTF.setText("" + EvolveNet.minMutationRate);
            }
        });

        JLabel autoMutationLabel = cp.addLabel("Auto Mutation Control:", (EvolveNet.autoMutationControl ? "ON" : "OFF"));
        cp.addButton("Toggle", () -> {
            EvolveNet.toggleAutoMutationControl();
            autoMutationLabel.setText((EvolveNet.autoMutationControl ? "ON" : "OFF"));
            currentMutationRateTF.setEnabled(!EvolveNet.autoMutationControl);
            currentMutationRateTFBtn.setEnabled(!EvolveNet.autoMutationControl);
        });

        cp.addButton("Run", () -> {
            run = true;
            while (run) {
                for (int i = 0; i < 500; i++) {
                    sim.iterate();
                    updateFitnessScore();
                }
                newGeneration();
                // TODO: Apply to new network only. Move to newGeneration()
                for (int i = 0; i < net.size(); i++) {
                    ((EvolveNet) net.get(i).getNetwork()).mutateNetwork();
                }

            }
            //			sim.getWorkspace().stop();
        });

        cp.addButton("Stop", () -> {
            run = false;
            //			sim.getWorkspace().stop();
        });

        currentMutationRateTF.setEnabled(!EvolveNet.autoMutationControl);
        currentMutationRateTFBtn.setEnabled(!EvolveNet.autoMutationControl);
    }


    private void setUpDebugPanel() {
        int y = cp.getHeight();
        dp = ControlPanel.makePanel(sim, "Debug Panel", 0, y + borderSize + 24);

        // Manual mutation control
        dp.addButton("New Synapse Mutation", () -> {
            for (int i = 0; i < net.size(); i++) {
                ((EvolveNet) net.get(i).getNetwork()).newSynapseMutation();
            }
        });
        dp.addButton("Synapse Strength Mutation", () -> {
            for (int i = 0; i < net.size(); i++) {
                ((EvolveNet) net.get(i).getNetwork()).synapseStrengthMutation();
            }
        });
        dp.addButton("New Neuron Mutation", () -> {
            for (int i = 0; i < net.size(); i++) {
                ((EvolveNet) net.get(i).getNetwork()).newNeuronMutation();
            }
        });


        // Just to test network removal.
        dp.addButton("Remove network 1", () -> {
            sim.getWorkspace().removeWorkspaceComponent(net.get(1).getNetworkComponent());
            sim.getWorkspace().removeWorkspaceComponent(world.get(1).getOdorWorldComponent());
        });
        dp.addButton("Add network 1 back", () -> {
            sim.getWorkspace().addWorkspaceComponent(net.get(1).getNetworkComponent());
            sim.getWorkspace().addWorkspaceComponent(world.get(1).getOdorWorldComponent());
        });
        dp.addButton("Remove All Networks", () -> {
            for (int i = 0; i < net.size(); i++) {
                sim.getWorkspace().removeWorkspaceComponent(net.get(i).getNetworkComponent());
                sim.getWorkspace().removeWorkspaceComponent(world.get(i).getOdorWorldComponent());
            }
        });


        dp.addButton("Toggle Minimize", () -> {
            for (int i = 0; i < net.size(); i++) {
                try {
                    sim.getDesktop().getDesktopComponent(net.get(i).getNetworkComponent()).getParentFrame().setIcon(!componentMinimized);
                    sim.getDesktop().getDesktopComponent(world.get(i).getOdorWorldComponent()).getParentFrame().setIcon(!componentMinimized);
                } catch (PropertyVetoException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            componentMinimized = !componentMinimized;
        });

        dp.addButton("Print Fitness", () -> {
            System.out.println("Fitness Score: ");
            for (int i = 0; i < attribute.size(); i++) {
                NetWorldPairAttribute a = attribute.get(i);
                System.out.println("\t" + net.get(i).getNetworkComponent().getName() + ": " + a.getFitnessScore());
            }
        });

        dp.addButton("Elimiate", () -> {
            newGeneration();
        });
    }


    @Override
    public String getName() {
        return "Simple NeuroEvolution";
    }

    @Override
    public void run() {
        // Clear workspace
        sim.getWorkspace().clearWorkspace();
        seed = System.nanoTime();
        Random rand = new Random(seed);
        seed = rand.nextLong();
        globalRand = new Random(seed);
        System.out.println(seed);

        NetWorldPair.sim = sim;

        setUpControlPanel();
        setUpDebugPanel();

    }

    @Override
    public RegisteredSimulation instantiate(SimbrainDesktop desktop) {
        return new SimpleNeuroevolution(desktop);
    }

}
