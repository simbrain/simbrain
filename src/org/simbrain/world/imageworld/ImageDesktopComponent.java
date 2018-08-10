package org.simbrain.world.imageworld;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.SimbrainPreferences;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.component_actions.CloseAction;
import org.simbrain.workspace.component_actions.OpenAction;
import org.simbrain.workspace.component_actions.SaveAction;
import org.simbrain.workspace.component_actions.SaveAsAction;
import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.workspace.gui.MultiCouplingMenu;
import org.simbrain.world.imageworld.dialogs.ResizeEmitterMatrixDialog;
import org.simbrain.world.imageworld.dialogs.SensorMatrixDialog;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

/**
 * GUI Interface for vision world.
 *
 * @author Tim Shea, Jeff Yoshimi
 */
public class ImageDesktopComponent extends GuiComponent<ImageWorldComponent> {

    /**
     * Combo box for selecting which sensor matrix to view.
     */
    private JComboBox<SensorMatrix> sensorMatrixCombo = new JComboBox<SensorMatrix>();

    /**
     * The image world component .
     */
    private ImageWorldComponent component;

    private JPanel toolbars = new JPanel(new FlowLayout(FlowLayout.LEFT));
    
    private JToolBar sourceToolbar = new JToolBar();
    
    private JToolBar sensorToolbar = new JToolBar();

    SFileChooser fileChooser;
    
    private JPopupMenu contextMenu;
    
    private MultiCouplingMenu multiCouplingMenu;

    /**
     * Construct a new ImageDesktopComponent GUI.
     *
     * @param frame     The frame in which to place GUI elements.
     * @param component The ImageWorldComponent to interact with.
     */
    public ImageDesktopComponent(GenericFrame frame, ImageWorldComponent component) {
        super(frame, component);
        this.component = component;

        setupMenuBar(frame);
        setupToolbars();

        // Lay out the whole component
        setLayout(new BorderLayout());
        add(toolbars, BorderLayout.NORTH);
        toolbars.add(sourceToolbar);
        toolbars.add(sensorToolbar);
        add(component.getWorld().getImagePanel(), BorderLayout.CENTER);
        component.getWorld().getImagePanel().setPreferredSize(new Dimension(640, 480));

        this.component.getWorld().addListener(new ImageWorld.Listener() {
            
            @Override
            public void imageSourceChanged(ImageSource changedSource) {
            }

            @Override
            public void sensorMatrixAdded(SensorMatrix addedMatrix) {
                updateComboBox();
            }

            @Override
            public void sensorMatrixRemoved(SensorMatrix removedMatrix) {
                updateComboBox();
            }
        });

        setupContextMenu(component);
        setupFileChooser();
    }

    private void setupMenuBar(GenericFrame frame) {

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File  ");
        menuBar.add(fileMenu);

        if (component.getWorld().getSourceType() == ImageWorld.SourceType.STATIC_SOURCE) {
            JMenuItem loadImage = new JMenuItem("Load Image...");
            loadImage.addActionListener(this::loadImage);
            fileMenu.add(loadImage);
        }
        JMenuItem saveImage = new JMenuItem("Save Image...");
        saveImage.addActionListener(this::saveImage);
        fileMenu.add(saveImage);

        fileMenu.addSeparator();
        fileMenu.add(copyAction);
        fileMenu.add(pasteAction);

        fileMenu.addSeparator();
        fileMenu.add(new OpenAction(this));
        fileMenu.add(new SaveAction(this));
        fileMenu.add(new SaveAsAction(this));
        fileMenu.addSeparator();
        fileMenu.add(new CloseAction(this.getWorkspaceComponent()));

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem helpItem = new JMenuItem("World Help");
        menuBar.add(helpMenu);
        ShowHelpAction helpAction = new ShowHelpAction("Pages/Worlds/ImageWorld/ImageWorld.html");
        helpItem.setAction(helpAction);
        helpMenu.add(helpItem);

        frame.setJMenuBar(menuBar);
    }

