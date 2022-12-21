package org.simbrain.util.widgets;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

/**
 * Easy to implement and customize progress window.
 */
public class ProgressWindow extends JFrame {

    private final JProgressBar progressBar;

    private final JLabel valueLabel;

    private Consumer<Integer> updateAction;

    public ProgressWindow(int maxNum, String label) {
        super();
        progressBar = new JProgressBar(0, maxNum);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        valueLabel = new JLabel(label);

        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(progressBar);
        panel.add(valueLabel);
        add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        setTitle("Progress");
    }

    public void setValue(int val) {
        progressBar.setValue(val);
    }

    public int getValue() {
        return progressBar.getValue();
    }

    public void setText(String text) {
        valueLabel.setText(text);
    }

    public String getText() {
        return valueLabel.getText();
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public JLabel getValueLabel() {
        return valueLabel;
    }

    public void close() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    public void setUpdateAction(int initVal, Consumer<Integer> updateAction) {
        this.updateAction = updateAction;
        updateAction.accept(initVal);
        pack();
    }

    public void invokeUpdateAction(int i) {
        updateAction.accept(i);
    }
}
