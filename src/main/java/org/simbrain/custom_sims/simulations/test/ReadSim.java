package org.simbrain.custom_sims.simulations.test;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.gui.SimbrainDesktop;

/**
 * A utility that can be used to read auto-generated script text from Simbrain 3.x to be used in converting
 * it.  Can use this to transfer a workspace from an older simbrain to this one.
 *
 * (1) Run a conversion script in Simbrain 3.X.  (2) Copy the resulting "script text".  (3) Paste
 * that text in the run method of this class.  (4) Run this simulation.  (5) Save the resulting workspace.
 */
public class ReadSim extends RegisteredSimulation {

    @Override
    public void run() {

        // To recreate a network from "script" text,
        // paste stuff here, run it, then save.  The code below is just an example.

        Workspace workspace = sim.getWorkspace();

        NetworkComponent netComp_Network1 = new NetworkComponent("netComp_Network1");
        workspace.addWorkspaceComponent(netComp_Network1);

        Network net_Network1 = netComp_Network1.getNetwork();
        Neuron nrn_Neuron_1 = new Neuron(net_Network1);
        nrn_Neuron_1.setLocation(0, 0);
        nrn_Neuron_1.setLabel("");
        net_Network1.addLooseNeuron(nrn_Neuron_1);
        Neuron nrn_Neuron_2 = new Neuron(net_Network1);
        nrn_Neuron_2.setLocation(45, 0);
        nrn_Neuron_2.setLabel("");
        net_Network1.addLooseNeuron(nrn_Neuron_2);
        Neuron nrn_Neuron_3 = new Neuron(net_Network1);
        nrn_Neuron_3.setLocation(90, 0);
        nrn_Neuron_3.setLabel("");
        net_Network1.addLooseNeuron(nrn_Neuron_3);
        Neuron nrn_Neuron_4 = new Neuron(net_Network1);
        nrn_Neuron_4.setLocation(135, 0);
        nrn_Neuron_4.setLabel("");
        net_Network1.addLooseNeuron(nrn_Neuron_4);
        Neuron nrn_Neuron_5 = new Neuron(net_Network1);
        nrn_Neuron_5.setLocation(180, 0);
        nrn_Neuron_5.setLabel("");
        net_Network1.addLooseNeuron(nrn_Neuron_5);
        Neuron nrn_Neuron_6 = new Neuron(net_Network1);
        nrn_Neuron_6.setLocation(225, 0);
        nrn_Neuron_6.setLabel("");
        net_Network1.addLooseNeuron(nrn_Neuron_6);
        Synapse syn_Synapse_1 = new Synapse(net_Network1.getLooseNeuron("Neuron_1"),net_Network1.getLooseNeuron("Neuron_2"),1.0119140764489423);
        net_Network1.addLooseSynapse(syn_Synapse_1);
        Synapse syn_Synapse_2 = new Synapse(net_Network1.getLooseNeuron("Neuron_1"),net_Network1.getLooseNeuron("Neuron_3"),4.8564388901894855);
        net_Network1.addLooseSynapse(syn_Synapse_2);
        Synapse syn_Synapse_3 = new Synapse(net_Network1.getLooseNeuron("Neuron_1"),net_Network1.getLooseNeuron("Neuron_4"),-9.931731856328526);
        net_Network1.addLooseSynapse(syn_Synapse_3);
        Synapse syn_Synapse_4 = new Synapse(net_Network1.getLooseNeuron("Neuron_1"),net_Network1.getLooseNeuron("Neuron_5"),2.0797739438281084);
        net_Network1.addLooseSynapse(syn_Synapse_4);
        Synapse syn_Synapse_5 = new Synapse(net_Network1.getLooseNeuron("Neuron_1"),net_Network1.getLooseNeuron("Neuron_6"),-6.214471583998485);
        net_Network1.addLooseSynapse(syn_Synapse_5);
        Synapse syn_Synapse_6 = new Synapse(net_Network1.getLooseNeuron("Neuron_2"),net_Network1.getLooseNeuron("Neuron_1"),4.856393265538985);
        net_Network1.addLooseSynapse(syn_Synapse_6);
        Synapse syn_Synapse_7 = new Synapse(net_Network1.getLooseNeuron("Neuron_2"),net_Network1.getLooseNeuron("Neuron_3"),0.20961192740958268);
        net_Network1.addLooseSynapse(syn_Synapse_7);
        Synapse syn_Synapse_8 = new Synapse(net_Network1.getLooseNeuron("Neuron_2"),net_Network1.getLooseNeuron("Neuron_4"),3.2567193808585557);
        net_Network1.addLooseSynapse(syn_Synapse_8);
        Synapse syn_Synapse_9 = new Synapse(net_Network1.getLooseNeuron("Neuron_2"),net_Network1.getLooseNeuron("Neuron_5"),8.853015211262377);
        net_Network1.addLooseSynapse(syn_Synapse_9);
        Synapse syn_Synapse_10 = new Synapse(net_Network1.getLooseNeuron("Neuron_2"),net_Network1.getLooseNeuron("Neuron_6"),6.8545000155186315);
        net_Network1.addLooseSynapse(syn_Synapse_10);
        Synapse syn_Synapse_11 = new Synapse(net_Network1.getLooseNeuron("Neuron_3"),net_Network1.getLooseNeuron("Neuron_1"),8.033995217757827);
        net_Network1.addLooseSynapse(syn_Synapse_11);
        Synapse syn_Synapse_12 = new Synapse(net_Network1.getLooseNeuron("Neuron_3"),net_Network1.getLooseNeuron("Neuron_2"),6.863870372334443);
        net_Network1.addLooseSynapse(syn_Synapse_12);
        Synapse syn_Synapse_13 = new Synapse(net_Network1.getLooseNeuron("Neuron_3"),net_Network1.getLooseNeuron("Neuron_4"),-1.3099695419288455);
        net_Network1.addLooseSynapse(syn_Synapse_13);
        Synapse syn_Synapse_14 = new Synapse(net_Network1.getLooseNeuron("Neuron_3"),net_Network1.getLooseNeuron("Neuron_5"),8.37007084594919);
        net_Network1.addLooseSynapse(syn_Synapse_14);
        Synapse syn_Synapse_15 = new Synapse(net_Network1.getLooseNeuron("Neuron_3"),net_Network1.getLooseNeuron("Neuron_6"),6.700934787448833);
        net_Network1.addLooseSynapse(syn_Synapse_15);
        Synapse syn_Synapse_16 = new Synapse(net_Network1.getLooseNeuron("Neuron_4"),net_Network1.getLooseNeuron("Neuron_1"),1.4313264099355685);
        net_Network1.addLooseSynapse(syn_Synapse_16);
        Synapse syn_Synapse_17 = new Synapse(net_Network1.getLooseNeuron("Neuron_4"),net_Network1.getLooseNeuron("Neuron_2"),-7.963639026461058);
        net_Network1.addLooseSynapse(syn_Synapse_17);
        Synapse syn_Synapse_18 = new Synapse(net_Network1.getLooseNeuron("Neuron_4"),net_Network1.getLooseNeuron("Neuron_3"),-9.323986899151567);
        net_Network1.addLooseSynapse(syn_Synapse_18);
        Synapse syn_Synapse_19 = new Synapse(net_Network1.getLooseNeuron("Neuron_4"),net_Network1.getLooseNeuron("Neuron_5"),6.078393905097673);
        net_Network1.addLooseSynapse(syn_Synapse_19);
        Synapse syn_Synapse_20 = new Synapse(net_Network1.getLooseNeuron("Neuron_4"),net_Network1.getLooseNeuron("Neuron_6"),8.48380350144999);
        net_Network1.addLooseSynapse(syn_Synapse_20);
        Synapse syn_Synapse_21 = new Synapse(net_Network1.getLooseNeuron("Neuron_5"),net_Network1.getLooseNeuron("Neuron_1"),4.532029452537339);
        net_Network1.addLooseSynapse(syn_Synapse_21);
        Synapse syn_Synapse_22 = new Synapse(net_Network1.getLooseNeuron("Neuron_5"),net_Network1.getLooseNeuron("Neuron_2"),-7.589768079593906);
        net_Network1.addLooseSynapse(syn_Synapse_22);
        Synapse syn_Synapse_23 = new Synapse(net_Network1.getLooseNeuron("Neuron_5"),net_Network1.getLooseNeuron("Neuron_3"),-1.6184321787880833);
        net_Network1.addLooseSynapse(syn_Synapse_23);
        Synapse syn_Synapse_24 = new Synapse(net_Network1.getLooseNeuron("Neuron_5"),net_Network1.getLooseNeuron("Neuron_4"),-9.133141793468546);
        net_Network1.addLooseSynapse(syn_Synapse_24);
        Synapse syn_Synapse_25 = new Synapse(net_Network1.getLooseNeuron("Neuron_5"),net_Network1.getLooseNeuron("Neuron_6"),0.023888247558481623);
        net_Network1.addLooseSynapse(syn_Synapse_25);
        Synapse syn_Synapse_26 = new Synapse(net_Network1.getLooseNeuron("Neuron_6"),net_Network1.getLooseNeuron("Neuron_1"),-2.9327899730282407);
        net_Network1.addLooseSynapse(syn_Synapse_26);
        Synapse syn_Synapse_27 = new Synapse(net_Network1.getLooseNeuron("Neuron_6"),net_Network1.getLooseNeuron("Neuron_2"),-3.657795303765776);
        net_Network1.addLooseSynapse(syn_Synapse_27);
        Synapse syn_Synapse_28 = new Synapse(net_Network1.getLooseNeuron("Neuron_6"),net_Network1.getLooseNeuron("Neuron_3"),-3.0572680659861273);
        net_Network1.addLooseSynapse(syn_Synapse_28);
        Synapse syn_Synapse_29 = new Synapse(net_Network1.getLooseNeuron("Neuron_6"),net_Network1.getLooseNeuron("Neuron_4"),0.09580722402684749);
        net_Network1.addLooseSynapse(syn_Synapse_29);
        Synapse syn_Synapse_30 = new Synapse(net_Network1.getLooseNeuron("Neuron_6"),net_Network1.getLooseNeuron("Neuron_5"),6.361338877279213);
        net_Network1.addLooseSynapse(syn_Synapse_30);


    }

    public ReadSim() {
        super();
    }

    public ReadSim(SimbrainDesktop desktop) {
        super(desktop);
    }

    @Override
    public String getSubmenuName() {
        return "Utils";
    }

    @Override
    public String getName() {
        return "Read Sim";
    }

    @Override
    public RegisteredSimulation instantiate(SimbrainDesktop desktop) {
        return new ReadSim(desktop);
    }
}
