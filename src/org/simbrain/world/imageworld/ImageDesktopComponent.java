package org.simbrain.world.imageworld;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

import javax.swing.*;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.component_actions.CloseAction;
import org.simbrain.workspace.component_actions.OpenAction;
import org.simbrain.workspace.component_actions.SaveAction;
import org.simbrain.workspace.component_actions.SaveAsAction;
import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.workspace.gui.MultiCouplingMenu;
import org.simbrain.world.imageworld.dialogs.SensorMatrixDialog;
import org.simbrain.world.imageworld.dialogs.ResizeEmitterMatrixDialog;

/**
 * GUI Interface for vision world.
 * @author Tim Shea, Jeff Yoshimi
 */
public class ImageDesktopComponent extends GuiComponent<ImageWorldComponent> {
    private static final long serialVersionUID = 9019927108869839191L;

    /** Combo box for selecting which sensor matrix to view. */
    private JComboBox<SensorMatrix> sensorMatrixCombo = new JComboBox<SensorMatrix>();

    /** The image world component . */
    private ImageWorldComponent component;

    private JPanel toolbars = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private JToolBar sourceToolbar = new JToolBar();
    private JToolBar sensorToolbar = new JToolBar();

    private JPopupMenu contextMenu;
    private MultiCouplingMenu multiCouplingMenu;

    /**
     * Construct a new ImageDesktopComponent GUI.
     * @param frame The frame in which to place GUI elements.
     * @param imageWorldComponent The ImageWorldComponent to interact with.
     */
    public ImageDesktopComponent(GenericFrame frame, ImageWorldComponent imageWorldComponent) {
        super(frame, imageWorldComponent);
        component = imageWorldComponent;

        JMenuBar menuBar = new JMenuBar();
        this.setUpMenus(menuBar);
        frame.setJMenuBar(menuBar);

        JButton selectEmitterButton = new JButton();
        selectEmitterButton.setIcon(ResourceManager.getSmallIcon("light-bulb.png"));
        selectEmitterButton.setToolTipText("View Emitter Matrix");
        selectEmitterButton.addActionListener(evt -> {
            component.getWorld().selectEmitterMatrix();
        });
        sourceToolbar.add(selectEmitterButton);

        JButton resizeEmitterButton = new JButton();
        resizeEmitterButton.setIcon(ResourceManager.getSmallIcon("resize.png"));
        resizeEmitterButton.setToolTipText("Resize Emitter Matrix");
        resizeEmitterButton.addActionListener(evt -> {
            ResizeEmitterMatrixDialog dialog = new ResizeEmitterMatrixDialog(component.getWorld());
            dialog.setVisible(true);
        });
        sourceToolbar.add(resizeEmitterButton);

        JButton viewImageButton = new JButton();
        viewImageButton.setIcon(ResourceManager.getSmallIcon("photo.png"));
        viewImageButton.setToolTipText("View Static Image");
        viewImageButton.addActionListener(evt -> {
            component.getWorld().selectStaticSource();
        });
        sourceToolbar.add(viewImageButton);

        JButton loadImageButton = new JButton();
        loadImageButton.setIcon(ResourceManager.getSmallIcon("Open.png"));
        loadImageButton.setToolTipText("Load Static Image");
        loadImageButton.addActionListener(this::loadImage);
        sourceToolbar.add(loadImageButton);

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

        JButton addSensorMatrix = new JButton(
                ResourceManager.getImageIcon("plus.png"));
        addSensorMatrix.setToolTipText("Add sensor matrix...");
        addSensorMatrix.addActionListener(evt -> {
            SensorMatrixDialog dialog = new SensorMatrixDialog(component.getWorld());
            dialog.setVisible(true);
        });
        sensorToolbar.add(addSensorMatrix);

        JButton deleteSensorMatrix = new JButton(
                ResourceManager.getImageIcon("minus.png"));
        deleteSensorMatrix.setToolTipText("Delete sensor matrix");
        deleteSensorMatrix.addActionListener(evt -> {
            SensorMatrix selectedSensorMatrix = (SensorMatrix) sensorMatrixCombo.getSelectedItem();
            component.getWorld().removeSensorMatrix(selectedSensorMatrix);
        });
        sensorToolbar.add(deleteSensorMatrix);

        // Lay out the whole component
        setLayout(new BorderLayout());
        add(toolbars, BorderLayout.NORTH);
        toolbars.add(sourceToolbar);
        toolbars.add(sensorToolbar);
        add(imageWorldComponent.getWorld().getImagePanel(), BorderLayout.CENTER);
        imageWorldComponent.getWorld().getImagePanel().setPreferredSize(new Dimension(640, 480));

        component.getWorld().addListener(new ImageWorld.Listener() {
            @Override
            public void imageSourceChanged(ImageSource changedSource) {}

            @Override
            public void sensorMatrixAdded(SensorMatrix addedMatrix) {
                updateComboBox();
            }

            @Override
            public void sensorMatrixRemoved(SensorMatrix removedMatrix) {
                updateComboBox();
            }
        });

        contextMenu = new JPopupMenu();
        multiCouplingMenu = new MultiCouplingMenu(component.getWorkspace(), contextMenu, 5);
        imageWorldComponent.getWorld().getImagePanel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                super.mouseClicked(evt);
                if (evt.isControlDown() || (evt.getButton() == MouseEvent.BUTTON3)) {
                    showContextMenu(evt);
                }
            }
        });
    }

    /** Reset the combo box for the sensor panels. */
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

    private void loadImage(ActionEvent evt) {
        SFileChooser fileChooser = new SFileChooser(System.getProperty("user.home"), "Select an image to load");
        fileChooser.setUseImagePreview(true);
        File file = fileChooser.showOpenDialog();
        if (file != null) {
            try {
                component.getWorld().loadImage(file.toString());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Unable to load file: " + file.toString());
            }
        }
    }

    private void showContextMenu(MouseEvent evt) {
        multiCouplingMenu.setSourceModels(component.getSelectedModels());
        contextMenu.show(component.getWorld().getImagePanel(), evt.getX(), evt.getY());
    }

    @Override
    protected void closing() {}

    /**
     * Sets up menus.
     * @param menuBar the bar to add menus
     */
    public void setUpMenus(JMenuBar menuBar) {
        JMenu fileMenu = new JMenu("File  ");
        menuBar.add(fileMenu);

        JMenuItem loadImage = new JMenuItem("Load Image...");
        loadImage.addActionListener(this::loadImage);
        fileMenu.add(loadImage);

        fileMenu.addSeparator();
        // TODO: Serialization not hooked up yet
        fileMenu.add(new OpenAction(this));
        fileMenu.add(new SaveAction(this));
        fileMenu.add(new SaveAsAction(this));
        fileMenu.addSeparator();
        fileMenu.add(new CloseAction(this.getWorkspaceComponent()));

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem helpItem = new JMenuItem("World Help");
        menuBar.add(helpMenu);
        ShowHelpAction helpAction = new ShowHelpAction(
                "Pages/Worlds/ImageWorld/ImageWorld.html"); // TODO: Create docs
        helpItem.setAction(helpAction);
        helpMenu.add(helpItem);
    }
}
