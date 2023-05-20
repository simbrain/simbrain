package org.simbrain.world.imageworld.gui;

import kotlinx.coroutines.Dispatchers;
import org.simbrain.util.ImageKt;
import org.simbrain.util.ResourceManager;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.gui.CouplingMenu;
import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.imageworld.ImageClipboard;
import org.simbrain.world.imageworld.ImageWorld;
import org.simbrain.world.imageworld.ImageWorldComponent;
import org.simbrain.world.imageworld.ImageWorldPreferences;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.LinkedList;

public class ImageWorldDesktopComponent extends DesktopComponent<ImageWorldComponent> {

    /**
     * Toolbars.
     */
    private JPanel toolbars = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private JToolBar sourceToolbar = new JToolBar();
    private JToolBar imageAlbumToolbar = new JToolBar();
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
     * Main model object.
     */
    private ImageWorld imageWorld;

    /**
     * If true, allow painting
     */
    public boolean paintMode = true;

    private transient ImageClipboard clipboard;

    /**
     * Current pen color when drawing on the current image.
     */
    private Color penColor = Color.white;

    /**
     * Button to advance to the next images.
     */
    private JButton nextImagesButton;

    /**
     * Button to go to the previous images.
     */
    private JButton previousImagesButton;

    private JButton takeSnapshotButton;

    private JLabel frameLabel = new JLabel();

    /**
     * Construct a new ImageDesktopComponent GUI.
     *
     * @param frame     The frame in which to place GUI elements.
     * @param component The ImageWorldComponent to interact with.
     */
    public ImageWorldDesktopComponent(GenericFrame frame, ImageWorldComponent component) {

        super(frame, component);
        imageWorld = component.getWorld();
        clipboard = new ImageClipboard(imageWorld);

        setupMenuBar(frame);
        setLayout(new BorderLayout());

        // Main image
        add(new ImagePanel(), BorderLayout.CENTER);
        imageWorld.getImageAlbum().getEvents().getImageUpdate().on(Dispatchers.getDefault(), true, () -> {
            updateToolbar();
            repaint();
        });
        imageWorld.getFilterCollection().getEvents().getFilterChanged().on((o, n) -> this.repaint());

        // Toolbars
        add(toolbars, BorderLayout.NORTH);
        toolbars.add(sourceToolbar);
        toolbars.add(sensorToolbar);
        var filterGui = new FilterCollectionGui(this, imageWorld.getFilterCollection());
        toolbars.add(filterGui.getToolBar());

        setupToolbars();

        add(imageAlbumToolbar, BorderLayout.SOUTH);
        updateToolbar();

        // Set up the file chooser
        fileChooser = new SFileChooser(ImageWorldPreferences.INSTANCE.getImageDirectory(), "");
        // TODO: Below breaks the file chooser
        //fileChooser.setUseImagePreview(true);
        // String[] exts = ImageIO.getReaderFileSuffixes();
        // String[] descriptions = ImageIO.getReaderFormatNames();
        // for (int i = 0; i < exts.length; ++i) {
        //    fileChooser.addExtension(descriptions[i], "." + exts[i]);
        // }

    }

    /**
     * Central panel to render the image.
     */
    private class ImagePanel extends JPanel {

        public ImagePanel() {
            super();

            // Ability to paint pixels black and white
            MouseAdapter mouseAdapter = new MouseAdapter() {

                @Override
                public void mouseDragged(MouseEvent evt) {
                    drawPixel(evt);
                }

                @Override
                public void mousePressed(MouseEvent evt) {
                    drawPixel(evt);
                }

                @Override
                public void mouseClicked(MouseEvent evt) {
                    super.mouseClicked(evt);
                    if (evt.isControlDown() || (evt.getButton() == MouseEvent.BUTTON3)) {
                        showContextMenu(evt);
                    }
                }
            };
            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            imageWorld.getFilterCollection().getCurrentFilter().updateFilter();
            g.drawImage(imageWorld.getFilterCollection().getCurrentFilter().getFilteredImage(),
                    0, 0, getWidth(), getHeight(), this);
        }

