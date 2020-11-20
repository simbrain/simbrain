package org.simbrain.world.imageworld.gui;

import org.simbrain.util.ResourceManager;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.SimbrainPreferences;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.component_actions.CloseAction;
import org.simbrain.workspace.component_actions.OpenAction;
import org.simbrain.workspace.component_actions.SaveAction;
import org.simbrain.workspace.component_actions.SaveAsAction;
import org.simbrain.workspace.gui.CouplingMenu;
import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.world.imageworld.PixelProducer;
import org.simbrain.world.imageworld.ImageWorld;
import org.simbrain.world.imageworld.PixelConsumer;
import org.simbrain.world.imageworld.SensorMatrix;
import org.simbrain.world.imageworld.dialogs.ResizeEmitterMatrixDialog;
import org.simbrain.world.imageworld.dialogs.SensorMatrixDialog;
import org.simbrain.world.imageworld.filters.FilteredImageSource;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Contains the toolbars and actions used for the GUI representation
 * of {@link PixelProducer} and {@link PixelConsumer}.
 */
public class ImageWorldDesktopPanel extends JPanel {

    /**
     * Combo box for selecting which sensor matrix to view.
     */
    private JComboBox<SensorMatrix> sensorMatrixCombo = new JComboBox<SensorMatrix>();
    private JComboBox<Color> colorPicker = new JComboBox<Color>();
    /**
     * Toolbars.
     */
    private JPanel toolbars = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private JPanel bottom_toolbars = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    /**
     * Main toolbar with buttons to advance images, etc.
     */
    private JToolBar sourceToolbar = new JToolBar();
    private JToolBar imageAlbumToolbar = new JToolBar();

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
     * Parent world, either an {@link PixelProducer} or {@link PixelConsumer}.
     */
    private ImageWorld world;

    /**
     * Parent GUI Component
     */
    private GuiComponent guiComponent;

    /**
     * Button to advance to the next images.
     */
    private JButton nextImagesButton;

    /**
     * Button to go to the previous images.
     */
    private JButton previousImagesButton;

    /**
     * Construct a new ImageDesktopComponent GUI.
     *
     * @param frame     The frame in which to place GUI elements.
     * @param guiComponent The parent GUI Component
     * @param world     The underlying ImageWorld
     */
    public ImageWorldDesktopPanel(GenericFrame frame, GuiComponent guiComponent, ImageWorld world) {

        this.world = world;
        this.guiComponent = guiComponent;

        setupMenuBar(frame);
        setupToolbars();

        // Lay out the whole component
        setLayout(new BorderLayout());
        add(toolbars, BorderLayout.NORTH);
        toolbars.add(sourceToolbar);
        toolbars.add(sensorToolbar);
        add(world.getImagePanel(), BorderLayout.CENTER);
        world.getImagePanel().setPreferredSize(new Dimension(640, 480));
        add(bottom_toolbars, BorderLayout.SOUTH);
        bottom_toolbars.add(imageAlbumToolbar);


        world.addListener(new ImageWorld.Listener() {

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
        world.getImagePanel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                super.mouseClicked(evt);
                if (evt.isControlDown() || (evt.getButton() == MouseEvent.BUTTON3)) {
                    showContextMenu(evt);
                }
            }
        });

        // Set up the file chooser
        fileChooser = new SFileChooser(SimbrainPreferences.getString("imagesDirectory"), "");
        fileChooser.setUseImagePreview(true);
        // String[] exts = ImageIO.getReaderFileSuffixes();
        // String[] descriptions = ImageIO.getReaderFormatNames();
        // for (int i = 0; i < exts.length; ++i) {
        //    fileChooser.addExtension(descriptions[i], "." + exts[i]);
        // }

