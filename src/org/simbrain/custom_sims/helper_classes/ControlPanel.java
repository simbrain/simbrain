package org.simbrain.custom_sims.helper_classes;

import org.simbrain.util.LabelledItemPanel;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;

/**
 * Utility for setting up a control panel in an internal frame.
 *
 * @author Jeff Yoshimi
 */
public class ControlPanel extends JPanel {

    /**
     * The internal frame all components are placed in.
     */
    private JInternalFrame internalFrame;

    /**
     * Allows for multiple labeled item panels in separate tabs.
     */
    private JTabbedPane tabbedPane;

    /**
     * Main panel, in case where there are not tabs.
     */
    private LabelledItemPanel mainPanel;

    /**
     * The main central panel, with a border layout. A bottom component can also
     * be added.
     */
    private JPanel centralPanel;

    /**
     * Construct the control panel with a specified number of tabs. Each
     * contains a labeled item panel.
     */
    public ControlPanel() {
        super(new BorderLayout());
        mainPanel = new LabelledItemPanel();
        centralPanel = new JPanel(new BorderLayout());
        centralPanel.add(BorderLayout.CENTER, mainPanel);
        JScrollPane scrollPane = new JScrollPane(centralPanel);
        scrollPane.setBorder(null);
        this.add(BorderLayout.CENTER, scrollPane);
    }

    /**
     * Add an item to the current tab.
     *
     * @param text      the text to display next to the component
     * @param component the component to display
     */
    public void addItem(String text, JComponent component) {
        if (text.equals("")) {
            mainPanel.addSpanningItem(component);
        } else {
            mainPanel.addItem(text, component);
        }
    }

    /**
     * Add a button to the control panel.
     *
     * @param buttonText name for the button itself
     * @param task       the task to run when the button is pressed
     * @return
     */
    public JButton addButton(String buttonText, Runnable task) {
        return addButton("", buttonText, task);
    }

    /**
     * Add a button to the control panel and text next to the button.
     *
     * @param buttonText  text for the button itself
     * @param buttonLabel text in the panel to the left of the button
     * @param task        the task to run when the button is pressed
     */
    public JButton addButton(String buttonText, String buttonLabel, Runnable task) {
        JButton button = new JButton(buttonLabel);
        button.addActionListener(e -> {
            Executors.newSingleThreadExecutor().execute(task);
        });
        this.addItem(buttonText, button);
        pack();
        return button;
    }

    /**
     * Add a text field to the panel.
     *
     * @param fieldLabel text in the panel to the left of the field
     * @param initText   initial text in the textfield
     * @return the text field
     */
    public JTextField addTextField(String fieldLabel, String initText) {
        JTextField tf = new JTextField(initText); // TODO: Check issue 35
        this.addItem(fieldLabel, tf);
        pack();
        return tf;
    }

    /**
     * Add a label to the panel.
     *
     * @param fieldLabel text in the panel to the left of the label
     * @param initText   initial text in the label
     * @return the label
     */
    public JLabel addLabel(String fieldLabel, String initText) {
        JLabel label = new JLabel(initText);
        this.addItem(fieldLabel, label);
        pack();
        return label;
    }

    /**
     * Add a checkbox to the panel.
     *
     * @param label   text in the panel to the left of the field
     * @param checked whether the box should initially be checked
     * @param task    task to run when clicking on the checkbox
     * @return the checkbox
     */
    public JCheckBox addCheckBox(String label, boolean checked, Runnable task) {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(checked);
        this.addItem(label, checkBox);
        checkBox.addActionListener(e -> {
            Executors.newSingleThreadExecutor().execute(task);
        });
        pack();
        return checkBox;
    }

    /**
     * Add an arbitrary component.
     *
     * @param component the component to add
     */
    public void addComponent(JComponent component) {
        mainPanel.addItem(component, 1);
    }

    /**
     * Add a horizontal separator.
     */
    public void addSeparator() {
        addComponent(new JSeparator(SwingConstants.HORIZONTAL));
    }

    /**
     * Add a component to the bottom of the control panel.
     *
     * @param bottomComponent the bottom component to add
     */
    public void addBottomComponent(JComponent bottomComponent) {
        centralPanel.add(BorderLayout.SOUTH, bottomComponent);
        pack();
    }

    /**
     * Make a control panel with a specified width and height.
     * Otherwise see {@link #makePanel(Simulation, String, int, int)}
     */
    public static ControlPanel makePanel(Simulation sim, String name, int x, int y, int width, int height) {
        ControlPanel panel = makePanel(sim, name, x, y);
        panel.internalFrame.setPreferredSize(new Dimension(width, height));
        return panel;
    }

    /**
     * Embed a control panel in an internal frame and get a reference to it.
     *
     * @param sim  reference to parent simulation
     * @param name title to display in panel frame
     * @param x    x coordinate of frame
     * @param y    y coordinate of frame
     * @return the internal frame
     */
    public static ControlPanel makePanel(Simulation sim, String name, int x, int y) {
        ControlPanel panel = new ControlPanel();
        panel.internalFrame = new JInternalFrame(name, true, true);
        panel.internalFrame.setLocation(x, y);
        panel.internalFrame.getContentPane().add(panel);
        panel.internalFrame.setVisible(true);
        panel.internalFrame.pack();
        sim.getDesktop().addInternalFrame(panel.internalFrame);
        return panel;
    }

    /**
     * Pack the internal frame, if there is one.
     */
    public void pack() {
        if (internalFrame != null) {
            internalFrame.pack();
        }
    }

}