package org.simbrain.network.dialog.synapse;

import java.awt.BorderLayout;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simnet.interfaces.SpikeResponse;


public class SpikeResponsePanel extends JPanel{

    private LabelledItemPanel topPanel = new LabelledItemPanel();
    private TristateDropDown cbScaleByPSPDiff = new TristateDropDown();
    private JTextField tfPSRestingPotential = new JTextField();
    private JComboBox cbSpikeResponseType = new JComboBox(SpikeResponse.getTypeList());
    private ArrayList spikerList = new ArrayList();
    private ArrayList selectionList = new ArrayList();
    
    
    public SpikeResponsePanel(){
        this.setLayout(new BorderLayout());
    }
}
