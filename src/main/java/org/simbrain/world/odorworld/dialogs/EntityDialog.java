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
package org.simbrain.world.odorworld.dialogs;

import org.simbrain.util.StandardDialog;
import org.simbrain.util.environment.SmellSourcePanel;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

import javax.swing.*;

/**
 * <b>DialogWorldEntity</b> displays the dialog box for settable values of
 * creatures and entities within a world environment.
 */
public class EntityDialog extends StandardDialog {

    /**
     * The entity for which this dialog is called.
     */
    private OdorWorldEntity entityRef;

    /**
     * Property editor for main entity properties.
     */
    private AnnotatedPropertyEditor mainEditor;

    /**
     * Tabbed pane.
     */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /**
     * Editor panel for smell source.
     */
    private SmellSourcePanel smellPanel;

    /**
     * Create and show the world entity dialog box.
     *
     * @param we reference to the world entity whose smell signature is being
     *           adjusted
     */
    public EntityDialog(final OdorWorldEntity we) {
        entityRef = we;
        init();
    }

    /**
     * Create and initialize instances of panel components.
     */
    private void init() {

        mainEditor = new AnnotatedPropertyEditor(entityRef);

        fillFieldValues();

        tabbedPane.addTab("Main", new JScrollPane(mainEditor));

        // Smell tabs
        if (entityRef.getSmellSource() != null) {
            smellPanel = new SmellSourcePanel(entityRef.getSmellSource());
            tabbedPane.addTab("Smell", smellPanel.getValuesPanel());
            tabbedPane.addTab("Dispersion", smellPanel.getDispersionPanel());
        }

        // Sensor / effector display
        if (entityRef.isSensorsEnabled()) {
            tabbedPane.addTab("Sensors", new SensorEffectorPanel(entityRef, SensorEffectorPanel.PanelType.Sensor, this));
        }
        if (entityRef.isEffectorsEnabled()) {
            tabbedPane.addTab("Effectors", new SensorEffectorPanel(entityRef, SensorEffectorPanel.PanelType.Effector, this));
        }

        setContentPane(tabbedPane);

        ShowHelpAction helpAction = new ShowHelpAction("Pages/Worlds/OdorWorld/OdorWorld.html");
        addButton(new JButton(helpAction));
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        EntityType oldType = entityRef.getEntityType();
        mainEditor.commitChanges();
        entityRef.getEvents().fireTypeChanged(oldType, entityRef.getEntityType());
        if (smellPanel != null) {
            smellPanel.commitChanges();
        }
    }

    @Override
    protected void closeDialogCancel() {
        super.closeDialogCancel();
    }

    /**
     * Fills the values within the fields of the dialog.
     */
    private void fillFieldValues() {
        if (mainEditor != null) {
            mainEditor.fillFieldValues();
        }
    }


}
