package org.simbrain.world.odorworld.dialogs;

import org.simbrain.util.StandardDialog;
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
    private final OdorWorldEntity entityRef;

    /**
     * Property editor for main entity properties.
     */
    private AnnotatedPropertyEditor mainEditor;

    /**
     * Tabbed pane.
     */
    private final JTabbedPane tabbedPane = new JTabbedPane();

    /**
     * Editor panel for smell source.
     */
    private AnnotatedPropertyEditor smellPanel;

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

        mainEditor = new AnnotatedPropertyEditor<>(entityRef);

        tabbedPane.addTab("Main", new JScrollPane(mainEditor));

        // Smell tabs
        entityRef.getSmellSource();
        smellPanel = new AnnotatedPropertyEditor<>(entityRef.getSmellSource());
        tabbedPane.addTab("Smell", smellPanel);

        // Sensor / effector display
        if (entityRef.isSensorsEnabled()) {
            tabbedPane.addTab("Sensors", new SensorEffectorPanel(entityRef, SensorEffectorPanel.PanelType.Sensor, this));
        }
        if (entityRef.isEffectorsEnabled()) {
            tabbedPane.addTab("Effectors", new SensorEffectorPanel(entityRef, SensorEffectorPanel.PanelType.Effector, this));
        }

        setContentPane(tabbedPane);

        ShowHelpAction helpAction = new ShowHelpAction("https://docs.simbrain.net/docs/worlds/odorworld.html");
        addButton(new JButton(helpAction));
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        EntityType oldType = entityRef.getEntityType();
        mainEditor.commitChanges();
        entityRef.getEvents().getTypeChanged().fire(oldType, entityRef.getEntityType());
        if (smellPanel != null) {
            smellPanel.commitChanges();
        }
        entityRef.getEvents().getPropertyChanged().fire();
    }

    @Override
    protected void closeDialogCancel() {
        super.closeDialogCancel();
    }

}
