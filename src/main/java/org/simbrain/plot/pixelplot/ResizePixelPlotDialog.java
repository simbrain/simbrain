package org.simbrain.plot.pixelplot;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.ShowHelpAction;

import javax.swing.*;
import java.text.NumberFormat;

public class ResizePixelPlotDialog extends StandardDialog {
    private static final long serialVersionUID = 1L;

    private PixelPlot world;
    private Box mainPanel = Box.createVerticalBox();
    private LabelledItemPanel pixelPlotPanel = new LabelledItemPanel();
    // private JCheckBox useColorCheckBox = new JCheckBox();
    private JFormattedTextField widthField = new JFormattedTextField(NumberFormat.getIntegerInstance());
    private JFormattedTextField heightField = new JFormattedTextField(NumberFormat.getIntegerInstance());

    /**
     * Construct a new ResizePixelPlotDialog.
     *
     * @param world The ImageWorld which holds the PixelPlot.
     */
    public ResizePixelPlotDialog(PixelPlot world) {
        this.world = world;
        setTitle("Pixel Plot Settings");
        ShowHelpAction helpAction = new ShowHelpAction("Pages/Worlds/ImageWorld/pixelPlot.html");
        addButton(new JButton(helpAction));
        mainPanel.add(pixelPlotPanel);
        // pixelPlotPanel.addItem("Use Color", useColorCheckBox);
        pixelPlotPanel.addItem("Width", widthField);
        pixelPlotPanel.addItem("Height", heightField);
        fillFieldValues();
        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(null);
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    public void fillFieldValues() {
        // useColorCheckBox.setSelected(world.getUseColorEmitter());
        widthField.setValue(world.getImage().getWidth());
        heightField.setValue(world.getImage().getHeight());
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        // boolean useColor = useColorCheckBox.isSelected();
        int width = Integer.parseInt(widthField.getText());
        int height = Integer.parseInt(heightField.getText());
        // world.setUseColorEmitter(useColor);
        world.setSize(width, height);
    }
}
