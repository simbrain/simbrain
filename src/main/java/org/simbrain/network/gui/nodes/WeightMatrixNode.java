package org.simbrain.network.gui.nodes;

import org.piccolo2d.util.PPaintContext;
import org.simbrain.network.core.Connector;
import org.simbrain.network.events.ConnectorEvents;
import org.simbrain.network.gui.ImageBox;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.WeightMatrixArrow;
import org.simbrain.network.gui.actions.edit.CopyAction;
import org.simbrain.network.gui.actions.edit.CutAction;
import org.simbrain.network.gui.actions.edit.DeleteAction;
import org.simbrain.network.gui.actions.edit.PasteAction;
import org.simbrain.network.matrix.WeightMatrix;
import org.simbrain.network.matrix.ZoeConnector;
import org.simbrain.util.ImageKt;
import org.simbrain.util.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.table.BasicDataWrapperKt;
import org.simbrain.util.table.SimbrainDataViewer;
import org.simbrain.util.table.TableActionsKt;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import static org.simbrain.network.gui.NetworkPanelMenusKt.createCouplingMenu;

/**
 * A visual representation of a weight matrix
 */
public class WeightMatrixNode extends ScreenElement {

    // TODO: Make this cover other subclasses of Connector besides WeightMatrix.
    // But for now we are only using WeightMatrix

    /**
     * The weight matrix this node represents
     */
    private Connector weightMatrix;

    /**
     * A box around the {@link #imageBox}
     */
    private ImageBox imageBox;

    /**
     * Width of the {@link #imageBox}
     */
    private int imageWidth = 90;

    /**
     * Height of the {@link #imageBox}
     */
    private int imageHeight = 90;

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
    public WeightMatrixNode(NetworkPanel np, Connector wm) {
        super(np);
        networkPanel = np;

        this.weightMatrix = wm;

        WeightMatrixArrow arrow = new WeightMatrixArrow(this);
        addChild(arrow);

        imageBox = new ImageBox(imageWidth, imageHeight, 4);
        addChild(imageBox);
        renderMatrixToImage();
        setBounds(imageBox.getBounds());

        setPickable(true);

        ConnectorEvents events = weightMatrix.getEvents();
        events.onDeleted(w -> removeFromParent());
        events.onUpdated(this::renderMatrixToImage);
        wm.getSource().getEvents().onLocationChange(arrow::invalidateFullBounds);
        wm.getTarget().getEvents().onLocationChange(arrow::invalidateFullBounds);
        invalidateFullBounds();
    }

    /**
     * Render the weight matrix to the {@link #imageBox}.
     */
    private void renderMatrixToImage() {

        BufferedImage img = null;

        if (weightMatrix.isEnableRendering()) {

            if (weightMatrix instanceof ZoeConnector) {
                // TODO: Temp representation. If there is enough divergence can break into separate classes and update
                //  NetworkPanel.kt accordingly
                double[] tempArray = new double[100];
                Arrays.fill(tempArray, .1);
                img = ImageKt.toSimbrainColorImage(tempArray, 10, 10);
            } else {
                double[] pixelArray = ((WeightMatrix)weightMatrix).getWeights();
                img = ImageKt.toSimbrainColorImage(pixelArray, ((WeightMatrix)weightMatrix).getWeightMatrix().ncols(),
                        ((WeightMatrix)weightMatrix).getWeightMatrix().nrows());

            }

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
                // TODO: Clean up instanceof checks and casts
                if (weightMatrix instanceof WeightMatrix) {
                    ((WeightMatrix) weightMatrix).diagonalize();
                }
            }
        };
        contextMenu.add(diagAction);

        // Coupling menu
        contextMenu.addSeparator();
        JMenu couplingMenu = createCouplingMenu(networkPanel.getNetworkComponent(), weightMatrix);
        contextMenu.add(couplingMenu);

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
        dialog.addClosingTask(ape::commitChanges);

        // Weight matrix
        if (weightMatrix instanceof WeightMatrix) {
            var wm = BasicDataWrapperKt.createFromMatrix(((WeightMatrix) weightMatrix).getWeightMatrix());
            var wmViewer = new SimbrainDataViewer(wm, false);
            TableActionsKt.addSimpleDefaults(wmViewer);
            tabs.addTab("Weight Matrix", wmViewer);
            dialog.addClosingTask(() -> {
                ((WeightMatrix) weightMatrix).setWeights(wm.getRowMajorDoubleArray());
                weightMatrix.getEvents().fireUpdated();
            });
        }

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
    public Connector getModel() {
        return weightMatrix;
    }

}