        /**
         * Draw a pixel at the current point in the image panel.
         */
        private void drawPixel(MouseEvent evt) {
            if(!paintMode || evt.isControlDown() || (evt.getButton() == MouseEvent.BUTTON3) ) {
                return;
            }
            var image = imageWorld.getImageAlbum().getCurrentImage();
            var ratioX = 1.0 * getWidth() / image.getWidth();
            var ratioY = 1.0 * getHeight() / image.getHeight();
            var x = (int) (evt.getX() / ratioX);
            var y = (int) (evt.getY() / ratioY);
            if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
                return;
            }
            if (evt.isShiftDown()) {
                image.setRGB(x, y, ImageKt.invert(penColor).getRGB());
            } else {
                image.setRGB(x, y, penColor.getRGB());
            }
            imageWorld.getImageAlbum().fireImageUpdate();
        }
    }

    private void setupMenuBar(GenericFrame frame) {

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File  ");
        menuBar.add(fileMenu);

        // Add load images menu item if it's an image album world
        JMenuItem loadImages = new JMenuItem("Load Images...");
        loadImages.addActionListener((e) -> {
            loadImages();
        });
        fileMenu.add(loadImages);

        JMenuItem saveImage = new JMenuItem("Save Image...");
        saveImage.addActionListener(e -> {
            saveImage();
        });

        fileMenu.add(saveImage);

        fileMenu.addSeparator();
        fileMenu.add(copyAction);
        fileMenu.add(pasteAction);

        fileMenu.addSeparator();
        fileMenu.add(SimbrainDesktop.INSTANCE.getActionManager().createImportAction(this));
        fileMenu.add(SimbrainDesktop.INSTANCE.getActionManager().createExportAction(this));
        fileMenu.addSeparator();
        fileMenu.add(SimbrainDesktop.INSTANCE.getActionManager().createCloseAction(this));

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
        CouplingMenu filterMenu = new CouplingMenu(getWorkspaceComponent(),
                imageWorld.getFilterCollection().getCurrentFilter());
        contextMenu.add(filterMenu);
        contextMenu.show(this, evt.getX(), evt.getY());
    }

    /**
     * Set up toolbars depending on what type of world is being displayed
     */
    private void setupToolbars() {

        getImageAlbumButtons().forEach(imageAlbumToolbar::add);


        JButton createCanvas = new JButton();
        createCanvas.setIcon(ResourceManager.getSmallIcon("menu_icons/PixelMatrix.png"));
        createCanvas.setToolTipText("New canvas...");
        createCanvas.addActionListener(e -> {
            JTextField wInp = new JTextField(5);
            JTextField hInp = new JTextField(5);
            wInp.setText("20");
            hInp.setText("20");
            JPanel myPanel = new JPanel();
            myPanel.add(new JLabel("Width:"));
            myPanel.add(wInp);
            myPanel.add(Box.createHorizontalStrut(15)); // a spacer
            myPanel.add(new JLabel("Height:"));
            myPanel.add(hInp);
            int result = JOptionPane.showConfirmDialog(null, myPanel, "Create new canvas, enter dimensions.", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                imageWorld.resetImageAlbum(Integer.parseInt(wInp.getText()), Integer.parseInt(hInp.getText()));
            }
        });
        sourceToolbar.add(createCanvas);

        //        // Add Color Picker
        //        JButton setColorButton = new JButton();
        //        setColorButton.setIcon(ResourceManager.getSmallIcon("menu_icons/PaintView.png"));
        //        setColorButton.setToolTipText("Pen Color");
        //        setColorButton.addActionListener(e -> {
        //            Color newColor = JColorChooser.showDialog(this, "Choose Color", penColor);
        //            if (newColor != null) {
        //                this.penColor = newColor;
        //            }
        //        });
        //        sourceToolbar.add(setColorButton);

        Color colorList[] = {Color.white, Color.black, Color.red, Color.blue, Color.green, Color.yellow, Color.cyan, Color.magenta};
        String colorNames[] = {"White", "Black", "Red", "Blue", "Green", "Yellow", "Cyan", "Magenta", "Custom"};
        JComboBox cbColorChoice = new JComboBox(colorNames);

        // Check box handling
        JCheckBox checkBoxDrawMode = new JCheckBox("Draw");
        checkBoxDrawMode.setSelected(paintMode);
        cbColorChoice.setEnabled(checkBoxDrawMode.isSelected());
        checkBoxDrawMode.addItemListener(e -> {
            paintMode = checkBoxDrawMode.isSelected();
            cbColorChoice.setEnabled(checkBoxDrawMode.isSelected());
        });

        cbColorChoice.addActionListener(e -> {
            int len = cbColorChoice.getItemCount();
            if(((JComboBox)e.getSource()).getSelectedIndex() == len -1) {
                System.out.println("Custom...");
            } else {
                this.penColor = colorList[cbColorChoice.getSelectedIndex()];
            }
        });

        sourceToolbar.add(checkBoxDrawMode);
        sourceToolbar.add(cbColorChoice);

    }

    /**
     * Copy image from current system clipboard.
     */
    Action copyAction = new AbstractAction("Copy") {
        @Override
        public void actionPerformed(ActionEvent e) {
            clipboard.copyImage();
        }
    };

    /**
     * Paste image from current system clipboard.
     */
    Action pasteAction = new AbstractAction("Paste") {
        @Override
        public void actionPerformed(ActionEvent e) {
            clipboard.pasteImage();
        }
    };

    /**
     * Save the current image.
     */
    private void saveImage() {
        fileChooser.setDescription("Save image");
        fileChooser.setUseImagePreview(true);
        File file = fileChooser.showSaveDialog(getWorkspaceComponent().getName() + ".png");
        //TODO
        // if (file != null) {
        //     try {
        //         pixelProducer.saveImage(file.toString());
        //     } catch (IOException ex) {
        //         JOptionPane.showMessageDialog(null, "Unable to save file: " + file.toString());
        //     }
        // }
    }

    /**
     * Toolbar buttons for image album.
     *
     * @return the list of buttons
     */
    public java.util.List<Component> getImageAlbumButtons() {
        java.util.List<Component> returnList = new LinkedList<>();

        var deleteCurrentImage = new JButton();
        deleteCurrentImage.setIcon(ResourceManager.getSmallIcon("menu_icons/RedX.png"));
        deleteCurrentImage.setToolTipText("Delete Current Image");
        deleteCurrentImage.addActionListener(e -> {
            imageWorld.getImageAlbum().deleteCurrentImage();
        });
        returnList.add(deleteCurrentImage);

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
            imageWorld.previousFrame();
        });
        returnList.add(previousImagesButton);

        nextImagesButton = new JButton();
        nextImagesButton.setIcon(ResourceManager.getSmallIcon("menu_icons/TangoIcons-GoNext.png"));
        nextImagesButton.setToolTipText("Next Image");
        nextImagesButton.addActionListener(e -> {
            imageWorld.nextFrame();
        });
        returnList.add(nextImagesButton);

        takeSnapshotButton = new JButton();
        takeSnapshotButton.setIcon(ResourceManager.getSmallIcon("menu_icons/camera.png"));
        takeSnapshotButton.setToolTipText("Take Snapshot");
        takeSnapshotButton.addActionListener(e -> {
            imageWorld.getImageAlbum().takeSnapshot();
        });
        returnList.add(takeSnapshotButton);


        returnList.add(frameLabel);


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
            imageWorld.loadImages(files);

            // Update status of buttons
            updateToolbar();

            // Save preferences
            ImageWorldPreferences.INSTANCE.setImageDirectory(fileChooser.getCurrentLocation());
        }
    }

    public void updateToolbar() {
        // Disable next / previous buttons when there is less than two images
        if (imageWorld.getNumImages() < 2) {
            nextImagesButton.setEnabled(false);
            previousImagesButton.setEnabled(false);
        } else {
            nextImagesButton.setEnabled(true);
            previousImagesButton.setEnabled(true);
        }
        var index = imageWorld.getImageAlbum().getFrameIndex();
        var numFrames = imageWorld.getImageAlbum().getNumFrames();
        var humanReadableFrameIndex = Math.min(index + 1, numFrames);
        frameLabel.setText(humanReadableFrameIndex + "/" + numFrames);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(650, 500);
    }
}
