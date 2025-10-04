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
import java.awt.*;

/**
 * EffectorDialog is a dialog box for adding effectors to Odor World.
 *
 * @author Lam Nguyen
 */

public class AddEffectorDialog extends StandardDialog {

    /**
     * Entity to which effector is being added.
     */
    private final OdorWorldEntity entity;

    /**
     * Main editing panel.
     */
    private AnnotatedPropertyEditor<APEObjectWrapper<Effector>> effectorCreatorPanel;

    /**
     * Main dialog box.
     */
    private final Box mainPanel = Box.createVerticalBox();

    /**
     * Effector Dialog add effector constructor.
     *
     * @param entity entity to add effector to
     * @param parent parent window for proper dialog stacking
     */
    public AddEffectorDialog(OdorWorldEntity entity, Window parent) {
        super(parent, "Add Effector");
        this.entity = entity;
        init("Add effector");
    }
    
    /**
     * Effector Dialog add effector constructor (without parent).
     *
     * @param entity
     */
    public AddEffectorDialog(OdorWorldEntity entity) {
        super();
        this.entity = entity;
        init("Add effector");
    }

    /**
     * Initialize default constructor.
     */
    private void init(String title) {
        setTitle(title);
        ShowHelpAction helpAction = new ShowHelpAction("https://docs.simbrain.net/docs/worlds/odorworld.html");
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
