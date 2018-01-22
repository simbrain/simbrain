package org.simbrain.world.threedworld.entities;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;

import org.simbrain.world.threedworld.entities.EditorDialog.Editor;

public class SensorEditor implements Editor {
    private Agent agent;
    private Sensor sensor;
    private JPanel panel;
    
    public SensorEditor(Agent agent, Sensor sensor) {
        this.agent = agent;
        this.sensor = sensor;
    }
    
    public Agent getAgent() {
        return agent;
    }
    
    public Sensor getSensor() {
        return sensor;
    }
    
    public JPanel getPanel() {
        return panel;
    }
    
    @Override
    public JComponent layoutFields() {
        panel = new JPanel();
        panel.setLayout(new MigLayout());
        panel.setBorder(new TitledBorder(sensor.getClass().getSimpleName()));
        return panel;
    }
    
    @Override
    public void readValues() {}
    
    @Override
    public void writeValues() {}
    
    @Override
    public void close() {}
}
