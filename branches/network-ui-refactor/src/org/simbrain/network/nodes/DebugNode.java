
package org.simbrain.network.nodes;

import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;

import edu.umd.cs.piccolo.PNode;

import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;

/**
 * Debug node.
 */
public final class DebugNode
    extends ScreenElement {

    /**
     * Create a new debug node.
     */
    public DebugNode(NetworkPanel net, final double x, final double y) {

        super(net);
        offset(x, y);

        setPickable(true);
        setChildrenPickable(false);

        PText label = new PText("Debug");
        PNode rect = PPath.createRectangle(0.0f, 0.0f, 50.0f, 50.0f);

        label.offset(7.5d, 21.0d);

        rect.addChild(label);
        addChild(rect);

        setBounds(rect.getBounds());
    }


    /** @see ScreenElement */
    protected boolean hasToolTipText() {
        return true;
    }

    /** @see ScreenElement */
    protected String getToolTipText() {
        return "debug";
    }

    /** @see ScreenElement */
    protected boolean hasContextMenu() {
        return true;
    }

    /** @see ScreenElement */
    protected JPopupMenu getContextMenu() {

        JPopupMenu contextMenu = new JPopupMenu();

        // add actions
        contextMenu.add(new JMenuItem("Debug node"));
        contextMenu.add(new JMenuItem("Node specific context menu item"));
        contextMenu.add(new JMenuItem("Node specific context menu item"));

        return contextMenu;
    }

    /** @see ScreenElement */
    protected boolean hasPropertyDialog() {
        return true;
    }

    /** @see ScreenElement */
    protected JDialog getPropertyDialog() {
        //
        // a real property dialog will need reference to this
        //    and perhaps to the networkPanel . . .
        // if there is more than one of this type of node selected,
        //    a property dialog suitable for multiple nodes should
        //    be created.
        return new DebugNodePropertyDialog();
    }


    /**
     * Debug node property dialog.  (just an placeholder)
     */
    private class DebugNodePropertyDialog
        extends JDialog {

        public DebugNodePropertyDialog() {
            super((java.awt.Frame) null, "Debug Node Property Dialog");

            javax.swing.JPanel pane = new javax.swing.JPanel();
            pane.setLayout(new java.awt.BorderLayout());
            pane.setBorder(new javax.swing.border.EmptyBorder(11, 11, 11, 11));
            pane.add("Center", new javax.swing.JLabel("Debug Node Property Dialog"));
            pane.add("South", createButtonPanel());
            setContentPane(pane);
            setBounds(100, 100, 400, 400);
        }

        private javax.swing.JComponent createButtonPanel() {
            javax.swing.JPanel pane = new javax.swing.JPanel();
            pane.setLayout(new javax.swing.BoxLayout(pane, javax.swing.BoxLayout.X_AXIS));
            pane.add(javax.swing.Box.createHorizontalGlue());
            pane.add(javax.swing.Box.createHorizontalGlue());
            pane.add(new javax.swing.JButton("Cancel"));
            pane.add(javax.swing.Box.createHorizontalStrut(11));
            pane.add(new javax.swing.JButton("OK"));
            return pane;
        }
    }
}