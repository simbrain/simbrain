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
import org.simbrain.world.imageworld.dialogs.SensorMatrixDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * GUI Interface for vision world.
 *
 * @author Tim Shea, Jeff Yoshimi
 */
public abstract class ImageDesktopComponent<IWC extends ImageWorldComponent> extends GuiComponent<IWC> {

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
     * Coupling menu.
     */
    private MultiCouplingMenu multiCouplingMenu;

    /**
     * Construct a new ImageDesktopComponent GUI.
     *
     * @param frame     The frame in which to place GUI elements.
     * @param component The ImageWorldComponent to interact with.
     */
    ImageDesktopComponent(GenericFrame frame, IWC component) {
        super(frame, component);
        setComponent(component);

        setupMenuBar(frame);
        setupToolbars();

        // Lay out the whole component
        setLayout(new BorderLayout());
        add(toolbars, BorderLayout.NORTH);
        toolbars.add(sourceToolbar);
        toolbars.add(sensorToolbar);
        add(getComponent().getWorld().getImagePanel(), BorderLayout.CENTER);
        getComponent().getWorld().getImagePanel().setPreferredSize(new Dimension(640, 480));

        this.getComponent().getWorld().addListener(new ImageWorld.Listener() {

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

        // Add all additional file menu items to file menu
        getAdditionalFileMenuItems().forEach(fileMenu::add);

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
     * Get a list of additional menu items from sub-class to be added to the file menu.
     * See {@link ImageDesktopComponent#setupMenuBar(GenericFrame)}.
     *
     * @return a list of menu items
     */
    public abstract List<JMenuItem> getAdditionalFileMenuItems();

    private void setupContextMenu(ImageWorldComponent component) {
        contextMenu = new JPopupMenu();
        contextMenu.add(copyAction);
        contextMenu.add(pasteAction);
        contextMenu.addSeparator();
        multiCouplingMenu = new MultiCouplingMenu(getComponent().getWorkspace(), contextMenu, 100);
        getComponent().getWorld().getImagePanel().addMouseListener(new MouseAdapter() {
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
        multiCouplingMenu.setSourceModels(getComponent().getAttributeContainers());
        contextMenu.show(getComponent().getWorld().getImagePanel(), evt.getX(), evt.getY());
    }

    @Override
    protected void closing() {
    }

    private void setupToolbars() {

        // Add additional buttons to the source toolbar
        getAdditionalSourceToolbarButtons().forEach(sourceToolbar::add);

        JButton saveImageButton = new JButton();
        saveImageButton.setIcon(ResourceManager.getSmallIcon("Save.png"));
        saveImageButton.setToolTipText("Save Image");
        saveImageButton.addActionListener(this::saveImage);
        sourceToolbar.add(saveImageButton);

        JButton clearImageButton = new JButton();
        clearImageButton.setIcon(ResourceManager.getSmallIcon("Eraser.png"));
        clearImageButton.setToolTipText("Clear Image");
        clearImageButton.addActionListener(evt -> getComponent().getWorld().clearImage());
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
                getComponent().getWorld().setCurrentSensorMatrix(selectedSensorMatrix);
            }
        });

        JButton addSensorMatrix = new JButton(ResourceManager.getImageIcon("plus.png"));
        addSensorMatrix.setToolTipText("Add Sensor Matrix");
        addSensorMatrix.addActionListener(evt -> {
            SensorMatrixDialog dialog = new SensorMatrixDialog(getComponent().getWorld());
            dialog.setVisible(true);
        });
        sensorToolbar.add(addSensorMatrix);

        JButton deleteSensorMatrix = new JButton(ResourceManager.getImageIcon("minus.png"));
        deleteSensorMatrix.setToolTipText("Delete Sensor Matrix");
        deleteSensorMatrix.addActionListener(evt -> {
            SensorMatrix selectedSensorMatrix = (SensorMatrix) sensorMatrixCombo.getSelectedItem();
            getComponent().getWorld().removeSensorMatrix(selectedSensorMatrix);
        });
        sensorToolbar.add(deleteSensorMatrix);
    }

    /**
     * Get a list of additional button from sub-class to be added to the sensorToolbar.
     *
     * See {@link ImageDesktopComponent#setupToolbars()}
     *
     * @return the list of buttons to be added
     */
    public abstract List<JButton> getAdditionalSourceToolbarButtons();

    Action copyAction = new AbstractAction("Copy") {
        @Override
        public void actionPerformed(ActionEvent e) {
            getComponent().getWorld().getClipboard().copyImage();
        }
    };

    Action pasteAction = new AbstractAction("Paste") {
        @Override
        public void actionPerformed(ActionEvent e) {
            getComponent().getWorld().getClipboard().pasteImage();
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
                getComponent().getWorld().saveImage(file.toString());
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
        SensorMatrix selectedSensorMatrix = getComponent().getWorld().getCurrentSensorMatrix();
        for (SensorMatrix sensorMatrix : getComponent().getWorld().getSensorMatrices()) {
            sensorMatrixCombo.addItem(sensorMatrix);
            if (sensorMatrix.equals(selectedSensorMatrix)) {
                sensorMatrixCombo.setSelectedItem(sensorMatrix);
            }
        }
    }

    /**
     * Get the image world component this desktop component holds.
     *
     * @return the image world component
     */
    public abstract IWC getComponent();


    public abstract void setComponent(IWC component);
}
