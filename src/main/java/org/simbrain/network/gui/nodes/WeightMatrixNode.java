package org.simbrain.network.gui.nodes;

import org.nd4j.linalg.factory.Nd4j;
import org.piccolo2d.util.PPaintContext;
import org.simbrain.network.dl4j.WeightMatrix;
import org.simbrain.network.events.WeightMatrixEvents;
import org.simbrain.network.gui.ImageBox;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.WeightMatrixArrow;
import org.simbrain.network.gui.actions.edit.CopyAction;
import org.simbrain.network.gui.actions.edit.CutAction;
import org.simbrain.network.gui.actions.edit.DeleteAction;
import org.simbrain.network.gui.actions.edit.PasteAction;
import org.simbrain.util.ImageKt;
import org.simbrain.util.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.table.NumericTable;
import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.util.table.SimbrainJTableScrollPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A visual representation of a weight matrix
 */
public class WeightMatrixNode extends ScreenElement implements PropertyChangeListener {

    /**
     * The weight matrix this node represents
     */
    private WeightMatrix weightMatrix;

    /**
     * A box around the {@link #imageBox}
     */
    private ImageBox imageBox;

    /**
     * Width of the {@link #imageBox}
     */
    private int imageWidth = 100;

    /**
     * Height of the {@link #imageBox}
     */
    private int imageHeight = 100;

    /**
     * Parent network panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * Construct the weight matrix node.
     *
     * @param np parent panel
     * @param wm the weight matrix being represented
     */
    public WeightMatrixNode(NetworkPanel np, WeightMatrix wm) {
        super(np);
        networkPanel = np;

        this.weightMatrix = wm;

        WeightMatrixArrow arrow = new WeightMatrixArrow(this);
        addChild(arrow);

        imageBox = new ImageBox(imageWidth, imageHeight, 4);
        addChild(imageBox);
        renderMatrixToImage();
        setBounds(imageBox.getBounds());
        addPropertyChangeListener(PROPERTY_FULL_BOUNDS, this);

        setPickable(true);

        WeightMatrixEvents events = weightMatrix.getEvents();
        events.onDelete(w -> removeFromParent());
        events.onUpdated(this::renderMatrixToImage);
        wm.getSource().getEvents().onLocationChange(arrow::invalidateFullBounds);
        wm.getTarget().getEvents().onLocationChange(arrow::invalidateFullBounds);
    }

    @Override
    public void propertyChange(PropertyChangeEvent arg0) {
        setBounds(imageBox.getFullBounds());
    }

    /**
     * Render the weight matrix to the {@link #imageBox}.
     */
    private void renderMatrixToImage() {

        BufferedImage img = null;

        if (weightMatrix.isEnableRendering()) {
            float[] activations = Nd4j.toFlattened(weightMatrix.getWeightMatrix()).toFloatVector();
            img = ImageKt.toSimbrainColorImage(activations, weightMatrix.getWeightMatrix().columns(),
                    weightMatrix.getWeightMatrix().rows());
        }

        imageBox.setImage(img);
    }

    @Override
    protected void paint(PPaintContext paintContext) {
        paintContext.getGraphics().setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        super.paint(paintContext);
    }

    public ImageBox getImageBox() {
        return imageBox;
    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public boolean isDraggable() {
        return false;
    }

    @Override
    public String getToolTipText() {
        return weightMatrix.toString();
    }

    @Override
    public JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();

        contextMenu.add(new CutAction(getNetworkPanel()));
        contextMenu.add(new CopyAction(getNetworkPanel()));
        contextMenu.add(new PasteAction(getNetworkPanel()));
        contextMenu.addSeparator();

        // Edit Submenu
        Action editArray = new AbstractAction("Edit...") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                StandardDialog dialog = getMatrixDialog();
                dialog.setVisible(true);
            }
        };
        contextMenu.add(editArray);
        contextMenu.add(new DeleteAction(getNetworkPanel()));

        contextMenu.addSeparator();
        Action randomizeAction = new AbstractAction("Randomize") {
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Rand.png"));
                putValue(SHORT_DESCRIPTION, "Randomize neuron array");
            }

            @Override
            public void actionPerformed(final ActionEvent event) {
                weightMatrix.randomize();
            }
        };
        contextMenu.add(randomizeAction);

        Action clearAction = new AbstractAction("Clear") {
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Eraser.png"));
                putValue(SHORT_DESCRIPTION, "Clear neuron array");
            }
            @Override
            public void actionPerformed(final ActionEvent event) {
                weightMatrix.clear();
            }
        };
        contextMenu.add(clearAction);

        Action diagAction = new AbstractAction("Diagonalize") {
            {
                //putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/"));
                putValue(SHORT_DESCRIPTION, "Diagonalize array");
            }
            @Override
            public void actionPerformed(final ActionEvent event) {
                weightMatrix.diagonalize();
            }
        };
        contextMenu.add(diagAction);

        contextMenu.addSeparator();
        Action editComponents = new AbstractAction("Edit Components...") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                StandardDialog dialog = new StandardDialog();
                NumericTable table = new NumericTable(weightMatrix.getWeightMatrix().toDoubleMatrix());
                SimbrainJTable st = SimbrainJTable.createTable(table);
                dialog.setContentPane(new SimbrainJTableScrollPanel(st));

                dialog.addClosingTask(() -> {
                    weightMatrix.setWeights(table.getFlattenedData());
                });
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);

            }
        };
        contextMenu.add(editComponents);

        // Coupling menu
        contextMenu.addSeparator();
        JMenu couplingMenu = networkPanel.getCouplingMenu(weightMatrix);
        if (couplingMenu != null) {
            contextMenu.add(couplingMenu);
        }

        return contextMenu;

    }

    /**
     * Returns the dialog for editing this weight matrix
     */
    private StandardDialog getMatrixDialog() {
        StandardDialog dialog = new StandardDialog();
        dialog.setTitle("Edit Weight Matrix");
        JTabbedPane tabs= new JTabbedPane();

        // Property Editor
        AnnotatedPropertyEditor ape = new AnnotatedPropertyEditor(weightMatrix);
        tabs.addTab("Properties", ape);

        // Weight matrix
        tabs.addTab("Weight Matrix", new JLabel("Coming soon"));
        // TODO

        dialog.setContentPane(tabs);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        return dialog;
    }

    @Override
    public JDialog getPropertyDialog() {
        return getMatrixDialog();
    }

    @Override
    public WeightMatrix getModel() {
        return weightMatrix;
    }

}
