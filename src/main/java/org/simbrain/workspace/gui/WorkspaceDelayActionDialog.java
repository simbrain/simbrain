package org.simbrain.workspace.gui;

import org.simbrain.util.StandardDialog;
import org.simbrain.workspace.Workspace;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * <b>WorkspaceDelayActionDialog</b> is a dialog box for setting the workspace update delay.
 */
public class WorkspaceDelayActionDialog extends StandardDialog {

    /**
     * Reference to workspace.
     **/
    private final Workspace workspace;

    /**
     * This method is the default constructor.
     *
     * @param workspace reference to parent workspace.
     */
    public WorkspaceDelayActionDialog(Workspace workspace) {
        this.workspace = workspace;
        setupEditorPanel();
        setModal(true);
        setLocationRelativeTo(null);
        pack();
    }

    /**
     * This method initializes the components on the panel.
     */
    private void setupEditorPanel() {
        // Initialize Dialog
        setTitle("Workspace Update Delay");
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        setContentPane(panel);

        JLabel delayLabel = new JLabel("Delay (ms)");
        JSlider delaySlider = new JSlider(JSlider.HORIZONTAL, 0, 500, workspace.getUpdateDelay());
        JLabel delayValueLabel = new JLabel("" + workspace.getUpdateDelay());
        delayValueLabel.setPreferredSize(new Dimension(50, 10));
        delaySlider.setToolTipText("Use this to delay simulation speed. " + "This will make it easier to see very fast updates.");
        delaySlider.setMajorTickSpacing(100);
        delaySlider.setPaintTicks(true);
        delaySlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                workspace.setUpdateDelay(source.getValue());
                delayValueLabel.setText("" + source.getValue());
            }
        });
        panel.add(delayLabel);
        panel.add(delaySlider);
        panel.add(delayValueLabel);
        panel.setBorder(null);
    }

}
