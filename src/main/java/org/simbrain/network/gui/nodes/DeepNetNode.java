package org.simbrain.network.gui.nodes;

import org.jetbrains.annotations.Nullable;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.nodes.PText;
import org.piccolo2d.util.PBounds;
import org.simbrain.network.events.LocationEvents;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.actions.edit.CopyAction;
import org.simbrain.network.gui.actions.edit.CutAction;
import org.simbrain.network.gui.actions.edit.DeleteAction;
import org.simbrain.network.gui.actions.edit.PasteAction;
import org.simbrain.network.gui.dialogs.DeepNetDialogsKt;
import org.simbrain.network.kotlindl.DeepNet;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;

import static org.simbrain.util.GeomKt.minus;

/**
 * GUI representation of KotlinDL deep network.
 */
public class DeepNetNode extends ScreenElement {

    /**
     * The deep network being represented.
     */
    private DeepNet deepNet;

    /**
     * Width in pixels of the main display box.
     */
    private final float initialWidth = 80;

    /**
     * Height in pixels of the main display box.
     */
    private final float initialHeight = 120;

    /**
     * Text showing info about the array.
     */
    private PText infoText;

    /**
     * Font for info text.
     */
    public static final Font INFO_FONT = new Font("Arial", Font.PLAIN, 8);

    private PPath box = PPath.createRectangle(0, 0, initialWidth, initialHeight);

    public DeepNetNode(NetworkPanel networkPanel, DeepNet dn) {
        super(networkPanel);
        this.deepNet = dn;

        box.setPickable(true);
        addChild(box);

        LocationEvents events = dn.getEvents();
        events.onDeleted(n -> removeFromParent());
        events.onUpdated(() -> {
            updateInfoText();
            // System.out.println(deepNet.getOutputs());
        });

        // Border box determines bounds
        PBounds bounds = box.getBounds();
        setBounds(bounds);

        // Info text
        infoText = new PText();
        infoText.setFont(INFO_FONT);
        addChild(infoText);
        updateInfoText();

        deepNet.getEvents().onLocationChange(this::pullViewPositionFromModel);
        pullViewPositionFromModel();
    }

    /**
     * Update status text.
     */
    private void updateInfoText() {
        infoText.setText("Output: (" +
                Utils.doubleArrayToString(deepNet.getOutputs().col(0), 2) + ")" +
                "\n\nInput: (" + Utils.doubleArrayToString(deepNet.doubleInputs(), 2) + ")");
        var newBounds = infoText.getBounds().getBounds();
        newBounds.grow(10,10); // add a margin
        box.setBounds(newBounds);
        setBounds(box.getBounds());
        deepNet.setWidth(box.getWidth());
        deepNet.setHeight(box.getHeight());
    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    @Override
    public JPopupMenu getContextMenu() {

        JPopupMenu contextMenu = new JPopupMenu();

        contextMenu.add(new CutAction(getNetworkPanel()));
        contextMenu.add(new CopyAction(getNetworkPanel()));
        contextMenu.add(new PasteAction(getNetworkPanel()));
        contextMenu.addSeparator();

        // Edit Submenu
        Action editNet = new AbstractAction("Edit...") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                StandardDialog dialog = (StandardDialog) getPropertyDialog();
                dialog.setLocationRelativeTo(null);
                dialog.pack();
                dialog.setVisible(true);
            }
        };
        contextMenu.add(editNet);
        contextMenu.add(new DeleteAction(getNetworkPanel()));
        contextMenu.addSeparator();

        // Coupling menu
        //contextMenu.addSeparator();
        //JMenu couplingMenu = networkPanel.getCouplingMenu(neuronArray);
        //if (couplingMenu != null) {
        //    contextMenu.add(couplingMenu);
        //}

        // Train Submenu
        Action trainDeepNet = new AbstractAction("Train...") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                DeepNetDialogsKt.showDeepNetTrainingDialog(deepNet);
            }
        };
        contextMenu.add(trainDeepNet);
        return contextMenu;
    }

    @Override
    public DeepNet getModel() {
        return deepNet;
    }

    public void pullViewPositionFromModel() {
        Point2D point = minus(deepNet.getLocation(), new Point2D.Double(getWidth() / 2, getHeight() / 2));
        this.setGlobalTranslation(point);
    }

    /**
     * Update the position of the model neuron based on the global coordinates
     * of this pnode.
     */
    public void pushViewPositionToModel() {
        Point2D p = this.getGlobalTranslation();
        deepNet.setLocation(new Point2D.Double(p.getX() + getWidth() / 2, p.getY() + getHeight() / 2));
    }

    @Override
    public void offset(double dx, double dy) {
        pushViewPositionToModel();
        super.offset(dx, dy);
    }

    @Override
    public boolean acceptsSourceHandle() {
        return true;
    }

    @Nullable
    @Override
    public JDialog getPropertyDialog() {
        return DeepNetDialogsKt.getDeepNetEditDialog(deepNet);
    }
}