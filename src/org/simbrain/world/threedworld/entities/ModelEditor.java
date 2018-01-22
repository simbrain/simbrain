package org.simbrain.world.threedworld.entities;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.miginfocom.swing.MigLayout;

/**
 * ModelEditor wraps the properties of a ModelEntity in an Editor form.
 */
public class ModelEditor extends EntityEditor {
    private ModelEntity model;
    private JComboBox<String> fileNameCombo = new JComboBox<String>();
    private JFileChooser fileChooser = new JFileChooser();
    private AbstractAction browseAction = new AbstractAction("Browse") {
        @Override
        public void actionPerformed(ActionEvent event) {
            int result = fileChooser.showOpenDialog(getTabbedPane());
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                File assetDirectory = new File(model.getEngine().getAssetDirectory());
                String relativePath = assetDirectory.toURI().relativize(file.toURI()).toString();
                fileNameCombo.setSelectedItem(relativePath);
                if (!fileNameCombo.getSelectedItem().equals(relativePath)) {
                    fileNameCombo.addItem(relativePath);
                    fileNameCombo.setSelectedItem(relativePath);
                }
            }
        }
    };
    private JButton browseButton = new JButton(browseAction);

    /**
     * Construct a new ModelEditor from the given model.
     * @param model The model from which to read and write values.
     */
    public ModelEditor(ModelEntity model) {
        super(model);
        this.model = model;
        fileChooser.setFileFilter(new FileNameExtensionFilter("jME3 Geometry", "j3o"));
        fileChooser.setCurrentDirectory(new File(model.getEngine().getAssetDirectory()));
    }

    @Override
    public JComponent layoutFields() {
        JComponent entityLayout = super.layoutFields();

        JPanel modelTab = new JPanel();
        modelTab.setLayout(new MigLayout());
        getTabbedPane().addTab("Model", modelTab);

        modelTab.add(new JLabel("File Name"));
        modelTab.add(fileNameCombo, "split");
        modelTab.add(browseButton, "wrap");

        return entityLayout;
    }

    @Override
    public void readValues() {
        super.readValues();
        File modelDirectory = new File(model.getEngine().getAssetDirectory() + "/Models");
        String[] models = modelDirectory.list((File dir, String name) -> {
            return name.endsWith(".j3o");
        });
        boolean selected = false;
        for (String file : models) {
            String modelFileName = "Models/" + file;
            fileNameCombo.addItem(modelFileName);
            if (modelFileName.equals(model.getFileName())) {
                fileNameCombo.setSelectedItem(modelFileName);
                selected = true;
            }
        }
        if (!selected) {
            fileNameCombo.addItem(model.getFileName());
            fileNameCombo.setSelectedItem(model.getFileName());
        }
    }

    @Override
    public void writeValues() {
        super.writeValues();
        String fileName = fileNameCombo.getSelectedItem().toString();
        if (!fileName.equals(model.getFileName())) {
            model.queueReload(fileName);
        }
    }
}