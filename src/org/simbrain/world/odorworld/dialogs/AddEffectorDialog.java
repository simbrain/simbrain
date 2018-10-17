/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.world.odorworld.dialogs;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.world.odorworld.effectors.Speech;
import org.simbrain.world.odorworld.effectors.StraightMovement;
import org.simbrain.world.odorworld.effectors.Turning;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * EffectorDialog is a dialog box for adding effectors to Odor World.
 *
 * @author Lam Nguyen
 */

public class AddEffectorDialog extends StandardDialog {

    /**
     * String of effector types.
     */
    private String[] effectors = {"StraightMovement", "Turning", "Speech"};

    /**
     * Entity to which effector is being added.
     */
    private OdorWorldEntity entity;

    /**
     * Instantiated entity to which effector is being added.
     */
    private OdorWorldEntity parentEntity;

    /**
     * Select effector type.
     */
    private JComboBox effectorType = new JComboBox(effectors);

    /**
     * Main dialog box.
     */
    private Box mainPanel = Box.createVerticalBox();

    /**
     * Panel for setting effector type.
     */
    private LabelledItemPanel typePanel = new LabelledItemPanel();

    /**
     * Effector Dialog add effector constructor.
     *
     * @param entity
     */
    public AddEffectorDialog(OdorWorldEntity entity) {
        this.entity = entity;
        this.parentEntity = entity;
        init("Add effector");
    }

    /**
     * Initialize default constructor.
     */
    private void init(String title) {
        setTitle(title);
        typePanel.addItem("Effector Type", effectorType);
        effectorType.setSelectedItem("SmellEffector");
        ShowHelpAction helpAction = new ShowHelpAction("Pages/Worlds/OdorWorld/effectors.html");
        addButton(new JButton(helpAction));
        initPanel();
        mainPanel.add(typePanel);
        setContentPane(mainPanel);
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    /**
     * Initialize the effector Dialog Panel based upon the current effector
     * type.
     */
    private void initPanel() {
        if (effectorType.getSelectedItem() == "StraightMovement") {
            setTitle("Add a straight movement effector");
        } else if (effectorType.getSelectedItem() == "Turning") {
            setTitle("Add a turning effector");
        } else if (effectorType.getSelectedItem() == "Speech") {
            setTitle("Add a speech effector");
        }
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        if (effectorType.getSelectedItem() == "StraightMovement") {
            parentEntity.addEffector(new StraightMovement(parentEntity));
        } else if (effectorType.getSelectedItem() == "Turning") {
            parentEntity.addEffector(new Turning(parentEntity, Turning.DEFAULT_LABEL, 0.0));
        } else if (effectorType.getSelectedItem() == "Speech") {
            parentEntity.addEffector(new Speech(entity));
        }
    }
}
