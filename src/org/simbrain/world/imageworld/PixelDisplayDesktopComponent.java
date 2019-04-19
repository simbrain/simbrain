package org.simbrain.world.imageworld;

import org.simbrain.util.SFileChooser;
import org.simbrain.util.SimbrainPreferences;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.component_actions.CloseAction;
import org.simbrain.workspace.component_actions.OpenAction;
import org.simbrain.workspace.component_actions.SaveAction;
import org.simbrain.workspace.component_actions.SaveAsAction;
import org.simbrain.workspace.gui.CouplingMenu;
import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.world.imageworld.dialogs.ResizeEmitterMatrixDialog;
import org.simbrain.world.imageworld.dialogs.SensorMatrixDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class PixelDisplayDesktopComponent extends GuiComponent<PixelDisplayComponent> {

    /**
     * The image world component .
     */
    private PixelDisplayComponent component;

    /**
     * Combo box for selecting which sensor matrix to view.
     */
    private JComboBox<SensorMatrix> sensorMatrixCombo = new JComboBox<SensorMatrix>();

    /**
     * Toolbars.
     */
    private JPanel toolbars = new JPanel(new FlowLayout(FlowLayout.LEFT));

    /**
     * Main toolbar with buttons to advance images, etc.
     */
    private JToolBar sourceToolbar = new JToolBar();

    /**
     * Toolbar for setting the sensor matrix.
     */
    private JToolBar sensorToolbar = new JToolBar();

    /**
     * Custom file chooser for selecting image files.
     */
    protected SFileChooser fileChooser;

    /**
     * Context menu.
     */
    private JPopupMenu contextMenu;

    /**
     * Construct a new ImageDesktopComponent GUI.
     *
     * @param frame     The frame in which to place GUI elements.
     * @param component The ImageWorldComponent to interact with.
     */
    public PixelDisplayDesktopComponent(GenericFrame frame, PixelDisplayComponent component) {
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

        component.getWorld().addListener(new ImageWorld.Listener() {

            @Override
            public void sensorMatrixAdded(SensorMatrix addedMatrix) {
                updateComboBox();
            }

            @Override
            public void sensorMatrixRemoved(SensorMatrix removedMatrix) {
                updateComboBox();
            }
        });

        // Set up context menu
        component.getWorld().getImagePanel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                super.mouseClicked(evt);
                if (evt.isControlDown() || (evt.getButton() == MouseEvent.BUTTON3)) {
                    showContextMenu(evt);
                }
            }
        });

        setupFileChooser();
    }


    private void setupMenuBar(GenericFrame frame) {

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File  ");
        menuBar.add(fileMenu);

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


    /**
     * Create and display the context menu.
     */
    private void showContextMenu(MouseEvent evt) {
        contextMenu = new JPopupMenu();
        contextMenu.add(copyAction);
        contextMenu.add(pasteAction);
        contextMenu.addSeparator();
        CouplingMenu sensorMatrixMenu = new CouplingMenu(getWorkspaceComponent(), component.getWorld().getCurrentSensorMatrix());
        contextMenu.add(sensorMatrixMenu);
        contextMenu.show(component.getWorld().getImagePanel(), evt.getX(), evt.getY());
    }

    private void setupToolbars() {

        // Add additional buttons to the source toolbar
        getAdditionalSourceToolbarButtons().forEach(sourceToolbar::add);

        JButton saveImageButton = new JButton();
        saveImageButton.setIcon(org.simbrain.resource.ResourceManager.getSmallIcon("Save.png"));
        saveImageButton.setToolTipText("Save Image");
        saveImageButton.addActionListener(this::saveImage);
        sourceToolbar.add(saveImageButton);

        JButton clearImageButton = new JButton();
        clearImageButton.setIcon(org.simbrain.resource.ResourceManager.getSmallIcon("Eraser.png"));
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

        JButton addSensorMatrix = new JButton(org.simbrain.resource.ResourceManager.getImageIcon("plus.png"));
        addSensorMatrix.setToolTipText("Add Sensor Matrix");
        addSensorMatrix.addActionListener(evt -> {
            SensorMatrixDialog dialog = new SensorMatrixDialog(component.getWorld());
            dialog.setVisible(true);
        });
        sensorToolbar.add(addSensorMatrix);

        JButton deleteSensorMatrix = new JButton(org.simbrain.resource.ResourceManager.getImageIcon("minus.png"));
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
        // String[] exts = ImageIO.getReaderFileSuffixes();
        // String[] descriptions = ImageIO.getReaderFormatNames();
        // for (int i = 0; i < exts.length; ++i) {
        //    fileChooser.addExtension(descriptions[i], "." + exts[i]);
        // }
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


    @Override
    protected void closing() {

    }

    public List<JButton> getAdditionalSourceToolbarButtons() {
        List<JButton> returnList = new LinkedList<>();
        JButton editEmitterButton = new JButton();
        editEmitterButton.setIcon(org.simbrain.resource.ResourceManager.getSmallIcon("resize.png"));
        editEmitterButton.setToolTipText("Edit Emitter Matrix");
        editEmitterButton.addActionListener(evt -> {
            ResizeEmitterMatrixDialog dialog = new ResizeEmitterMatrixDialog(component.getWorld());
            dialog.setVisible(true);
        });
        returnList.add(editEmitterButton);
        return returnList;
    }


}
