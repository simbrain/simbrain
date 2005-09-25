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

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.network.dialog.RandomPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simnet.neurons.DecayNeuron;

/**
 * 
 * <b>DecayNeuronPanel</b>
 */
public class DecayNeuronPanel extends AbstractNeuronPanel implements ActionListener {

    private TristateDropDown cbRelAbs = new TristateDropDown("Relative", "Absolute");
    private JTextField tfDecayAmount = new JTextField();
    private JTextField tfDecayFraction = new JTextField();
    private JTextField tfBaseLine = new JTextField();
    private JTabbedPane tabbedPane = new JTabbedPane();
    private LabelledItemPanel mainTab = new LabelledItemPanel();
    private RandomPanel randTab = new RandomPanel(true);
    private TristateDropDown isClipping = new TristateDropDown();
    private TristateDropDown isAddNoise = new TristateDropDown();
    
    public DecayNeuronPanel(){

        cbRelAbs.addActionListener(this);
        cbRelAbs.setActionCommand("relAbs");
        
        this.add(tabbedPane);
        mainTab.addItem("", cbRelAbs);
        mainTab.addItem("Base line", tfBaseLine);
        mainTab.addItem("Decay amount", tfDecayAmount);
        mainTab.addItem("Decay fraction", tfDecayFraction);
        mainTab.addItem("Use clipping", isClipping);
        mainTab.addItem("Add noise", isAddNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
        checkBounds();
    }
    
    public void actionPerformed(ActionEvent e){

        if(e.getActionCommand().equals("relAbs")){
            checkBounds();
        }
    }
    
    private void checkBounds(){
        if (cbRelAbs.getSelectedIndex() == 0){
            tfDecayAmount.setEnabled(false);
            tfDecayFraction.setEnabled(true);
        } else {
            tfDecayFraction.setEnabled(false);
            tfDecayAmount.setEnabled(true);
        }
    }
     
     /**
     * Populate fields with current data
     */
    public void fillFieldValues() {
        
        DecayNeuron neuron_ref = (DecayNeuron)neuron_list.get(0);
        
        cbRelAbs.setSelectedIndex(neuron_ref.getRelAbs());
        tfBaseLine.setText(Double.toString(neuron_ref.getBaseLine()));
        tfDecayAmount.setText(Double.toString(neuron_ref.getDecayAmount()));
        tfDecayFraction.setText(Double.toString(neuron_ref.getDecayFraction()));
        isClipping.setSelected(neuron_ref.getClipping());
        isAddNoise.setSelected(neuron_ref.getAddNoise());

        //Handle consistency of multiple selections
        if(!NetworkUtils.isConsistent(neuron_list, DecayNeuron.class, "getRelAbs")) {
            cbRelAbs.setNull();
        }
        if(!NetworkUtils.isConsistent(neuron_list, DecayNeuron.class, "getBaseLine")) {
            tfBaseLine.setText(NULL_STRING);
        }
        if(!NetworkUtils.isConsistent(neuron_list, DecayNeuron.class, "getDecayFraction")) {
            tfDecayFraction.setText(NULL_STRING);
        }
        if(!NetworkUtils.isConsistent(neuron_list, DecayNeuron.class, "getDecayAmount")) {
            tfDecayAmount.setText(NULL_STRING);
        }
        if(!NetworkUtils.isConsistent(neuron_list, DecayNeuron.class, "getClipping")){
            isClipping.setNull();
        }
        if(!NetworkUtils.isConsistent(neuron_list, DecayNeuron.class, "getAddNoise")) {
            isAddNoise.setNull();
        }
        randTab.fillFieldValues(getRandomizers());
    }
    
    private ArrayList getRandomizers() {
        ArrayList ret = new ArrayList();
        for (int i = 0; i < neuron_list.size(); i++) {
            ret.add(((DecayNeuron)neuron_list.get(i)).getNoiseGenerator());
        }
        return ret;
    }

    /**
     * Fill field values to default values for additive neuron
     *
     */
    public void fillDefaultValues() {
        DecayNeuron neuron_ref = new DecayNeuron();
        cbRelAbs.setSelectedIndex(neuron_ref.getRelAbs());
        tfBaseLine.setText(Double.toString(neuron_ref.getBaseLine()));
        tfDecayAmount.setText(Double.toString(neuron_ref.getDecayFraction()));
        tfDecayFraction.setText(Double.toString(neuron_ref.getDecayFraction()));
        isClipping.setSelected(neuron_ref.getClipping());
        isAddNoise.setSelected(neuron_ref.getAddNoise());
        randTab.fillDefaultValues();
    }
    
    
    /**
     * Called externally when the dialog is closed, to commit any changes made
     */
    public void commitChanges() {

        for (int i = 0; i < neuron_list.size(); i++) {
            DecayNeuron neuron_ref = (DecayNeuron) neuron_list.get(i);

            if (cbRelAbs.isNull() == false) {
                neuron_ref.setRelAbs(cbRelAbs.getSelectedIndex());
            }
            if (tfDecayAmount.getText().equals(NULL_STRING) == false) {
                neuron_ref.setDecayAmount(Double.parseDouble(tfDecayAmount
                        .getText()));
            }
            if (tfBaseLine.getText().equals(NULL_STRING) == false) {
                neuron_ref.setBaseLine(Double.parseDouble(tfBaseLine
                        .getText()));
            }
            if (tfDecayFraction.getText().equals(NULL_STRING) == false) {
                neuron_ref.setDecayFraction(Double.parseDouble(tfDecayFraction
                        .getText()));
            }
            if (isClipping.isNull() == false){
                neuron_ref.setClipping(isClipping.isSelected());
            }
            if (isAddNoise.isNull() == false) {
                neuron_ref.setAddNoise(isAddNoise.isSelected());
            }
            randTab.commitRandom(neuron_ref.getNoiseGenerator());
        }

    }

}
