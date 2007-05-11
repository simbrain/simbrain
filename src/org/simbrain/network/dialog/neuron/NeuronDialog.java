/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network.dialog.neuron;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.network.actions.ShowHelpAction;
import org.simbrain.network.nodes.NeuronNode;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simnet.interfaces.Neuron;
import org.simnet.neurons.AdditiveNeuron;
import org.simnet.neurons.BinaryNeuron;
import org.simnet.neurons.ClampedNeuron;
import org.simnet.neurons.DecayNeuron;
import org.simnet.neurons.ExponentialDecayNeuron;
import org.simnet.neurons.IACNeuron;
import org.simnet.neurons.IntegrateAndFireNeuron;
import org.simnet.neurons.IzhikevichNeuron;
import org.simnet.neurons.LMSNeuron;
import org.simnet.neurons.LinearNeuron;
import org.simnet.neurons.LogisticNeuron;
import org.simnet.neurons.NakaRushtonNeuron;
import org.simnet.neurons.PointNeuron;
import org.simnet.neurons.RandomNeuron;
import org.simnet.neurons.RunningAverageNeuron;
import org.simnet.neurons.SigmoidalNeuron;
import org.simnet.neurons.SinusoidalNeuron;
import org.simnet.neurons.StochasticNeuron;
import org.simnet.neurons.TemporalDifferenceNeuron;
import org.simnet.neurons.ThreeValuedNeuron;
import org.simnet.neurons.TraceNeuron;


/**
 * <b>NeuronDialog</b> is a dialog box for setting the properties of the Neuron.
 */
public class NeuronDialog extends StandardDialog implements ActionListener {

    /** Null string. */
    public static final String NULL_STRING = "...";

    /** Main panel. */
    private Box mainPanel = Box.createVerticalBox();

    /** Top panel. */
    private LabelledItemPanel topPanel = new LabelledItemPanel();

    /** Neuron panel. */
    private AbstractNeuronPanel neuronPanel;

    /** Neuron type combo box. */
    private JComboBox cbNeuronType = new JComboBox(Neuron.getTypeList());

    /** Activation field. */
    private JTextField tfActivation = new JTextField();

    /** Increment field. */
    private JTextField tfIncrement = new JTextField();

    /** Upper bound field. */
    private JTextField tfUpBound = new JTextField();

    /** Lower bound field. */
    private JTextField tfLowBound = new JTextField();
    
    /** Update priority field */
    private JTextField tfUpdatePriority = new JTextField();

    /** Upper label. */
    private JLabel upperLabel = new JLabel("Upper bound");

    /** Lower label. */
    private JLabel lowerLabel = new JLabel("Lower bound");

    /** Help Button. */
    private JButton helpButton = new JButton("Help");

    /** Show Help Action. */
    private ShowHelpAction helpAction = new ShowHelpAction();

    /** The neurons being modified. */
    private ArrayList neuronList = new ArrayList();

    /** The pnodes which refer to them. */
    private ArrayList selectionList;

    /** Used to determin if anything in the workspace has been changed. */
    private boolean neuronsHaveChanged = false;

    /**
     * @param selectedNeurons the pnode_neurons being adjusted
     */
    public NeuronDialog(final ArrayList selectedNeurons) {
        selectionList = selectedNeurons;
        setNeuronList();
        init();
    }

    /**
     * Get the logical neurons from the NeuronNodes.
     */
    private void setNeuronList() {
        neuronList.clear();

        Iterator i = selectionList.iterator();

        while (i.hasNext()) {
            NeuronNode n = (NeuronNode) i.next();
            neuronList.add(n.getNeuron());
        }
    }

    /**
     * Initialises the components on the panel.
     */
    private void init() {
        setTitle("Neuron Dialog");

        initNeuronType();
        fillFieldValues();
        updateHelp();

        helpButton.setAction(helpAction);
        this.addButton(helpButton);
        cbNeuronType.addActionListener(this);

        topPanel.addItem("Activation", tfActivation);
        topPanel.addItem("Increment", tfIncrement);
        topPanel.addItem("Update priority", tfUpdatePriority);
        topPanel.addItemLabel(upperLabel, tfUpBound);
        topPanel.addItemLabel(lowerLabel, tfLowBound);
        topPanel.addItem("Neuron type", cbNeuronType);

        mainPanel.add(topPanel);
        mainPanel.add(neuronPanel);
        setContentPane(mainPanel);

    }

