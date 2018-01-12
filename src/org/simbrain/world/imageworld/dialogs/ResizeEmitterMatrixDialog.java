package org.simbrain.world.imageworld.dialogs;

import javax.swing.*;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.world.imageworld.ImageWorld;

public class ResizeEmitterMatrixDialog extends StandardDialog {
    private static final long serialVersionUID = 1L;

    private ImageWorld world;
    private Box mainPanel = Box.createVerticalBox();
    private LabelledItemPanel emitterMatrixPanel = new LabelledItemPanel();
    private JCheckBox useColorCheckBox = new JCheckBox();
    private JTextField widthField = new JTextField("10");
    private JTextField heightField = new JTextField("10");

    /**
     * Construct a new ResizeEmitterMatrixDialog.
     * @param world The ImageWorld which holds the EmitterMatrix.
     */
    public ResizeEmitterMatrixDialog(ImageWorld world) {
        this.world = world;
        setTitle("Emitter Matrix Settings");
        ShowHelpAction helpAction = new ShowHelpAction("Pages/Worlds/ImageWorld/emitterMatrix.html");
        addButton(new JButton(helpAction));
        mainPanel.add(emitterMatrixPanel);
        emitterMatrixPanel.addItem("Use Color", useColorCheckBox);
        emitterMatrixPanel.addItem("Width", widthField);
        emitterMatrixPanel.addItem("Height", heightField);
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
        boolean useColor = useColorCheckBox.isSelected();
        int width = Integer.parseInt(widthField.getText());
        int height = Integer.parseInt(heightField.getText());
        world.setUseColorEmitter(useColor);
        world.resizeEmitterMatrix(width, height);
    }
}
