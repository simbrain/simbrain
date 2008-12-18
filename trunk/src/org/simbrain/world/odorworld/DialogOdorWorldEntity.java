/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
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
package org.simbrain.world.odorworld;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.simbrain.util.ComboBoxRenderer;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;
import org.simbrain.util.environment.SmellSourcePanel;
import org.simbrain.world.odorworld.entities.StaticEntity;


/**
 * <b>DialogWorldEntity</b> displays the dialog box for settable values of creatures and entities within a world
 * environment.
 */
public class DialogOdorWorldEntity extends StandardDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    /** The dimension for the combobox renderer. */
    private final int cbRendererDimension = 35;

    /** The panel containing the items that are not specific to any other panels. */
    private LabelledItemPanel topPanel = new LabelledItemPanel();

    /** The entity for which this dialog is called. */
    private StaticEntity entityRef = null;

    /** The visual container for the sub panels. */
    private Box mainPanel = Box.createVerticalBox();

    /** The text field containing the name of the entity. */
    private JTextField tfEntityName = new JTextField();

    /** The Combobox from which to choose the entity image. */
    //private JComboBox cbImageName = new JComboBox(StaticEntity.imagesRenderer());
    //private JComboBox cbImageName = new JComboBox(null);

    /** The renderer to display the combobox. */
    private ComboBoxRenderer cbRenderer = new ComboBoxRenderer();

    /** The panel containing stimulus information. */
    private SmellSourcePanel stimPanel = null;

    /** The panel containing information pertaining to agents. */
    private PanelAgent agentPanel = null;

    /** The panel containing item-specific information not in other panels. */
    private LabelledItemPanel miscPanel = new LabelledItemPanel();

    /** The text field containing the number of bites until the item dies (absolute, not remaining). */
    private JTextField bitesToDie = new JTextField();

    /** The checkbox identifying whether or not the item is edible. */
    private JCheckBox edible = new JCheckBox();

    /** The probability of a resurrection each turn. */
    private JTextField resurrectionProb = new JTextField();

    /**
     * Create and show the world entity dialog box.
     *
     * @param we reference to the world entity whose smell signature is being adjusted
     */
    public DialogOdorWorldEntity(final StaticEntity we) {
        entityRef = we;
        init();
        this.pack();
        this.setLocationRelativeTo(null);
    }

    /**
     * Create and initialise instances of panel componets.
     */
    private void init() {
        this.fillFieldValues();

//        topPanel.addItem("Image", cbImageName);
//
//        bitesToDie.setColumns(2);
//        edible.addActionListener(this);
//
//        cbRenderer.setPreferredSize(new Dimension(cbRendererDimension, cbRendererDimension));
//        cbImageName.setRenderer(cbRenderer);

        // TODO!
//        if (entityRef instanceof OdorWorldAgent) {
//            setTitle("Entity Dialog - " + entityRef.getName());
//            topPanel.addItem("Entity", tfEntityName);
//            setStimPanel(new StimulusVectorPanel(entityRef));
//            setAgentPanel(new PanelAgent((OdorWorldAgent) entityRef));
//            getStimPanel().getTabbedPane().addTab("Agent", getAgentPanel());
//            mainPanel.add(topPanel);
//            mainPanel.add(getStimPanel());
//            setContentPane(mainPanel);
//        } else {
//            setTitle("Entity Dialog");
//            setStimPanel(new StimulusVectorPanel(entityRef));
//            mainPanel.add(topPanel);
//            mainPanel.add(getStimPanel());
//            setContentPane(mainPanel);
//        }

        miscPanel.addItem("Edible", edible);
        miscPanel.addItem("Bites to die", bitesToDie);
        miscPanel.addItem("Resurrection Probability", resurrectionProb);
        //getStimPanel().getTabbedPane().addTab("Miscellaneous", miscPanel);
    }

    /**
     * Fills the values within the fields of the dialog.
     */
    private void fillFieldValues() {
//        tfEntityName.setText(entityRef.getName());
////        cbImageName.setSelectedIndex(entityRef.getImageNameIndex(entityRef.getImageName()));
//        edible.setSelected(entityRef.getEdible());
//        bitesToDie.setText((new Integer(entityRef.getBitesToDie())).toString());
//        bitesToDie.setEnabled(entityRef.getEdible());
//        resurrectionProb.setText("" + entityRef.getResurrectionProb());
    }

    /**
     * Commits changes to the entity that are shown in the dialog.
     */
    public void commitChanges() {
//        entityRef.setEdible(edible.isSelected());
//
//        if (!edible.isSelected()) {
//            entityRef.setBites(0);
//        }
//
//        entityRef.setBitesToDie(Integer.parseInt(bitesToDie.getText()));
//        entityRef.setResurrectionProb(Double.parseDouble(resurrectionProb.getText()));

//        if (!entityRef.getName().equals(tfEntityName.getText())) {
//            if (!Utils.containsName(entityRef.getParent().getEntityNames(), tfEntityName.getText())) {
//                entityRef.setName(tfEntityName.getText());
//                ArrayList a = new ArrayList();
//                a.add(entityRef);
//                entityRef.getParent().getParentWorkspace().removeAgentsFromCouplings(a);
//                entityRef.getParent().getParentWorkspace().attachAgentsToCouplings();
//            } else {
//                JOptionPane.showMessageDialog(
//                                              null, "The name \"" + tfEntityName.getText() + "\" already exists.",
//                                              "Warning", JOptionPane.ERROR_MESSAGE);
//            }
//        }

//        entityRef.setImageName(cbImageName.getSelectedItem().toString());
    }

    /**
     * Respond to button pressing events.
     * @param e the ActionEvent triggering this method
     */
    public void actionPerformed(final ActionEvent e) {
        Object o = e.getSource();

        if (o == edible) {
            bitesToDie.setEnabled(edible.isSelected());
        }
    }

    /**
     * @param stimPanel The stimPanel to set.
     */
    public void setStimPanel(final SmellSourcePanel stimPanel) {
        this.stimPanel = stimPanel;
    }

    /**
     * @return Returns the stimPanel.
     */
    public SmellSourcePanel getStimPanel() {
        return stimPanel;
    }

    /**
     * @param agentPanel The agentPanel to set.
     */
    public void setAgentPanel(final PanelAgent agentPanel) {
        this.agentPanel = agentPanel;
    }

    /**
     * @return Returns the agentPanel.
     */
    public PanelAgent getAgentPanel() {
        return agentPanel;
    }
}
