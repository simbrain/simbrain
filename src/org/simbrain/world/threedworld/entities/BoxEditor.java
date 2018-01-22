package org.simbrain.world.threedworld.entities;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jme3.math.Vector3f;

import net.miginfocom.swing.MigLayout;

class BoxEditor extends EntityEditor {
    private BoxEntity box;
    private JFormattedTextField sizeXField = new JFormattedTextField(EditorDialog.floatFormat);
    private JFormattedTextField sizeYField = new JFormattedTextField(EditorDialog.floatFormat);
    private JFormattedTextField sizeZField = new JFormattedTextField(EditorDialog.floatFormat);
    private JFormattedTextField massField = new JFormattedTextField(EditorDialog.floatFormat);
    private JTextField materialField = new JTextField(20);
    
    {
        sizeXField.setColumns(6);
        sizeYField.setColumns(6);
        sizeZField.setColumns(6);
        massField.setColumns(6);
    }
    
    public BoxEditor(BoxEntity box) {
        super(box);
        this.box = box;
    }
    
    @Override
    public JComponent layoutFields() {
        JComponent entityLayout = super.layoutFields();
        
        JPanel boxTab = new JPanel();
        boxTab.setLayout(new MigLayout());
        getTabbedPane().addTab("Box", boxTab);
        
        boxTab.add(new JLabel("Size"));
        boxTab.add(sizeXField, "split 3");
        boxTab.add(sizeYField);
        boxTab.add(sizeZField, "wrap");
        
        boxTab.add(new JLabel("Mass"));
        boxTab.add(massField, "wrap");
        
        boxTab.add(new JLabel("Material"));
        boxTab.add(materialField, "wrap");
        
        return entityLayout;
    }
    
    @Override
    public void readValues() {
        super.readValues();
        sizeXField.setValue(box.getSize().x);
        sizeYField.setValue(box.getSize().y);
        sizeZField.setValue(box.getSize().z);
        massField.setValue(box.getMass());
        materialField.setText(box.getMaterial());
    }
    
    @Override
    public void writeValues() {
        super.writeValues();
        box.getEngine().enqueue(() -> {
            box.setSize(new Vector3f(
                    ((Number)sizeXField.getValue()).floatValue(),
                    ((Number)sizeYField.getValue()).floatValue(),
                    ((Number)sizeZField.getValue()).floatValue()));
            box.setMass(((Number)massField.getValue()).floatValue());
            box.setMaterial(materialField.getText());
        });
    }
}