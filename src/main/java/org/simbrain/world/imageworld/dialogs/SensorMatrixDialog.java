package org.simbrain.world.imageworld.dialogs;

import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.world.imageworld.ImageSourceProperties;
import org.simbrain.world.imageworld.ImageWorld;
import org.simbrain.world.imageworld.SensorMatrix;
import org.simbrain.world.imageworld.filters.FilteredImageSource;

import javax.swing.*;

/**
 * A dialog to create a new SensorMatrix.
 */
public class SensorMatrixDialog extends StandardDialog {

    private ImageWorld world;
    private AnnotatedPropertyEditor editorPanel;
    private ImageSourceProperties imageSourceMeta = new ImageSourceProperties();

    /**
     * Construct a new SensorMatrixDialog for selecting parameters of a new
     * SensorMatrix.
     *
     * @param world The ImageWorld which will hold the new SensorMatrix.
     */
    public SensorMatrixDialog(ImageWorld world) {
        this.world = world;
        setTitle("Create Sensor Matrix");
        ShowHelpAction helpAction = new ShowHelpAction("Pages/Worlds/ImageWorld/sensorMatrix.html");
        addButton(new JButton(helpAction));

        imageSourceMeta.setName("Filter " + (world.getSensorMatrices().size() + 1));

        editorPanel = new AnnotatedPropertyEditor(imageSourceMeta);
        Box mainPanel = Box.createVerticalBox();
        mainPanel.add(editorPanel);

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(null);
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        editorPanel.commitChanges();
        String name = imageSourceMeta.getName();
        // FilteredImageSource filter = filterFactory.create(world.getCompositeImageSource());
        FilteredImageSource filter = new FilteredImageSource(
                world.getImageSource(),
                imageSourceMeta.getColorOp(),
                imageSourceMeta.getWidth(),
                imageSourceMeta.getHeight()
        );
        SensorMatrix matrix = new SensorMatrix(name, filter);
        world.addSensorMatrix(matrix);
    }
}