        // Update status of buttons for image album worlds
        if (world instanceof PixelProducer) {
            updateButtons();
        }

    }

    private void setupMenuBar(GenericFrame frame) {

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File  ");
        menuBar.add(fileMenu);

        // Add load images menu item if it's an image album world
        if (world instanceof PixelProducer) {
            JMenuItem loadImages = new JMenuItem("Load Images...");
            loadImages.addActionListener(e -> {
                loadImages();
            });
            fileMenu.add(loadImages);
        }

        JMenuItem saveImage = new JMenuItem("Save Image...");
        saveImage.addActionListener(e -> {
            saveImage();
        });

        fileMenu.add(saveImage);

        fileMenu.addSeparator();
        fileMenu.add(copyAction);
        fileMenu.add(pasteAction);

        fileMenu.addSeparator();
        fileMenu.add(new OpenAction(guiComponent));
        fileMenu.add(new SaveAction(guiComponent));
        fileMenu.add(new SaveAsAction(guiComponent));
        fileMenu.addSeparator();
        fileMenu.add(new CloseAction(guiComponent.getWorkspaceComponent()));

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem helpItem = new JMenuItem("World Help");
        menuBar.add(helpMenu);
        //TODO: Conditional depending on type
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
        CouplingMenu sensorMatrixMenu = new CouplingMenu(guiComponent.getWorkspaceComponent(), world.getCurrentSensorMatrix());
        contextMenu.add(sensorMatrixMenu);
        contextMenu.show(world.getImagePanel(), evt.getX(), evt.getY());
    }

    /**
     * Set up toolbars depending on what type of world is being displayed
     */
    private void setupToolbars() {

//        if (world instanceof PixelProducer) {
//            getImageAlbumButtons().forEach(sourceToolbar::add);
//        } else if (world instanceof PixelConsumer) {
//            getPixelDisplayToolbar().forEach(sourceToolbar::add);
//        }

        if (world instanceof PixelProducer) {
            getImageAlbumButtons().forEach(imageAlbumToolbar::add);
        } else if (world instanceof PixelConsumer) {
            getPixelDisplayToolbar().forEach(imageAlbumToolbar::add);
        }

        JButton saveImageButton = new JButton();
        saveImageButton.setIcon(ResourceManager.getSmallIcon("menu_icons/Save.png"));
        saveImageButton.setToolTipText("Save Image");
        saveImageButton.addActionListener(e -> {
            saveImage();
        });
        sourceToolbar.add(saveImageButton);

        if (world instanceof PixelProducer) {
            JButton createCanvas = new JButton();
            createCanvas.setIcon(ResourceManager.getSmallIcon("menu_icons/PixelMatrix.png"));
            createCanvas.setToolTipText("Create canvas");

            createCanvas.addActionListener(e -> {
                // StandardDialog dialog = new StandardDialog();
                // JPanel pane = new JPanel();
                // JTextField rows = new JTextField();
                // JTextField columns = new JTextField();
                // rows.setText("40");
                // rows.setColumns(3);
                // columns.setText("10");
                // columns.setColumns(3);
                // pane.add(new JLabel("Rows"));
                // pane.add(rows);
                // pane.add(new JLabel("Columns"));
                // pane.add(columns);
                //
                // dialog.setContentPane(pane);
                // dialog.pack();
                // dialog.setLocationRelativeTo(null);
                // dialog.setVisible(true);
                // if (!dialog.hasUserCancelled()) {
                //     System.out.println("here");
                //     ((ImageAlbumWorld)world).createBlankCanvas(Integer.parseInt(rows.getText()),Integer.parseInt(columns.getText()));
                // }
                ((PixelProducer)world).createBlankCanvas(10,10);
            });
            sourceToolbar.add(createCanvas);
        }

        // Add Color Picker
        JButton setColorButton = new JButton();
        setColorButton.setIcon(ResourceManager.getSmallIcon("menu_icons/PaintView.png"));
        setColorButton.setToolTipText("Pen Color");
        setColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose Color",
                    ((PixelProducer)world).getPenColor());
            ((PixelProducer)world).setPenColor(newColor);
        });
        sourceToolbar.add(setColorButton);