    /** @see StandardDialog */
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    /**
     * Initialize the main neuron panel based on the type of the selected neurons.
     */
    public void initNeuronType() {
        Neuron neuronRef = (Neuron) neuronList.get(0);

        if (!NetworkUtils.isConsistent(neuronList, Neuron.class, "getType")) {
            cbNeuronType.addItem(AbstractNeuronPanel.NULL_STRING);
            cbNeuronType.setSelectedIndex(Neuron.getTypeList().length);
            neuronPanel = new ClampedNeuronPanel(); // Simply to serve as an empty panel
        } else if (neuronRef instanceof BinaryNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(BinaryNeuron.getName()));
            neuronPanel = new BinaryNeuronPanel();
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        } else if (neuronRef instanceof TraceNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(TraceNeuron.getName()));
            neuronPanel = new TraceNeuronPanel();
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        } else if (neuronRef instanceof AdditiveNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(AdditiveNeuron.getName()));
            neuronPanel = new AdditiveNeuronPanel(neuronRef.getParentNetwork());
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        } else if (neuronRef instanceof LinearNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(LinearNeuron.getName()));
            neuronPanel = new LinearNeuronPanel();
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        } else if (neuronRef instanceof SigmoidalNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(SigmoidalNeuron.getName()));
            neuronPanel = new SigmoidalNeuronPanel();
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        } else if (neuronRef instanceof RandomNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(RandomNeuron.getName()));
            neuronPanel = new RandomNeuronPanel();
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        } else if (neuronRef instanceof ClampedNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(ClampedNeuron.getName()));
            neuronPanel = new ClampedNeuronPanel();
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        } else if (neuronRef instanceof StochasticNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(StochasticNeuron.getName()));
            neuronPanel = new StochasticNeuronPanel();
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        } else if (neuronRef instanceof LogisticNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(LogisticNeuron.getName()));
            neuronPanel = new LogisticNeuronPanel();
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        } else if (neuronRef instanceof IntegrateAndFireNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(IntegrateAndFireNeuron.getName()));
            neuronPanel = new IntegrateAndFireNeuronPanel(neuronRef.getParentNetwork());
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        } else if (neuronRef instanceof SinusoidalNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(SinusoidalNeuron.getName()));
            neuronPanel = new SinusoidalNeuronPanel();
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        } else if (neuronRef instanceof IzhikevichNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(IzhikevichNeuron.getName()));
            neuronPanel = new IzhikevichNeuronPanel(neuronRef.getParentNetwork());
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        } else if (neuronRef instanceof NakaRushtonNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(NakaRushtonNeuron.getName()));
            neuronPanel = new NakaRushtonNeuronPanel(neuronRef.getParentNetwork());
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        } else if (neuronRef instanceof DecayNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(DecayNeuron.getName()));
            neuronPanel = new DecayNeuronPanel();
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        } else if (neuronRef instanceof IACNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(IACNeuron.getName()));
            neuronPanel = new IACNeuronPanel();
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        } else if (neuronRef instanceof ThreeValuedNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(ThreeValuedNeuron.getName()));
            neuronPanel = new ThreeValuedNeuronPanel();
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        } else if (neuronRef instanceof LMSNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(LMSNeuron.getName()));
            neuronPanel = new LMSNeuronPanel();
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        } else if (neuronRef instanceof ExponentialDecayNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(ExponentialDecayNeuron.getName()));
            neuronPanel = new ExponentialDecayNeuronPanel(neuronRef.getParentNetwork());
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        } else if (neuronRef instanceof RunningAverageNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(RunningAverageNeuron.getName()));
            neuronPanel = new RunningAverageNeuronPanel();
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        } else if (neuronRef instanceof TemporalDifferenceNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(TemporalDifferenceNeuron.getName()));
            neuronPanel = new TemporalDifferenceNeuronPanel();
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        } else if (neuronRef instanceof PointNeuron) {
            cbNeuronType.setSelectedIndex(Neuron.getNeuronTypeIndex(PointNeuron.getName()));
            neuronPanel = new PointNeuronPanel(neuronRef.getParentNetwork());
            neuronPanel.setNeuronList(neuronList);
            neuronPanel.fillFieldValues();
        }
    }

    /**
     * Change all the neurons from their current type to the new selected type.
     */
    public void changeNeurons() {
        if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(BinaryNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron oldNeuron = (Neuron) neuronList.get(i);
                BinaryNeuron newNeuron = new BinaryNeuron(oldNeuron);
                newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        } else if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(AdditiveNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron oldNeuron = (Neuron) neuronList.get(i);
                AdditiveNeuron newNeuron = new AdditiveNeuron(oldNeuron);
                newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        } else if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(LinearNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron oldNeuron = (Neuron) neuronList.get(i);
                LinearNeuron newNeuron = new LinearNeuron(oldNeuron);
                newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        } else if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(SigmoidalNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron oldNeuron = (Neuron) neuronList.get(i);
                SigmoidalNeuron newNeuron = new SigmoidalNeuron(oldNeuron);
                newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        } else if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(RandomNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron oldNeuron = (Neuron) neuronList.get(i);
                RandomNeuron newNeuron = new RandomNeuron(oldNeuron);
                newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        } else if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(ClampedNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron oldNeuron = (Neuron) neuronList.get(i);
                ClampedNeuron newNeuron = new ClampedNeuron(oldNeuron);
                newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        } else if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(StochasticNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron oldNeuron = (Neuron) neuronList.get(i);
                StochasticNeuron newNeuron = new StochasticNeuron(oldNeuron);
                newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        } else if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(LogisticNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron oldNeuron = (Neuron) neuronList.get(i);
                LogisticNeuron newNeuron = new LogisticNeuron(oldNeuron);
                newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        } else if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(IntegrateAndFireNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron oldNeuron = (Neuron) neuronList.get(i);
                IntegrateAndFireNeuron newNeuron = new IntegrateAndFireNeuron(oldNeuron);
                newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        } else if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(SinusoidalNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron oldNeuron = (Neuron) neuronList.get(i);
                SinusoidalNeuron newNeuron = new SinusoidalNeuron(oldNeuron);
                newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        } else if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(IzhikevichNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron oldNeuron = (Neuron) neuronList.get(i);
                IzhikevichNeuron newNeuron = new IzhikevichNeuron(oldNeuron);
                newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        } else if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(NakaRushtonNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron oldNeuron = (Neuron) neuronList.get(i);
                NakaRushtonNeuron newNeuron = new NakaRushtonNeuron(oldNeuron);
                newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        } else if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(DecayNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron oldNeuron = (Neuron) neuronList.get(i);
                DecayNeuron newNeuron = new DecayNeuron(oldNeuron);
                newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        } else if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(IACNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron oldNeuron = (Neuron) neuronList.get(i);
                IACNeuron newNeuron = new IACNeuron(oldNeuron);
                newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        } else if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(ThreeValuedNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron oldNeuron = (Neuron) neuronList.get(i);
                ThreeValuedNeuron newNeuron = new ThreeValuedNeuron(oldNeuron);
                newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        }  else if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(LMSNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron oldNeuron = (Neuron) neuronList.get(i);
                LMSNeuron newNeuron = new LMSNeuron(oldNeuron);
                newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        }  else if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(TraceNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron oldNeuron = (Neuron) neuronList.get(i);
                TraceNeuron newNeuron = new TraceNeuron(oldNeuron);
                newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        } else if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(ExponentialDecayNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron oldNeuron = (Neuron) neuronList.get(i);
                ExponentialDecayNeuron newNeuron = new ExponentialDecayNeuron(oldNeuron);
                newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        } else if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(RunningAverageNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron oldNeuron = (Neuron) neuronList.get(i);
                RunningAverageNeuron newNeuron = new RunningAverageNeuron(oldNeuron);
                newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        } else if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(TemporalDifferenceNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron oldNeuron = (Neuron) neuronList.get(i);
                TemporalDifferenceNeuron newNeuron = new TemporalDifferenceNeuron(oldNeuron);
                newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        } else if (cbNeuronType.getSelectedItem().toString().equalsIgnoreCase(PointNeuron.getName())) {
            for (int i = 0; i < neuronList.size(); i++) {
                    Neuron oldNeuron = (Neuron) neuronList.get(i);
                    PointNeuron newNeuron = new PointNeuron(oldNeuron);
                    newNeuron.getParentNetwork().changeNeuron(oldNeuron, newNeuron);
            }
        } 
    }

    /**
     * Set the help page based on the currently selected neuron type.
     */
    private void updateHelp() {
        if (cbNeuronType.getSelectedItem() == NULL_STRING) {
            helpAction.setTheURL("Network/neuron.html");
        } else {
            String spacelessString = cbNeuronType.getSelectedItem().toString().replace(" ", "");
            helpAction.setTheURL("Network/neuron/" + spacelessString + ".html");
        }
    }

    /**
     * Respond to neuron type changes.
     * @param e Action event.
     */
    public void actionPerformed(final ActionEvent e) {

        neuronsHaveChanged = true;
        Neuron neuronRef = (Neuron) neuronList.get(0);
        updateHelp();

        if (cbNeuronType.getSelectedItem().equals(BinaryNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new BinaryNeuronPanel();
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } else if (cbNeuronType.getSelectedItem().equals(AdditiveNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new AdditiveNeuronPanel(neuronRef.getParentNetwork());
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } else if (cbNeuronType.getSelectedItem().equals(LinearNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new LinearNeuronPanel();
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } else if (cbNeuronType.getSelectedItem().equals(SigmoidalNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new SigmoidalNeuronPanel();
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } else if (cbNeuronType.getSelectedItem().equals(RandomNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new RandomNeuronPanel();
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } else if (cbNeuronType.getSelectedItem().equals(ClampedNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new ClampedNeuronPanel();
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } else if (cbNeuronType.getSelectedItem().equals(StochasticNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new StochasticNeuronPanel();
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } else if (cbNeuronType.getSelectedItem().equals(LogisticNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new LogisticNeuronPanel();
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } else if (cbNeuronType.getSelectedItem().equals(IntegrateAndFireNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new IntegrateAndFireNeuronPanel(neuronRef.getParentNetwork());
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } else if (cbNeuronType.getSelectedItem().equals(SinusoidalNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new SinusoidalNeuronPanel();
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } else if (cbNeuronType.getSelectedItem().equals(IzhikevichNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new IzhikevichNeuronPanel(neuronRef.getParentNetwork());
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } else if (cbNeuronType.getSelectedItem().equals(NakaRushtonNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new NakaRushtonNeuronPanel(neuronRef.getParentNetwork());
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } else if (cbNeuronType.getSelectedItem().equals(DecayNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new DecayNeuronPanel();
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } else if (cbNeuronType.getSelectedItem().equals(IACNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new IACNeuronPanel();
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } else if (cbNeuronType.getSelectedItem().equals(ThreeValuedNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new ThreeValuedNeuronPanel();
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } else if (cbNeuronType.getSelectedItem().equals(LMSNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new LMSNeuronPanel();
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } else if (cbNeuronType.getSelectedItem().equals(TraceNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new TraceNeuronPanel();
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } else if (cbNeuronType.getSelectedItem().equals(ExponentialDecayNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new ExponentialDecayNeuronPanel(neuronRef.getParentNetwork());
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } else if (cbNeuronType.getSelectedItem().equals(RunningAverageNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new RunningAverageNeuronPanel();
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } else if (cbNeuronType.getSelectedItem().equals(TemporalDifferenceNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new TemporalDifferenceNeuronPanel();
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } else if (cbNeuronType.getSelectedItem().equals(PointNeuron.getName())) {
            mainPanel.remove(neuronPanel);
            neuronPanel = new PointNeuronPanel(neuronRef.getParentNetwork());
            neuronPanel.fillDefaultValues();
            mainPanel.add(neuronPanel);
        } 

        pack();
    }

    /**
     * Set the initial values of dialog components.
     */
    private void fillFieldValues() {
        Neuron neuronRef = (Neuron) neuronList.get(0);
        tfActivation.setText(Double.toString(neuronRef.getActivation()));
        tfIncrement.setText(Double.toString(neuronRef.getIncrement()));
        tfUpdatePriority.setText(Integer.toString(neuronRef.getUpdatePriority()));
        tfLowBound.setText(Double.toString(neuronRef.getLowerBound()));
        tfUpBound.setText(Double.toString(neuronRef.getUpperBound()));

        neuronPanel.fillFieldValues();

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(neuronList, Neuron.class, "getActivation")) {
            tfActivation.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, Neuron.class, "getIncrement")) {
            tfIncrement.setText(NULL_STRING);
        }
        
        if (!NetworkUtils.isConsistent(neuronList, Neuron.class, "getUpdatePriority")) {
            tfUpdatePriority.setText(NULL_STRING);
        }
        
        if (!NetworkUtils.isConsistent(neuronList, Neuron.class, "getLowerBound")) {
            tfLowBound.setText(NULL_STRING);
        }

        if (!NetworkUtils.isConsistent(neuronList, Neuron.class, "getUpperBound")) {
            tfUpBound.setText(NULL_STRING);
        }
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {

        for (int i = 0; i < neuronList.size(); i++) {
            Neuron neuronRef = (Neuron) neuronList.get(i);

            if (!tfActivation.getText().equals(NULL_STRING)) {
                neuronRef.setActivation(Double.parseDouble(tfActivation.getText()));
            }

            if (!tfIncrement.getText().equals(NULL_STRING)) {
                neuronRef.setIncrement(Double.parseDouble(tfIncrement.getText()));
            }
            
            if (!tfUpdatePriority.getText().equals(NULL_STRING)) {
                neuronRef.setUpdatePriority(Integer.parseInt(tfUpdatePriority.getText()));
            }
            
            if (!tfUpBound.getText().equals(NULL_STRING)) {
                neuronRef.setUpperBound(Double.parseDouble(tfUpBound.getText()));
            }

            if (!tfLowBound.getText().equals(NULL_STRING)) {
                neuronRef.setLowerBound(Double.parseDouble(tfLowBound.getText()));
            }
        }

        if (neuronsHaveChanged) {
            changeNeurons();
        }

        ((Neuron) neuronList.get(0)).getParentNetwork().getRootNetwork().fireNetworkChanged();
        setNeuronList();
        neuronPanel.setNeuronList(neuronList);
        neuronPanel.commitChanges();
    }
}
