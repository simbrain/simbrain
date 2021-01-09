package org.simbrain.world.imageworld.gui;

import org.simbrain.util.ResourceManager;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.SimbrainPreferences;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.component_actions.CloseAction;
import org.simbrain.workspace.component_actions.OpenAction;
import org.simbrain.workspace.component_actions.SaveAction;
import org.simbrain.workspace.component_actions.SaveAsAction;
import org.simbrain.workspace.gui.CouplingMenu;
import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.world.imageworld.ImageClipboard;
import org.simbrain.world.imageworld.ImageWorld;
import org.simbrain.world.imageworld.ImageWorldComponent;

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
        imageWorld.getImageAlbum().getEvents().onImageUpdate(() -> {
            repaint();
        });

        // Toolbars
        add(toolbars, BorderLayout.NORTH);
        toolbars.add(sourceToolbar);
        toolbars.add(sensorToolbar);
        var filterGui = new FilterCollectionGui(this, imageWorld.getFilterCollection());
        toolbars.add(filterGui.getToolBar());
        filterGui.getFilterComboBox().addActionListener(e -> {
            repaint();
        });

        setupToolbars();

        add(imageAlbumToolbar, BorderLayout.SOUTH);
        updateButtons();

        // Set up the file chooser
        fileChooser = new SFileChooser(SimbrainPreferences.getString("imagesDirectory"), "");
        // TODO: Below breaks the file chooser
        //fileChooser.setUseImagePreview(true);
        // String[] exts = ImageIO.getReaderFileSuffixes();
        // String[] descriptions = ImageIO.getReaderFormatNames();
        // for (int i = 0; i < exts.length; ++i) {
        //    fileChooser.addExtension(descriptions[i], "." + exts[i]);
        // }

        // Set up context menu
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                super.mouseClicked(evt);
                if (evt.isControlDown() || (evt.getButton() == MouseEvent.BUTTON3)) {
                    showContextMenu(evt);
                }
            }
        });

        // TODO. Only paint in paint "mode"
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
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    /**
     * Draw a pixel at the current point in the image panel.
     */
    private void drawPixel(MouseEvent evt) {
        var ratioX = 1.0 * getWidth() / imageWorld.getImageAlbum().getWidth();
        var ratioY = 1.0 * getHeight() / imageWorld.getImageAlbum().getHeight();
        var x = (int) (evt.getX() / ratioX);
        var y = (int) (evt.getY() / ratioY);
        imageWorld.getImageAlbum().getCurrentImage().setRGB(x, y, penColor.getRGB());
        imageWorld.getImageAlbum().fireImageUpdate();
    }

    /**
     * Central panel to render the image.
     */
    private class ImagePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(imageWorld.getFilterCollection().getCurrentFilter().getFilteredImage(),
                    0, 0, getWidth(), getHeight(), this);
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
        fileMenu.add(new OpenAction(this));
        fileMenu.add(new SaveAction(this));
        fileMenu.add(new SaveAsAction(this));
        fileMenu.addSeparator();
        fileMenu.add(new CloseAction(getWorkspaceComponent()));

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

        JButton saveImageButton = new JButton();
        saveImageButton.setIcon(ResourceManager.getSmallIcon("menu_icons/Save.png"));
        saveImageButton.setToolTipText("Save Image");
        saveImageButton.addActionListener(e -> {
            saveImage();
        });
        sourceToolbar.add(saveImageButton);

        JButton createCanvas = new JButton();
        createCanvas.setIcon(ResourceManager.getSmallIcon("menu_icons/PixelMatrix.png"));
        createCanvas.setToolTipText("Create canvas");

        createCanvas.addActionListener(e -> {
            imageWorld.createBlankCanvas(10, 10);
        });
        sourceToolbar.add(createCanvas);

        // Add Color Picker
        JButton setColorButton = new JButton();
        setColorButton.setIcon(ResourceManager.getSmallIcon("menu_icons/PaintView.png"));
        setColorButton.setToolTipText("Pen Color");
        setColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose Color", penColor);
            if (newColor != null) {
                this.penColor = newColor;
            }
        });
        sourceToolbar.add(setColorButton);

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
    public java.util.List<JButton> getImageAlbumButtons() {
        java.util.List<JButton> returnList = new LinkedList<>();

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
        if (imageWorld.getNumImages() < 2) {
            nextImagesButton.setEnabled(false);
            previousImagesButton.setEnabled(false);
        } else {
            nextImagesButton.setEnabled(true);
            previousImagesButton.setEnabled(true);
        }

    }

    @Override
    protected void closing() {
    }

}