//        // Add Color Picker
//        JButton setColorButton2 = new JButton();
//        setColorButton2.setIcon(ResourceManager.getSmallIcon("menu_icons/PaintView.png"));
//        setColorButton2.setToolTipText("Pen Color");
//        setColorButton2.addActionListener(e -> {
//            Color newColor2 = JColorChooser.showDialog(this, "Choose Color", ((PixelProducer)world).getPenColor());
//            ((PixelProducer)world).setPenColor(newColor2);
//        });
//        sourceToolbar.add(setColorButton2);

        sensorToolbar.add(new JLabel("Filters:"));
        sensorToolbar.add(sensorMatrixCombo);
        sensorMatrixCombo.setToolTipText("Which Sensor Matrix to View");
        updateComboBox();
        sensorMatrixCombo.setSelectedItem(world.getCurrentSensorMatrix());
        sensorMatrixCombo.setMaximumSize(new Dimension(200, 100));
        sensorMatrixCombo.addActionListener(evt -> {
            SensorMatrix selectedSensorMatrix = (SensorMatrix) sensorMatrixCombo.getSelectedItem();
            if (selectedSensorMatrix != null) {
                world.setCurrentSensorMatrix(selectedSensorMatrix);
            }
        });

        // Add Sensor Matrix
        JButton addSensorMatrix = new JButton(ResourceManager.getImageIcon("menu_icons/plus.png"));
        addSensorMatrix.setToolTipText("Add Sensor Matrix");
        addSensorMatrix.addActionListener(evt -> {
            SensorMatrixDialog dialog = new SensorMatrixDialog(world);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        });
        sensorToolbar.add(addSensorMatrix);

        // Delete sensor matrix
