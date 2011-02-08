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

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.util.ComboBoxRenderer;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.environment.SmellSourcePanel;
import org.simbrain.util.propertyeditor.ReflectivePropertyEditor;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * <b>DialogWorldEntity</b> displays the dialog box for settable values of
 * creatures and entities within a world environment.
 */
public class DialogOdorWorldEntity extends StandardDialog implements
        ActionListener {

    private static final long serialVersionUID = 1L;

    /** The dimension for the combobox renderer. */
    private final int cbRendererDimension = 35;

    /** The entity for which this dialog is called. */
    private OdorWorldEntity entityRef;

    /** The visual container for the sub panels. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** The text field containing the name of the entity. */
    private JTextField tfEntityName = new JTextField();

    /** The Combobox from which to choose the entity image. */
    private JComboBox cbImageName = new JComboBox();

    /** The renderer to display the combobox. */
    private ComboBoxRenderer cbRenderer = new ComboBoxRenderer();

    /** The panel containing item-specific information not in other panels. */
    private LabelledItemPanel miscPanel = new LabelledItemPanel();

    /** Property editor for main entity properties. */
    private ReflectivePropertyEditor mainEditor;

    /**
     * The text field containing the number of bites until the item dies
     * (absolute, not remaining).
     */
    private JTextField bitesToDie = new JTextField();

    /** The checkbox identifying whether or not the item is edible. */
    private JCheckBox edible = new JCheckBox();

    /** The probability of a resurrection each turn. */
    private JTextField resurrectionProb = new JTextField();

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Editor panel for smell source. */
    SmellSourcePanel smellPanel;

    /**
     * Create and show the world entity dialog box.
     *
     * @param we reference to the world entity whose smell signature is being
     *            adjusted
     */
    public DialogOdorWorldEntity(final OdorWorldEntity we) {
        entityRef = we;
        init();
        this.pack();
        this.setLocationRelativeTo(null);
    }

    /**
     * Create and initialize instances of panel components.
     */
    private void init() {

        this.fillFieldValues();

        // Main tab
        //tabbedPane.addTab("Main", mainPanel);
        mainEditor = new ReflectivePropertyEditor(entityRef);
        tabbedPane.addTab("Main", mainEditor);

        // Smell tabs
        if (entityRef.getSmellSource() != null) {
            smellPanel = new SmellSourcePanel(entityRef.getSmellSource());
            tabbedPane.addTab("Smell", smellPanel.getValuesPanel());
            tabbedPane.addTab("Dispersion", smellPanel.getDispersionPanel());
        }

        cbRenderer.setPreferredSize(new Dimension(cbRendererDimension,
                cbRendererDimension));
        cbImageName.setRenderer(cbRenderer);
        //mainPanel.addItem("Image", cbImageName);

        // Misc. Tabs
        bitesToDie.setColumns(2);
        edible.addActionListener(this);
        miscPanel.addItem("Edible", edible);
        miscPanel.addItem("Bites to die", bitesToDie);
        miscPanel.addItem("Resurrection Probability", resurrectionProb);
        //lifeCycleEditor =  new ReflectivePropertyEditor(entityRef.getLifeCycle());
        //tabbedPane.addTab("LifeCycle", lifeCycleEditor);

        setContentPane(tabbedPane);
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    @Override
    protected void closeDialogCancel() {
        super.closeDialogCancel();
    }

    /**
     * Fills the values within the fields of the dialog.
     */
    private void fillFieldValues() {
        if (entityRef != null) {
            tfEntityName.setText(entityRef.getName());
        }
         //
         //cbImageName.setSelectedIndex(entityRef.getImageNameIndex(entityRef.));
         //edible.setSelected(entityRef`);
         //bitesToDie.setText((new
         //Integer(entityRef.getBitesToDie())).toString());
         //bitesToDie.setEnabled(entityRef.getEdible());
         //resurrectionProb.setText("" + entityRef.getResurrectionProb());
    }

    /**
     * Commits changes to the entity that are shown in the dialog.
     */
    public void commitChanges() {
        mainEditor.commit();
        smellPanel.commitChanges();
        //lifeCycleEditor.commit();
        // entityRef.setEdible(edible.isSelected());
        //
        // if (!edible.isSelected()) {
        // entityRef.setBites(0);
        // }
        //
        // entityRef.setBitesToDie(Integer.parseInt(bitesToDie.getText()));
        // entityRef.setResurrectionProb(Double.parseDouble(resurrectionProb.getText()));

        // if (!entityRef.getName().equals(tfEntityName.getText())) {
        // if (!Utils.containsName(entityRef.getParent().getEntityNames(),
        // tfEntityName.getText())) {
        // entityRef.setName(tfEntityName.getText());
        // ArrayList a = new ArrayList();
        // a.add(entityRef);
        // entityRef.getParent().getParentWorkspace().removeAgentsFromCouplings(a);
        // entityRef.getParent().getParentWorkspace().attachAgentsToCouplings();
        // } else {
        // JOptionPane.showMessageDialog(
        // null, "The name \"" + tfEntityName.getText() + "\" already exists.",
        // "Warning", JOptionPane.ERROR_MESSAGE);
        // }
        // }

        // entityRef.setImageName(cbImageName.getSelectedItem().toString());
    }

    /**
     * Respond to button pressing events.
     *
     * @param e the ActionEvent triggering this method
     */
    public void actionPerformed(final ActionEvent e) {
        Object o = e.getSource();

        if (o == edible) {
            bitesToDie.setEnabled(edible.isSelected());
        }
    }

}