    private void setupContextMenu(ImageWorldComponent component) {
        contextMenu = new JPopupMenu();
        contextMenu.add(copyAction);
        contextMenu.add(pasteAction);
        contextMenu.addSeparator();
        multiCouplingMenu = new MultiCouplingMenu(component.getWorkspace(), contextMenu, 5);
        component.getWorld().getImagePanel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                super.mouseClicked(evt);
                if (evt.isControlDown() || (evt.getButton() == MouseEvent.BUTTON3)) {
                    showContextMenu(evt);
                }
            }
        });
    }

    private void showContextMenu(MouseEvent evt) {
        multiCouplingMenu.setSourceModels(component.getSelectedModels());
        contextMenu.show(component.getWorld().getImagePanel(), evt.getX(), evt.getY());
    }

    @Override
    protected void closing() {
    }

    private void setupToolbars() {
        if (component.getWorld().getSourceType() == ImageWorld.SourceType.EMITTER_SOURCE) {
            JButton editEmitterButton = new JButton();
            editEmitterButton.setIcon(ResourceManager.getSmallIcon("resize.png"));
            editEmitterButton.setToolTipText("Edit Emitter Matrix");
            editEmitterButton.addActionListener(evt -> {
                ResizeEmitterMatrixDialog dialog = new ResizeEmitterMatrixDialog(component.getWorld());
                dialog.setVisible(true);
            });
            sourceToolbar.add(editEmitterButton);
        }

        if (component.getWorld().getSourceType() == ImageWorld.SourceType.STATIC_SOURCE) {
            JButton loadImageButton = new JButton();
            loadImageButton.setIcon(ResourceManager.getSmallIcon("photo.png"));
            loadImageButton.setToolTipText("Load Image");
            loadImageButton.addActionListener(this::loadImage);
            sourceToolbar.add(loadImageButton);
        }

        JButton saveImageButton = new JButton();
        saveImageButton.setIcon(ResourceManager.getSmallIcon("Save.png"));
        saveImageButton.setToolTipText("Save Image");
        saveImageButton.addActionListener(this::saveImage);
        sourceToolbar.add(saveImageButton);

        JButton clearImageButton = new JButton();
        clearImageButton.setIcon(ResourceManager.getSmallIcon("Eraser.png"));
        clearImageButton.setToolTipText("Clear Image");
        clearImageButton.addActionListener(evt -> component.getWorld().clearImage());
        // sourceToolbar.add(clearImageButton); // Too destructive.  Don't include on toolbar

        sensorToolbar.add(new JLabel("Sensors:"));
        sensorToolbar.add(sensorMatrixCombo);
        sensorMatrixCombo.setToolTipText("Which Sensor Matrix to View");
        updateComboBox();
        sensorMatrixCombo.setSelectedIndex(0);
        sensorMatrixCombo.setMaximumSize(new java.awt.Dimension(200, 100));
        sensorMatrixCombo.addActionListener(evt -> {
            SensorMatrix selectedSensorMatrix = (SensorMatrix) sensorMatrixCombo.getSelectedItem();
            if (selectedSensorMatrix != null) {
                component.getWorld().setCurrentSensorMatrix(selectedSensorMatrix);
            }
        });

        JButton addSensorMatrix = new JButton(ResourceManager.getImageIcon("plus.png"));
        addSensorMatrix.setToolTipText("Add Sensor Matrix");
        addSensorMatrix.addActionListener(evt -> {
            SensorMatrixDialog dialog = new SensorMatrixDialog(component.getWorld());
            dialog.setVisible(true);
        });
        sensorToolbar.add(addSensorMatrix);

        JButton deleteSensorMatrix = new JButton(ResourceManager.getImageIcon("minus.png"));
        deleteSensorMatrix.setToolTipText("Delete Sensor Matrix");
        deleteSensorMatrix.addActionListener(evt -> {
            SensorMatrix selectedSensorMatrix = (SensorMatrix) sensorMatrixCombo.getSelectedItem();
            component.getWorld().removeSensorMatrix(selectedSensorMatrix);
        });
        sensorToolbar.add(deleteSensorMatrix);
    }

    Action copyAction = new AbstractAction("Copy") {
        @Override
        public void actionPerformed(ActionEvent e) {
            component.getWorld().getClipboard().copyImage();
        }
    };

    Action pasteAction = new AbstractAction("Paste") {
        @Override
        public void actionPerformed(ActionEvent e) {
            component.getWorld().getClipboard().pasteImage();
        }
    };

    private void setupFileChooser() {
        fileChooser = new SFileChooser(SimbrainPreferences.getString("imagesDirectory"), "");
        fileChooser.setUseImagePreview(true);
        String[] exts = ImageIO.getReaderFileSuffixes();
        //String[] descriptions = ImageIO.getReaderFormatNames();
        //for (int i = 0; i < exts.length; ++i) {
        //    fileChooser.addExtension(descriptions[i], "." + exts[i]);
        //}
    }

    private void loadImage(ActionEvent evt) {
        fileChooser.setDescription("Select an image to load");
        File file = fileChooser.showOpenDialog();
        if (file != null) {
            try {
                component.getWorld().loadImage(file.toString());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Unable to load file: " + file.toString());
            }
        }
    }

    private void saveImage(ActionEvent evt) {
        fileChooser.setDescription("Select filename to save");
        fileChooser.setUseImagePreview(true);
        File file = fileChooser.showSaveDialog();
        if (file != null) {
            try {
                component.getWorld().saveImage(file.toString());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Unable to save file: " + file.toString());
            }
        }
    }

    /**
     * Reset the combo box for the sensor panels.
     */
    private void updateComboBox() {
        sensorMatrixCombo.removeAllItems();
        SensorMatrix selectedSensorMatrix = component.getWorld().getCurrentSensorMatrix();
        for (SensorMatrix sensorMatrix : component.getWorld().getSensorMatrices()) {
            sensorMatrixCombo.addItem(sensorMatrix);
            if (sensorMatrix.equals(selectedSensorMatrix)) {
                sensorMatrixCombo.setSelectedItem(sensorMatrix);
            }
        }
    }
}
