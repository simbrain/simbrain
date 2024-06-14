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

import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.APEObjectWrapper;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditorKt;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.effectors.StraightMovement;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

import javax.swing.*;

/**
 * EffectorDialog is a dialog box for adding effectors to Odor World.
 *
 * @author Lam Nguyen
 */

public class AddEffectorDialog extends StandardDialog {

    /**
     * Entity to which effector is being added.
     */
    private OdorWorldEntity entity;

    /**
     * Main editing panel.
     */
    private AnnotatedPropertyEditor<APEObjectWrapper<Effector>> effectorCreatorPanel;

    /**
     * Main dialog box.
     */
    private Box mainPanel = Box.createVerticalBox();

    /**
     * Effector Dialog add effector constructor.
     *
     * @param entity
     */
    public AddEffectorDialog(OdorWorldEntity entity) {
        this.entity = entity;
        init("Add effector");
    }

    /**
     * Initialize default constructor.
     */
    private void init(String title) {
        setTitle(title);
        ShowHelpAction helpAction = new ShowHelpAction("Pages/Worlds/OdorWorld/effectors.html");
        addButton(new JButton(helpAction));
        effectorCreatorPanel = new AnnotatedPropertyEditor(AnnotatedPropertyEditorKt.objectWrapper("Add Effector",
                new StraightMovement()));
        mainPanel.add(effectorCreatorPanel);
        setContentPane(mainPanel);
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        effectorCreatorPanel.commitChanges();
        entity.addEffector(AnnotatedPropertyEditorKt.getWrapperWidgetValue(effectorCreatorPanel));
    }

}
