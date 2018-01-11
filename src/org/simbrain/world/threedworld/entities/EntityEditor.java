package org.simbrain.world.threedworld.entities;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.simbrain.world.threedworld.entities.EditorDialog.Editor;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public class EntityEditor implements Editor {
    private Entity entity;
    private JTabbedPane tabbedPane;
    private JPanel mainTab;
    private JTextField nameField = new JTextField(20);
    private JFormattedTextField xPositionField = new JFormattedTextField(EditorDialog.floatFormat);
    private JFormattedTextField yPositionField = new JFormattedTextField(EditorDialog.floatFormat);
    private JFormattedTextField zPositionField = new JFormattedTextField(EditorDialog.floatFormat);
    private JFormattedTextField yawRotationField = new JFormattedTextField(EditorDialog.floatFormat);
    private JFormattedTextField pitchRotationField = new JFormattedTextField(EditorDialog.floatFormat);
    private JFormattedTextField rollRotationField = new JFormattedTextField(EditorDialog.floatFormat);
    
    public EntityEditor(Entity entity) {
        this.entity = entity;
        xPositionField.setColumns(6);
        yPositionField.setColumns(6);
        zPositionField.setColumns(6);
        yawRotationField.setColumns(6);
        pitchRotationField.setColumns(6);
        rollRotationField.setColumns(6);
    }
    
    public Entity getEntity() {
        return entity;
    }
    
    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }
    
    public JPanel getMainTab() {
        return mainTab;
    }
    
    @Override
    public JComponent layoutFields() {
        tabbedPane = new JTabbedPane();
        
        mainTab = new JPanel();
        MigLayout layout = new MigLayout();
        mainTab.setLayout(layout);
        tabbedPane.addTab("Entity", mainTab);
        
        mainTab.add(new JLabel("Name"));
        mainTab.add(nameField, "growx, wrap");
        
        mainTab.add(new JLabel("Position"));
        mainTab.add(xPositionField, "split 3");
        mainTab.add(yPositionField);
        mainTab.add(zPositionField, "wrap");
        
        mainTab.add(new JLabel("Rotation"));
        mainTab.add(yawRotationField, "split 3");
        mainTab.add(pitchRotationField);
        mainTab.add(rollRotationField, "wrap");
        
        return tabbedPane;
    }
    
    @Override
    public void readValues() {
        nameField.setText(entity.getName());
        xPositionField.setValue(entity.getPosition().x);
        yPositionField.setValue(entity.getPosition().y);
        zPositionField.setValue(entity.getPosition().z);
        float[] angles = entity.getRotation().toAngles(null);
        yawRotationField.setValue(FastMath.RAD_TO_DEG * angles[1]);
        pitchRotationField.setValue(FastMath.RAD_TO_DEG * angles[0]);
        rollRotationField.setValue(FastMath.RAD_TO_DEG * angles[2]);
    }
    
    @Override
    public void writeValues() {
        entity.setName(nameField.getText());
        entity.queuePosition(new Vector3f(
                ((Number)xPositionField.getValue()).floatValue(),
                ((Number)yPositionField.getValue()).floatValue(),
                ((Number)zPositionField.getValue()).floatValue()));
        Quaternion rotation = new Quaternion();
        rotation.fromAngles(
                FastMath.DEG_TO_RAD * ((Number)pitchRotationField.getValue()).floatValue(),
                FastMath.DEG_TO_RAD * ((Number)yawRotationField.getValue()).floatValue(),
                FastMath.DEG_TO_RAD * ((Number)rollRotationField.getValue()).floatValue());
        entity.queueRotation(rotation);
    }
    
    @Override
    public void close() {}
}
