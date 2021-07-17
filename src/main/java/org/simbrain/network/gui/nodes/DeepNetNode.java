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
import org.simbrain.network.kotlindl.DeepNet;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;

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
    private DeepNet net;

    /**
     * Parent panel.
     */
    private NetworkPanel networkPanel;

    /**
     * Width in pixels of the main display box.
     */
    private final float boxWidth = 80;

    /**
     * Height in pixels of the main display box.
     */
    private final float boxHeight = 120;

    /**
     * Text showing info about the array.
     */
    private PText infoText;

    /**
     * Font for info text.
     */
    public static final Font INFO_FONT = new Font("Arial", Font.PLAIN, 8);

    private PPath box = PPath.createRectangle(0, 0, boxWidth, boxHeight);

    public DeepNetNode(NetworkPanel networkPanel, DeepNet dn) {
        super(networkPanel);
        this.net = dn;

        box.setPickable(true);
        addChild(box);

        LocationEvents events = dn.getEvents();
        events.onDeleted(n -> removeFromParent());
        events.onUpdated(() -> {
            // renderArrayToActivationsImage();
            updateInfoText();
        });

        // Border box determines bounds
        PBounds bounds = box.getBounds();
        setBounds(bounds);

        // Info text
        infoText = new PText();
        infoText.setFont(INFO_FONT);
        addChild(infoText);
        infoText.offset(8, 8);
        updateInfoText();

        this.centerFullBoundsOnPoint(net.getLocation().getX(), net.getLocation().getY());

        net.getEvents().onLocationChange(this::pullViewPositionFromModel);
    }

    /**
     * Update status text.
     */
    private void updateInfoText() {
        infoText.setText(net.toString());
        PBounds pb = infoText.getBounds();
        box.setBounds(pb.x-2, pb.y-2, pb.width+20, pb.height+20);
        setBounds(box.getBounds());
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

        // Coupling menu
        //contextMenu.addSeparator();
        //JMenu couplingMenu = networkPanel.getCouplingMenu(neuronArray);
        //if (couplingMenu != null) {
        //    contextMenu.add(couplingMenu);
        //}

        return contextMenu;
    }

    @Override
    public DeepNet getModel() {
        return net;
    }

    public void pullViewPositionFromModel() {
        Point2D point = minus(net.getLocation(), new Point2D.Double(boxWidth / 2, boxHeight / 2));
        this.setGlobalTranslation(point);
    }

    /**
     * Update the position of the model neuron based on the global coordinates
     * of this pnode.
     */
    public void pushViewPositionToModel() {
        Point2D p = this.getGlobalTranslation();
        net.setLocation(new Point2D.Double(p.getX() + boxWidth / 2, p.getY() + boxHeight / 2));
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
        StandardDialog dialog = AnnotatedPropertyEditor.getDialog(net);
        dialog.addClosingTask(() -> {
                net.getEvents().fireUpdated();
        });
        return dialog;
    }
}