//        JButton deleteSensorMatrix = new JButton(ResourceManager.getImageIcon("menu_icons/minus.png"));
//        deleteSensorMatrix.setToolTipText("Delete Sensor Matrix");
//        deleteSensorMatrix.addActionListener(evt -> {
//            SensorMatrix selectedSensorMatrix = (SensorMatrix) sensorMatrixCombo.getSelectedItem();
//            world.removeSensorMatrix(selectedSensorMatrix);
//        });
//        sensorToolbar.add(deleteSensorMatrix);

        // Editor Sensor Matrix
        JButton editSensorMatrix = new JButton(ResourceManager.getImageIcon("menu_icons/Prefs.png"));
        editSensorMatrix.setToolTipText("Edit Sensor Matrix");
        editSensorMatrix.addActionListener(evt -> {

            // Create a dialog to edit to sensor matrix and filtered image source, if any
            StandardDialog filterEditorDialog = new StandardDialog();
            JPanel dialogPanel = new JPanel();
            dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
            filterEditorDialog.setContentPane(dialogPanel);

            // Edit the top level sensor matrix, basically just a name
            SensorMatrix sensorMatrix = world.getCurrentSensorMatrix();
            AnnotatedPropertyEditor sensorEditor = new AnnotatedPropertyEditor(sensorMatrix);
            dialogPanel.add(sensorEditor);
            filterEditorDialog.addClosingTask(() -> {
                sensorEditor.commitChanges();
            });

            // If the sensor matrix has a filtered image source, edit it too
            if (sensorMatrix.getSource() instanceof FilteredImageSource) {
                FilteredImageSource imageSource = (FilteredImageSource) sensorMatrix.getSource();
                AnnotatedPropertyEditor filterEditor = new AnnotatedPropertyEditor(imageSource);
                dialogPanel.add(filterEditor);
                filterEditorDialog.addClosingTask(() -> {
                    filterEditor.commitChanges();
                    // Update the image based on the image source
                    world.getImageSource().notifyResize();
                    world.getImageSource().notifyImageUpdate();
                    sensorMatrixCombo.updateUI();
                });
            }


            // Delete sensor matrix
//            JButton deleteSensorMatrix = new JButton(ResourceManager.getImageIcon("menu_icons/minus.png"));
            JButton deleteSensorMatrix = new JButton("Delete Filter");

            deleteSensorMatrix.setToolTipText("Delete Sensor Matrix");
            deleteSensorMatrix.addActionListener(evt2 -> {
                SensorMatrix selectedSensorMatrix = (SensorMatrix) sensorMatrixCombo.getSelectedItem();
                world.removeSensorMatrix(selectedSensorMatrix);
            });
            dialogPanel.add(deleteSensorMatrix);

            filterEditorDialog.pack();
            filterEditorDialog.setLocationRelativeTo(this);
            filterEditorDialog.setVisible(true);

        });
        sensorToolbar.add(editSensorMatrix);

    }


    /**
     * Copy image from current system clipboard.
     */
    Action copyAction = new AbstractAction("Copy") {
        @Override
        public void actionPerformed(ActionEvent e) {
            world.getClipboard().copyImage();
        }
    };

    /**
     * Paste image from current system clipboard.
     */
    Action pasteAction = new AbstractAction("Paste") {
        @Override
        public void actionPerformed(ActionEvent e) {
            world.getClipboard().pasteImage();
        }
    };

    /**
     * Save the current image.
     */
    private void saveImage() {
        fileChooser.setDescription("Save image");
        fileChooser.setUseImagePreview(true);
        File file = fileChooser.showSaveDialog(guiComponent.getWorkspaceComponent().getName() + ".png");
        if (file != null) {
            try {
                world.saveImage(file.toString());
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
        SensorMatrix selectedSensorMatrix = world.getCurrentSensorMatrix();
        for (SensorMatrix sensorMatrix : world.getSensorMatrices()) {
            sensorMatrixCombo.addItem(sensorMatrix);
            if (sensorMatrix.equals(selectedSensorMatrix)) {
                sensorMatrixCombo.setSelectedItem(sensorMatrix);
            }
        }
    }

    /**
     * Toolbar buttons for image album world.
     *
     * @return the list of buttons
     */
    public List<JButton> getImageAlbumButtons() {
        List<JButton> returnList = new LinkedList<>();

        JButton loadImagesButton = new JButton();
        loadImagesButton.setIcon(ResourceManager.getSmallIcon("menu_icons/photo.png"));
        loadImagesButton.setToolTipText("Load Images");
        loadImagesButton.addActionListener(e -> {
            loadImages();
        });
        returnList.add(loadImagesButton);

        previousImagesButton = new JButton();
        previousImagesButton.setIcon(ResourceManager.getSmallIcon("menu_icons/TangoIcons-GoPrevious.png"));
        previousImagesButton.setToolTipText("Previous Image");
        previousImagesButton.addActionListener(e -> {
            ((PixelProducer)world).previousFrame();
        });
        returnList.add(previousImagesButton);

        nextImagesButton = new JButton();
        nextImagesButton.setIcon(ResourceManager.getSmallIcon("menu_icons/TangoIcons-GoNext.png"));
        nextImagesButton.setToolTipText("Next Image");
        nextImagesButton.addActionListener(e -> {
            ((PixelProducer)world).nextFrame();
        });
        returnList.add(nextImagesButton);

//        JButton loadImagesButton = new JButton();
//        loadImagesButton.setIcon(ResourceManager.getSmallIcon("menu_icons/photo.png"));
//        loadImagesButton.setToolTipText("Load Images");
//        loadImagesButton.addActionListener(e -> {
//            loadImages();
//        });
//        returnList.add(loadImagesButton);

        return returnList;
    }

    /**
     * Toolbar buttons for pixel display world.
     *
     * @return the list of buttons
     */
    public List<JButton> getPixelDisplayToolbar() {
        List<JButton> returnList = new LinkedList<>();
        JButton editEmitterButton = new JButton();
        editEmitterButton.setIcon(ResourceManager.getSmallIcon("menu_icons/resize.png"));
        editEmitterButton.setToolTipText("Edit Emitter Matrix");
        editEmitterButton.addActionListener(evt -> {
            ResizeEmitterMatrixDialog dialog = new ResizeEmitterMatrixDialog((PixelConsumer) world);
            dialog.setVisible(true);
        });
        returnList.add(editEmitterButton);
        return returnList;
    }


    /**
     * Load a set of images to be used as the "Album" in an image album.
     */
    private void loadImages() {
        fileChooser.setDescription("Select images to load");
        File[] files = fileChooser.showMultiOpenDialogNative();
        if (files != null) {

            // Load the images
            ((PixelProducer) world).loadImages(files);

            // Update status of buttons
            updateButtons();

            // Save preferences
            SimbrainPreferences.putString("imagesDirectory", fileChooser.getCurrentLocation());
        }
    }

    /**
     * Update whether buttons are enabled or not based on the status of the
     * image world.
     */
    public void updateButtons() {
        // Disable next / previous buttons when there is less than two images
        if (((PixelProducer) world).getNumImages() < 2) {
            nextImagesButton.setEnabled(false);
            previousImagesButton.setEnabled(false);
        } else {
            nextImagesButton.setEnabled(true);
            previousImagesButton.setEnabled(true);

        }

    }
}
