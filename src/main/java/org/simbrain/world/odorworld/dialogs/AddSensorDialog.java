package org.simbrain.world.odorworld.dialogs;

import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.APEObjectWrapper;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditorKt;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.Sensor;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import javax.swing.*;
import java.awt.*;

/**
 * SensorDialog is a dialog box for adding Sensors to Odor World.
 *
 * @author Lam Nguyen
 */

public class AddSensorDialog extends StandardDialog {

    /**
     * Entity to which sensor is being added.
     */
    private final OdorWorldEntity entity;

    /**
     * Main editing panel.
     */
    private AnnotatedPropertyEditor<APEObjectWrapper<Sensor>> sensorCreatorPanel;

    /**
     * Main dialog box.
     */
    private final Box mainPanel = Box.createVerticalBox();

    /**
     * Sensor Dialog add sensor constructor.
     *
     * @param entity entity to add sensor to
     * @param parent parent window for proper dialog stacking
     */
    public AddSensorDialog(OdorWorldEntity entity, Window parent) {
        super(parent, "Add Sensor");
        this.entity = entity;
        init("Add Sensor");
    }
    
    /**
     * Sensor Dialog add sensor constructor (without parent).
     *
     * @param entity
     */
    public AddSensorDialog(OdorWorldEntity entity) {
        super();
        this.entity = entity;
        init("Add Sensor");
    }

    /**
     * Initialize default constructor.
     */
    private void init(String title) {
        setTitle(title);
        ShowHelpAction helpAction = new ShowHelpAction("https://docs.simbrain.net/docs/worlds/odorworld.html");
        addButton(new JButton(helpAction));
        sensorCreatorPanel = new AnnotatedPropertyEditor<>(AnnotatedPropertyEditorKt.objectWrapper("Add Sensor",
                new SmellSensor()));
        mainPanel.add(sensorCreatorPanel);
        setContentPane(mainPanel);
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        sensorCreatorPanel.commitChanges();
        entity.addSensor(AnnotatedPropertyEditorKt.getWrapperWidgetValue(sensorCreatorPanel));
    }

}
