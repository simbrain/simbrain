package org.simbrain.world.threedworld.entities;

import net.miginfocom.swing.MigLayout;
import org.simbrain.world.threedworld.entities.EditorDialog.Editor;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public class EffectorEditor implements Editor {
    private Agent agent;
    private Effector effector;
    private JPanel panel;

    public EffectorEditor(Agent agent, Effector effector) {
        this.agent = agent;
        this.effector = effector;
    }

    public Agent getAgent() {
        return agent;
    }

    public Effector getEffector() {
        return effector;
    }

    public JPanel getPanel() {
        return panel;
    }

    @Override
    public JComponent layoutFields() {
        panel = new JPanel();
        panel.setLayout(new MigLayout());
        panel.setBorder(new TitledBorder(effector.getClass().getSimpleName()));
        return panel;
    }

    @Override
    public void readValues() {
    }

    @Override
    public void writeValues() {
    }

    @Override
    public void close() {
    }
}
