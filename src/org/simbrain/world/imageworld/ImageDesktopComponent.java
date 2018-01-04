package org.simbrain.world.imageworld;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.component_actions.CloseAction;
import org.simbrain.workspace.component_actions.OpenAction;
import org.simbrain.workspace.component_actions.SaveAction;
import org.simbrain.workspace.component_actions.SaveAsAction;
import org.simbrain.workspace.gui.GuiComponent;
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
    private ActionListener loadImageListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent evt) {
            SFileChooser fileChooser = new SFileChooser(
                    System.getProperty("user.home"),
                    "Select an image to load");
            fileChooser.setUseImagePreview(true);
            File file = fileChooser.showOpenDialog();
            if (file != null) {
                try {
                    component.getImageWorld().loadImage(file.toString());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Unabled to load file: " + file.toString());
                }
            }
        }
    };

    /**
     * Construct a new ImageDesktopComponent GUI.
     * @param frame The frame in which to place GUI elements.
     * @param imageWorldComponent The ImageWorldComponent to interact with.
     */
    public ImageDesktopComponent(GenericFrame frame,
            ImageWorldComponent imageWorldComponent) {
        super(frame, imageWorldComponent);
        component = imageWorldComponent;

        JMenuBar menuBar = new JMenuBar();
        this.setUpMenus(menuBar);
        frame.setJMenuBar(menuBar);

        JButton selectEmitterButton = new JButton();
        selectEmitterButton.setIcon(ResourceManager.getSmallIcon("light-bulb.png"));
        selectEmitterButton.setToolTipText("View Emitter Matrix");
        selectEmitterButton.addActionListener(evt -> {
            component.getImageWorld().selectEmitterMatrix();
        });
        sourceToolbar.add(selectEmitterButton);

        JButton resizeEmitterButton = new JButton();
        resizeEmitterButton.setIcon(ResourceManager.getSmallIcon("resize.png"));
        resizeEmitterButton.setToolTipText("Resize Emitter Matrix");
        resizeEmitterButton.addActionListener(evt -> {
            ResizeEmitterMatrixDialog dialog = new ResizeEmitterMatrixDialog(component.getImageWorld());
            dialog.setVisible(true);
        });
        sourceToolbar.add(resizeEmitterButton);

        JButton viewImageButton = new JButton();
        viewImageButton.setIcon(ResourceManager.getSmallIcon("photo.png"));
        viewImageButton.setToolTipText("View Static Image");
        viewImageButton.addActionListener(evt -> {
            component.getImageWorld().selectStaticSource();
        });
        sourceToolbar.add(viewImageButton);

        JButton loadImageButton = new JButton();
        loadImageButton.setIcon(ResourceManager.getSmallIcon("Open.png"));
        loadImageButton.setToolTipText("Load Static Image");
        loadImageButton.addActionListener(loadImageListener);
        sourceToolbar.add(loadImageButton);

        sensorToolbar.add(sensorMatrixCombo);
        sensorMatrixCombo.setToolTipText("Which Sensor Matrix to View");
        updateComboBox();
        sensorMatrixCombo.setSelectedIndex(0);
        sensorMatrixCombo.setMaximumSize(new java.awt.Dimension(200, 100));
        sensorMatrixCombo.addActionListener(evt -> {
            SensorMatrix selectedSensorMatrix = (SensorMatrix) sensorMatrixCombo.getSelectedItem();
            if (selectedSensorMatrix != null) {
                component.getImageWorld().setCurrentSensorMatrix(selectedSensorMatrix);
            }
        });

        JButton addSensorMatrix = new JButton(
                ResourceManager.getImageIcon("plus.png"));
        addSensorMatrix.setToolTipText("Add sensor matrix...");
        addSensorMatrix.addActionListener(evt -> {
            SensorMatrixDialog dialog = new SensorMatrixDialog(component.getImageWorld());
            dialog.setVisible(true);
        });
        sensorToolbar.add(addSensorMatrix);

        JButton deleteSensorMatrix = new JButton(
                ResourceManager.getImageIcon("minus.png"));
        deleteSensorMatrix.setToolTipText("Delete sensor matrix");
        deleteSensorMatrix.addActionListener(evt -> {
            SensorMatrix selectedSensorMatrix = (SensorMatrix) sensorMatrixCombo.getSelectedItem();
            component.getImageWorld().removeSensorMatrix(selectedSensorMatrix);
        });
        sensorToolbar.add(deleteSensorMatrix);

        // Lay out the whole component
        setLayout(new BorderLayout());
        add(toolbars, BorderLayout.NORTH);
        toolbars.add(sourceToolbar);
        toolbars.add(sensorToolbar);
        add(imageWorldComponent.getImageWorld().getImagePanel(), BorderLayout.CENTER);
        imageWorldComponent.getImageWorld().getImagePanel().setPreferredSize(new Dimension(640, 480));

        component.getImageWorld().addListener(this::updateComboBox);
    }

    /** Reset the combo box for the sensor panels. */
    private void updateComboBox() {
        sensorMatrixCombo.removeAllItems();
        SensorMatrix selectedSensorMatrix = component.getImageWorld().getCurrentSensorMatrix();
        for (SensorMatrix sensorMatrix : component.getImageWorld().getSensorMatrices()) {
            sensorMatrixCombo.addItem(sensorMatrix);
            if (sensorMatrix.equals(selectedSensorMatrix)) {
                sensorMatrixCombo.setSelectedItem(sensorMatrix);
            }
        }
    }

    @Override
    protected void closing() { }

    /**
     * Sets up menus.
     * @param menuBar the bar to add menus
     */
    public void setUpMenus(JMenuBar menuBar) {
        JMenu fileMenu = new JMenu("File  ");
        menuBar.add(fileMenu);

        JMenuItem loadImage = new JMenuItem("Load Image...");
        loadImage.addActionListener(loadImageListener);
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
