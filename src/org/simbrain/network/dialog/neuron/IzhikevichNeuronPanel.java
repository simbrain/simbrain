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

import java.util.ArrayList;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.network.dialog.RandomPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simnet.interfaces.Network;
import org.simnet.neurons.IzhikevichNeuron;

/**
 * 
 * <b>IzhikevichNeuronPanel</b>
 */
public class IzhikevichNeuronPanel extends AbstractNeuronPanel {

    private JTextField tfA = new JTextField();
    private JTextField tfB = new JTextField();
    private JTextField tfC = new JTextField();
    private JTextField tfD = new JTextField();
    private JTextField tfTimeStep = new JTextField();
    private TristateDropDown tsNoise = new TristateDropDown();
    private JTabbedPane tabbedPane = new JTabbedPane();
    private LabelledItemPanel mainTab = new LabelledItemPanel();
    private RandomPanel randTab = new RandomPanel(true);
    
    public IzhikevichNeuronPanel(Network net){

        parentNet = net;
        
        this.add(tabbedPane);
        mainTab.addItem("Time step", tfTimeStep);
        mainTab.addItem("A", tfA);
        mainTab.addItem("B", tfB);
        mainTab.addItem("C", tfC);
        mainTab.addItem("D", tfD);
        mainTab.addItem("Add noise", tsNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
    }
    
    public void fillFieldValues() {
        IzhikevichNeuron neuron_ref = (IzhikevichNeuron)neuron_list.get(0);
        
        tfTimeStep.setText(Double.toString(parentNet.getTimeStep()));
        
        tfA.setText(Double.toString(neuron_ref.getA()));
        tfB.setText(Double.toString(neuron_ref.getB()));
        tfC.setText(Double.toString(neuron_ref.getC()));
        tfD.setText(Double.toString(neuron_ref.getD()));
        tsNoise.setSelected(neuron_ref.getAddNoise());

        //Handle consistency of multiple selections
        if(!NetworkUtils.isConsistent(neuron_list, IzhikevichNeuron.class, "getA")) {
            tfA.setText(NULL_STRING);
        }
        if(!NetworkUtils.isConsistent(neuron_list, IzhikevichNeuron.class, "getB")) {
            tfB.setText(NULL_STRING);
        }
        if(!NetworkUtils.isConsistent(neuron_list, IzhikevichNeuron.class, "getC")) {
            tfC.setText(NULL_STRING);
        }
        if(!NetworkUtils.isConsistent(neuron_list, IzhikevichNeuron.class, "getD")) {
            tfD.setText(NULL_STRING);
        }
        if(!NetworkUtils.isConsistent(neuron_list, IzhikevichNeuron.class, "getAddNoise")) {
            tsNoise.setNull();
        }
        randTab.fillFieldValues(getRandomizers());
    }

    private ArrayList getRandomizers() {
        ArrayList ret = new ArrayList();
        for (int i = 0; i < neuron_list.size(); i++) {
            ret.add(((IzhikevichNeuron)neuron_list.get(i)).getNoiseGenerator());
        }
        return ret;
    }
    
    public void fillDefaultValues() {
        IzhikevichNeuron neuron_ref = new IzhikevichNeuron();
        tfTimeStep.setText(Double.toString(parentNet.getTimeStep()));
        tfA.setText(Double.toString(neuron_ref.getA()));
        tfB.setText(Double.toString(neuron_ref.getB()));
        tfC.setText(Double.toString(neuron_ref.getC()));
        tfD.setText(Double.toString(neuron_ref.getD()));
        tsNoise.setSelected(neuron_ref.getAddNoise());
        randTab.fillDefaultValues();
    }

    public void commitChanges() {
        
        parentNet.setTimeStep(Double.parseDouble(tfTimeStep.getText()));
        
        for (int i = 0; i < neuron_list.size(); i++) {
            IzhikevichNeuron neuron_ref = (IzhikevichNeuron) neuron_list.get(i);

            if (tfA.getText().equals(NULL_STRING) == false) {
                neuron_ref.setA(Double.parseDouble(tfA
                        .getText()));
            }
            if (tfB.getText().equals(NULL_STRING) == false) {
                neuron_ref.setB(Double.parseDouble(tfB
                        .getText()));
            }
            if (tfC.getText().equals(NULL_STRING) == false) {
                neuron_ref.setC(Double.parseDouble(tfC
                        .getText()));
            }
            if (tfD.getText().equals(NULL_STRING) == false) {
                neuron_ref.setD(Double.parseDouble(tfD
                        .getText()));
            }
            if (tsNoise.isNull() == false) {
                neuron_ref.setAddNoise(tsNoise.isSelected());
            }
            randTab.commitRandom(neuron_ref.getNoiseGenerator());
        }

    }

}
