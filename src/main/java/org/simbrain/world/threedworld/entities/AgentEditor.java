package org.simbrain.world.threedworld.entities;

import net.miginfocom.swing.MigLayout;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.world.threedworld.entities.EditorDialog.Editor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentEditor extends ModelEditor {
    private static Map<String, Class<? extends Sensor>> sensorTypes = new HashMap<>();
    private static Map<String, Class<? extends Effector>> effectorTypes = new HashMap<>();

    static {
        sensorTypes.put("Vision", VisionSensor.class);
        sensorTypes.put("Collision", CollisionSensor.class);
        effectorTypes.put("Walking", WalkingEffector.class);
    }

    private Agent agent;
    private JPanel sensorTab;
    private JComboBox<String> sensorTypesComboBox = new JComboBox<>();
    private Action addSensorAction = new AbstractAction("Add") {
        @Override
        public void actionPerformed(ActionEvent event) {
            addSensor((String) sensorTypesComboBox.getSelectedItem());
        }
    };
    private JButton addSensorButton = new JButton(addSensorAction);
    private JPanel effectorTab;
    private List<AnnotatedPropertyEditor> sensorEditors = new ArrayList<>();
    private List<EffectorEditor> effectorEditors = new ArrayList<>();
    private JComboBox<String> effectorTypesComboBox = new JComboBox<>();
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
        Class<? extends Sensor> sensorType = sensorTypes.get(sensorName);
        agent.getEngine().enqueue(() -> {
            try {
                sensorType.getConstructor(Agent.class).newInstance(agent);
            } catch (Exception e) {
                throw new RuntimeException("Unable to add sensor", e);
            }
        }, true);
        agent.getSensor(sensorType).ifPresent(this::addSensorEditor);
    }

    private void addSensorEditor(Sensor sensor) {
        AnnotatedPropertyEditor editor = new AnnotatedPropertyEditor(sensor);
        Action deleteAction = new AbstractAction("Delete") {
            @Override
            public void actionPerformed(ActionEvent event) {
                deleteSensor(sensor, editor);
            }
        };
        JButton deleteButton = new JButton(deleteAction);
        editor.add(deleteButton);
        sensorEditors.add(editor);
        sensorTab.add(editor, "growx, wrap");
        filterSensorTypes();
        sensorTab.repaint();
    }

    public void addEffector(String effectorName) {
        agent.getEngine().enqueue(() -> {
            try {
                Class<? extends Effector> effectorType = effectorTypes.get(effectorName);
                Effector effector = effectorType.getConstructor(Agent.class).newInstance(agent);
                EffectorEditor editor = effector.getEditor();
                effectorEditors.add(editor);
                addEffectorEditor(editor);
                editor.readValues();
                filterEffectorTypes();
            } catch (Exception e) {
                throw new RuntimeException("Unable to add effector", e);
            }
        });
    }

    public void deleteSensor(Sensor sensor, AnnotatedPropertyEditor editor) {
        agent.getEngine().enqueue(sensor::delete, true);
        sensorEditors.remove(editor);
        sensorTab.remove(editor);
        filterSensorTypes();
        sensorTab.repaint();
    }

    public void deleteEffector(EffectorEditor editor) {
        Effector effector = editor.getEffector();
        agent.getEngine().enqueue(effector::delete, true);
        effectorEditors.remove(editor);
        effectorTab.remove(editor.getPanel());
        filterEffectorTypes();
        effectorTab.repaint();
    }

    @Override
    public JComponent layoutFields() {
        JComponent modelLayout = super.layoutFields();

        sensorTab = new JPanel();
        sensorTab.setLayout(new MigLayout("", "[grow]", ""));
        getTabbedPane().addTab("Sensors", sensorTab);
        sensorTab.add(sensorTypesComboBox, "growx, split 2");
        sensorTab.add(addSensorButton, "wrap");
        for (AnnotatedPropertyEditor editor : sensorEditors) {
            sensorTab.add(editor, "growx, wrap");
        }

        effectorTab = new JPanel();
        effectorTab.setLayout(new MigLayout("", "[grow]", ""));
        getTabbedPane().addTab("Effectors", effectorTab);
        effectorTab.add(effectorTypesComboBox, "growx, split 2");
        effectorTab.add(addEffectorButton, "wrap");
        for (EffectorEditor editor : effectorEditors) {
            addEffectorEditor(editor);
        }

        return modelLayout;
    }

    private void addEffectorEditor(EffectorEditor editor) {
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
            addSensorEditor(sensor);
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
        for (AnnotatedPropertyEditor sensorEditor : sensorEditors) {
            sensorEditor.commitChanges();
        }
        for (Editor effectorEditor : effectorEditors) {
            effectorEditor.writeValues();
        }
    }

    @Override
    public void close() {
        super.close();
        for (Editor effectorEditor : effectorEditors) {
            effectorEditor.close();
        }
    }
}
