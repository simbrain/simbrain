
package org.simbrain.network.nodes;

import java.awt.Color;
import java.awt.Shape;
import java.awt.BasicStroke;

import java.awt.geom.RoundRectangle2D;

import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import edu.umd.cs.piccolo.PNode;

import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;

import org.simbrain.network.NetworkPanel;

/**
 * Subnetwork node2.
 *
 * <p>
 * Node composition:
 * <pre>
 * SubnetworkNode
 *   |
 *   + -- CalloutNode
 *   |     |
 *   |     + -- PText, for callout label
 *   |     |
 *   |     + -- PPath, for callout outline
 *   |     |
 *   |     + -- ... (child neuron, synapse, etc. nodes)
 *   |
 *   + -- PPath, for connector
 *   |
 *   + -- PPath, for outline
 *   |
 *   + -- ... (or add them here?  I prefer this currently)
 * </pre>
 * </p>
 *
 * TODO:
 * Height and width (of outline at least) needs to expand to
 *    encompass height and width of all child nodes
 */
public final class SubnetworkNode2
    extends PNode {

    /** Callout node. */
    private CalloutNode callout;

    /** Connector node. */
    private PPath connector;

    /** Outline node. */
    private PPath outline;

    /** True if this subnetwork node is to show its callout. */
    private boolean showCallout;

    /** True if this subnetwork node is to show its connector. */
    private boolean showConnector;

    /** True if this subnetwork node is to show its outline. */
    private boolean showOutline;


    /**
     * Create a new subnetwork node with the specified network panel.
     *
     * @param networkPanel network panel for this subnetwork node
     */
    public SubnetworkNode2(final NetworkPanel networkPanel) {

        super();

        showCallout = true;
        showConnector = true;
        showOutline = true;
        setPickable(false);

        callout = new CalloutNode(networkPanel);
        callout.offset(220.0d, -20.0d);

        connector = PPath.createLine(200.0f, 0.0f, 220.0f, -5.5f);
        Shape rect = new RoundRectangle2D.Float(0.0f, 0.0f, 200.0f, 200.0f, 8.0f, 8.0f);
        outline = new PPath(rect, new DashStroke());

        outline.setStrokePaint(Color.LIGHT_GRAY);
        outline.setPickable(false);

        connector.setStroke(new DashStroke());
        connector.setStrokePaint(Color.LIGHT_GRAY);
        connector.setPickable(false);

        // TODO:
        // Connector needs to behave similar to synapse node on drags

        addChild(outline);
        addChild(connector);
        addChild(callout);
    }


    /**
     * Set to true if this subnetwork node is to show its callout.
     *
     * <p>This is a bound property.</b>
     *
     * @param showTab true if this subnetwork node is to show its callout
     */
    public void setShowCallout(final boolean showCallout) {
        boolean oldShowCallout = this.showCallout;
        this.showCallout = showCallout;
        firePropertyChange("showCallout", Boolean.valueOf(oldShowCallout), Boolean.valueOf(this.showCallout));
    }

    /**
     * Return true if this subnetwork node is to show its callout.
     *
     * @return true if this subnetwork node is to show its callout
     */
    public boolean getShowCallout() {
        return showCallout;
    }

    /**
     * Set to true if this subnetwork node is to show its connector.
     *
     * <p>This is a bound property.</b>
     *
     * @param showConnector true if this subnetwork node is to show its connector
     */
    public void setShowConnector(final boolean showConnector) {
        boolean oldShowConnector = this.showConnector;
        this.showConnector = showConnector;
        firePropertyChange("showConnector", Boolean.valueOf(oldShowConnector), Boolean.valueOf(showConnector));
    }

    /**
     * Return true if this subnetwork node is to show its connector.
     *
     * @return true if this subnetwork node is to show its connector
     */
    public boolean getShowConnector() {
        return showConnector;
    }

    /**
     * Set to true if this subnetwork node is to show its outline.
     *
     * <p>This is a bound property.</b>
     *
     * @param showOutline true if this subnetwork node is to show its outline
     */
    public void setShowOutline(final boolean showOutline) {
        boolean oldShowOutline = this.showOutline;
        this.showOutline = showOutline;
        firePropertyChange("showOutline", Boolean.valueOf(oldShowOutline), Boolean.valueOf(showOutline));
    }

    /**
     * Return true if this subnetwork node is to show its outline.
     *
     * @return true if this subnetwork node is to show its outline
     */
    public boolean getShowOutline() {
        return showOutline;
    }


    /**
     * Subnetwork callout node.
     */
    private class CalloutNode
        extends ScreenElement {

        /** Callout label. */
        private PText label;

        /** Callout outline. */
        private PPath outline;


        /**
         * Create a new subnetwork callout node with the specified network panel.
         *
         * @param networkPanel network panel for this subnetwork callout node
         */
        public CalloutNode(final NetworkPanel networkPanel) {

            super(networkPanel);

            setPickable(true);

            label = new PText("Subnetwork");
            Shape rect = new RoundRectangle2D.Float(0.0f, 0.0f, 200.0f, 25.0f, 8.0f, 8.0f);
            outline = new PPath(rect, new DashStroke());

            // offset from parent in parent's local coordinate system
            label.offset(5.0f, 7.0f);
            label.setPickable(false);

            outline.setStrokePaint(Color.LIGHT_GRAY);
            outline.setPickable(false);

            addChild(outline);
            addChild(label);

            setBounds(outline.getBounds());
        }


        /** @see ScreenElement */
        protected boolean hasToolTipText() {
            return true;
        }

        /** @see ScreenElement */
        protected String getToolTipText() {
            return "subnetwork";
        }

        /** @see ScreenElement */
        protected boolean hasContextMenu() {
            return true;
        }

        /** @see ScreenElement */
        protected JPopupMenu getContextMenu() {

            JPopupMenu contextMenu = new JPopupMenu();
            contextMenu.add(new javax.swing.JMenuItem("subnetwork tab node action"));
            contextMenu.add(new javax.swing.JMenuItem("subnetwork tab node action"));
            return contextMenu;
        }

        /** @see ScreenElement */
        protected boolean hasPropertyDialog() {
            return true;
        }

        /** @see ScreenElement */
        protected JDialog getPropertyDialog() {
            return new SubnetworkPropertyDialog();
        }
    }


    /**
     * Subnetwork property dialog.  (just an placeholder)
     */
    private class SubnetworkPropertyDialog
        extends JDialog {

        public SubnetworkPropertyDialog() {
            super((java.awt.Frame) null, "Subnetwork Property Dialog");

            javax.swing.JPanel pane = new javax.swing.JPanel();
            pane.setLayout(new java.awt.BorderLayout());
            pane.setBorder(new javax.swing.border.EmptyBorder(11, 11, 11, 11));
            pane.add("Center", new javax.swing.JLabel("Subnetwork Property Dialog"));
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

    /**
     * Dash stroke.
     */
    private class DashStroke
        extends BasicStroke {

        /**
         * Create a new dash stroke.
         */
        public DashStroke()
        {
            super(1.0f, CAP_ROUND, JOIN_ROUND, 10.0f, new float[] { 5.0f, 4.0f }, 1.0f);
        }
    }
}