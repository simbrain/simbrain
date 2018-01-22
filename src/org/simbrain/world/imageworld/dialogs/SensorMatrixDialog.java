package org.simbrain.world.imageworld.dialogs;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.world.imageworld.ImageWorld;
import org.simbrain.world.imageworld.SensorMatrix;
import org.simbrain.world.imageworld.filters.ImageFilter;
import org.simbrain.world.imageworld.filters.ImageFilterFactory;

/**
 * A dialog to create a new SensorMatrix.
 */
public class SensorMatrixDialog extends StandardDialog {
    private static final long serialVersionUID = 1L;

    private ImageWorld world;
    private Box mainPanel = Box.createVerticalBox();
    private LabelledItemPanel sensorMatrixPanel = new LabelledItemPanel();
    private JTextField nameField = new JTextField();
    private JComboBox<String> filterTypeCombo = new JComboBox<String>();
    private LabelledItemPanel filterPanel;
    private ImageFilterFactory filterFactory;

    /**
     * Construct a new SensorMatrixDialog for selecting parameters of a new
     * SensorMatrix.
     * @param world The ImageWorld which will hold the new SensorMatrix.
     */
    public SensorMatrixDialog(ImageWorld world) {
        this.world = world;
        setTitle("Create Sensor Matrix");
        ShowHelpAction helpAction = new ShowHelpAction("Pages/Worlds/ImageWorld/sensorMatrix.html");
        addButton(new JButton(helpAction));
        mainPanel.add(sensorMatrixPanel);
        for (String type : ImageFilterFactory.getTypes()) {
            ImageFilterFactory.getFactory(type).setDefaultValues();
            filterTypeCombo.addItem(type);
        }
        filterTypeCombo.addActionListener((evt) -> {
            if (filterPanel != null) {
                mainPanel.remove(filterPanel);
            }
            filterFactory = ImageFilterFactory.getFactory((String) filterTypeCombo.getSelectedItem());
            filterPanel = filterFactory.getEditorPanel();
            mainPanel.add(filterPanel);
            filterPanel.revalidate();
            pack();
        });
        filterTypeCombo.setSelectedIndex(0);
        sensorMatrixPanel.addItem("Name", nameField);
        sensorMatrixPanel.addItem("Filter", filterTypeCombo);
        mainPanel.add(filterPanel);
        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(null);
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    /** Called externally when the dialog is closed, to commit any changes made. */
    public void commitChanges() {
        String name = nameField.getText();
        ImageFilter filter = filterFactory.create(world.getCompositeImageSource());
        SensorMatrix matrix = new SensorMatrix(name, filter);
        world.addSensorMatrix(matrix);
    }
}
