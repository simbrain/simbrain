package org.simbrain.world.threedworld.entities;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.simbrain.world.threedworld.entities.EditorDialog.Editor;

public class AgentEditor extends ModelEditor {
    private static Map<String, Class<? extends Sensor>> sensorTypes = new HashMap<String, Class<? extends Sensor>>();
    private static Map<String, Class<? extends Effector>> effectorTypes = new HashMap<String, Class<? extends Effector>>();

    static {
        sensorTypes.put("Vision", VisionSensor.class);
        sensorTypes.put("Collision", CollisionSensor.class);
        effectorTypes.put("Walking", WalkingEffector.class);
    }

    private Agent agent;
    private JPanel sensorTab;
    private JComboBox<String> sensorTypesComboBox = new JComboBox<String>();
    private Action addSensorAction = new AbstractAction("Add") {
        @Override
        public void actionPerformed(ActionEvent event) {
            addSensor((String) sensorTypesComboBox.getSelectedItem());
        }
    };
    private JButton addSensorButton = new JButton(addSensorAction);
    private JPanel effectorTab;
    private List<SensorEditor> sensorEditors = new ArrayList<SensorEditor>();
    private List<EffectorEditor> effectorEditors = new ArrayList<EffectorEditor>();
    private JComboBox<String> effectorTypesComboBox = new JComboBox<String>();
    private Action addEffectorAction = new AbstractAction("Add") {
        @Override
        public void actionPerformed(ActionEvent event) {
            addEffector((String) effectorTypesComboBox.getSelectedItem());
        }
    };
    private JButton addEffectorButton = new JButton(addEffectorAction);

    public AgentEditor(Agent agent) {
        super(agent.getModel());
        this.agent = agent;
        filterSensorTypes();
        filterEffectorTypes();
    }

    private void filterSensorTypes() {
        sensorTypesComboBox.removeAllItems();
        for (String sensorName : sensorTypes.keySet()) {
            if (agent.getSensor(sensorTypes.get(sensorName)) == null) {
                sensorTypesComboBox.addItem(sensorName);
            }
        }
    }

    private void filterEffectorTypes() {
        effectorTypesComboBox.removeAllItems();
        for (String effectorName : effectorTypes.keySet()) {
            if (agent.getEffector(effectorTypes.get(effectorName)) == null) {
                effectorTypesComboBox.addItem(effectorName);
            }
        }
    }

    public Agent getAgent() {
        return agent;
    }

    public void addSensor(String sensorName) {
        agent.getEngine().enqueue(() -> {
            try {
                Class<? extends Sensor> sensorType = sensorTypes.get(sensorName);
                Sensor sensor = sensorType.getConstructor(Agent.class).newInstance(agent);
                SensorEditor editor = sensor.getEditor();
                sensorEditors.add(editor);
                layoutSensorEditor(editor);
                editor.readValues();
                filterSensorTypes();
            } catch (Exception e) {
                throw new RuntimeException("Unable to add sensor", e);
            }
        });
    }

    public void addEffector(String effectorName) {
        agent.getEngine().enqueue(() -> {
            try {
                Class<? extends Effector> effectorType = effectorTypes.get(effectorName);
                Effector effector = effectorType.getConstructor(Agent.class).newInstance(agent);
                EffectorEditor editor = effector.getEditor();
                effectorEditors.add(editor);
                layoutEffectorEditor(editor);
                editor.readValues();
                filterEffectorTypes();
            } catch (Exception e) {
                throw new RuntimeException("Unable to add effector", e);
            }
        });
    }

    public void deleteSensor(SensorEditor editor) {
        agent.getEngine().enqueue(() -> {
            sensorEditors.remove(editor);
            Sensor sensor = editor.getSensor();
            sensor.delete();
            sensorTab.remove(editor.getPanel());
            filterSensorTypes();
            sensorTab.repaint();
        });
    }

    public void deleteEffector(EffectorEditor editor) {
        agent.getEngine().enqueue(() -> {
            effectorEditors.remove(editor);
            Effector effector = editor.getEffector();
            effector.delete();
            effectorTab.remove(editor.getPanel());
            filterEffectorTypes();
            effectorTab.repaint();
        });
    }

    @Override
    public JComponent layoutFields() {
        JComponent modelLayout = super.layoutFields();

        sensorTab = new JPanel();
        sensorTab.setLayout(new MigLayout("", "[grow]", ""));
        getTabbedPane().addTab("Sensors", sensorTab);
        layoutSensors();

        effectorTab = new JPanel();
        effectorTab.setLayout(new MigLayout("", "[grow]", ""));
        getTabbedPane().addTab("Effectors", effectorTab);
        layoutEffectors();

        return modelLayout;
    }

    private void layoutSensors() {
        sensorTab.add(sensorTypesComboBox, "growx, split 2");
        sensorTab.add(addSensorButton, "wrap");
        for (SensorEditor editor : sensorEditors) {
            layoutSensorEditor(editor);
        }
    }

    private void layoutSensorEditor(SensorEditor editor) {
        sensorTab.add(editor.layoutFields(), "growx, wrap");
        Action deleteAction = new AbstractAction("Delete") {
            @Override
            public void actionPerformed(ActionEvent event) {
                deleteSensor(editor);
            }
        };
        JButton deleteButton = new JButton(deleteAction);
        editor.getPanel().add(deleteButton, "wrap");
    }

    private void layoutEffectors() {
        effectorTab.add(effectorTypesComboBox, "growx, split 2");
        effectorTab.add(addEffectorButton, "wrap");
        for (EffectorEditor editor : effectorEditors) {
            layoutEffectorEditor(editor);
        }
    }

    private void layoutEffectorEditor(EffectorEditor editor) {
        effectorTab.add(editor.layoutFields());
        Action deleteAction = new AbstractAction("Delete") {
            @Override
            public void actionPerformed(ActionEvent event) {
                deleteEffector(editor);
            }
        };
        JButton deleteButton = new JButton(deleteAction);
        editor.getPanel().add(deleteButton, "wrap");
    }

    @Override
    public void readValues() {
        super.readValues();
        for (Sensor sensor : agent.getSensors()) {
            SensorEditor sensorEditor = sensor.getEditor();
            sensorEditor.readValues();
            sensorEditors.add(sensorEditor);
        }
        for (Effector effector : agent.getEffectors()) {
            EffectorEditor effectorEditor = effector.getEditor();
            effectorEditor.readValues();
            effectorEditors.add(effectorEditor);
        }
    }

    @Override
    public void writeValues() {
        super.writeValues();
        for (Editor sensorEditor : sensorEditors) {
            sensorEditor.writeValues();
        }
        for (Editor effectorEditor : effectorEditors) {
            effectorEditor.writeValues();
        }
    }

    @Override
    public void close() {
        super.close();
        for (Editor sensorEditor : sensorEditors) {
            sensorEditor.close();
        }
        for (Editor effectorEditor : effectorEditors) {
            effectorEditor.close();
        }
    }
}
