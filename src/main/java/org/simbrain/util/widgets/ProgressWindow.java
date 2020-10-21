package org.simbrain.util.widgets;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

public class ProgressWindow extends JFrame {

    JProgressBar progressBar;

    JLabel fitnessScore;

    public ProgressWindow(int maxGeneration) {
        super();
        progressBar = new JProgressBar(0, maxGeneration);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        fitnessScore = new JLabel("Fitness Score");

        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(progressBar);
        panel.add(fitnessScore);
        add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        setTitle("Evolution Progress");
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public JLabel getFitnessScore() {
        return fitnessScore;
    }

    public void close() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }
}
