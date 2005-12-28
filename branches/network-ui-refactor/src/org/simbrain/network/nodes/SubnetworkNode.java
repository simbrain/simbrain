
package org.simbrain.network.nodes;

import java.awt.Color;

import java.awt.event.ActionEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JDialog;
import javax.swing.JPopupMenu;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import edu.umd.cs.piccolo.PNode;

import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;

import org.simbrain.network.NetworkPanel;

/**
 * Subnetwork node.
 *
 * <p>
 * Node composition:
 * <pre>
 * SubnetworkNode
 *   |
 *   + -- TabNode
 *   |     |
 *   |     + -- PText, for tab label
 *   |     |
 *   |     + -- PPath, for tab background
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
public final class SubnetworkNode
    extends ScreenElement {

    /** Tab node. */
    private TabNode tab;

    /** Outline node. */
    private PPath outline;

    /** True if this subnetwork node is to show its tab. */
    private boolean showTab;

    /** True if this subnetwork node is to show its outline. */
    private boolean showOutline;

    private Action showTabAction;
    private Action hideTabAction;
    private Action showOutlineAction;
    private Action hideOutlineAction;


    /**
     * Create a new subnetwork node with the specified network panel.
     *
     * @param networkPanel network panel for this subnetwork node
     */
    public SubnetworkNode(final NetworkPanel networkPanel) {

        super(networkPanel);

        showTab = false;
        showOutline = false;
        tab = new TabNode();

        setPickable(true);

        showTabAction = new AbstractAction("Show tab") {
                public void actionPerformed(final ActionEvent event) {
                    setShowTab(true);
                }
            };
        hideTabAction = new AbstractAction("Hide tab") {
                public void actionPerformed(final ActionEvent event) {
                    setShowTab(false);
                }
            };
        showOutlineAction = new AbstractAction("Show outline") {
                public void actionPerformed(final ActionEvent event) {
                    setShowOutline(true);
                }
            };
        hideOutlineAction = new AbstractAction("Hide outline") {
                public void actionPerformed(final ActionEvent event) {
                    setShowOutline(false);
                }
            };

        addPropertyChangeListener("showTab", new PropertyChangeListener()
            {
                /** @see PropertyChangeListener */
                public void propertyChange(final PropertyChangeEvent event) {
                    boolean showTab = ((Boolean) event.getNewValue()).booleanValue();
                    if (showTab) {
                        System.out.println("adding tab...");
                        // set tab width to full bounds width
                        addChild(tab);
                    }
                    else {
                        System.out.println("removing tab...");
                        removeChild(tab);
                    }
                }
            });

        addPropertyChangeListener("showOutline", new PropertyChangeListener()
            {
                /** @see PropertyChangeListener */
                public void propertyChange(final PropertyChangeEvent event) {
                    boolean showOutline = ((Boolean) event.getNewValue()).booleanValue();
                    if (showOutline) {
                        System.out.println("adding outline...");
                        // create outline to full bounds width
                        outline = PPath.createRectangle(0.0f, 0.0f, 200.0f, 200.0f);
                        outline.setStrokePaint(new Color(255, 255, 255, 0));
                        outline.setStrokePaint(Color.LIGHT_GRAY);

                        addChild(outline);

                        outline.moveInBackOf(tab);
                    }
                    else {
                        if (outline != null) {
                            System.out.println("removing outline...");
                            removeChild(outline);
                        }
                    }
                }
            });

        SwingUtilities.invokeLater(new Runnable() {

                /** @see Runnable */
                public void run() {
                    setShowTab(true);
                    setShowOutline(true);
                }
            });
    }


    /**
     * Set to true if this subnetwork node is to show its tab.
     *
     * <p>This is a bound property.</b>
     *
     * @param showTab true if this subnetwork node is to show its tab
     */
    public void setShowTab(final boolean showTab) {
        boolean oldShowTab = this.showTab;
        this.showTab = showTab;
        firePropertyChange("showTab", Boolean.valueOf(oldShowTab), Boolean.valueOf(this.showTab));
    }

    /**
     * Return true if this subnetwork node is to show its tab.
     *
     * @return true if this subnetwork node is to show its tab
     */
    public boolean getShowTab() {
        return showTab;
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

    /** @see ScreenElement */
    public boolean isSelectable() {
        return true;
    }

    /** @see ScreenElement */
    public boolean isDraggable() {
        return true;
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
        contextMenu.add(showTabAction);
        contextMenu.add(hideTabAction);
        contextMenu.add(showOutlineAction);
        contextMenu.add(hideOutlineAction);

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


    /**
     * Subnetwork tab node.
     *
     * TODO:
     * Width needs to expand to encompass width of all child nodes
     * Dragging this node around needs to move all child nodes and
     *    parent subnetwork and outline nodes
     */
    private class TabNode
        extends PNode {

        /** Tab label. */
        private PText label;

        /** Tab background. */
        private PPath background;


        /**
         * Create a new subnetwork tab node with the specified network panel.
         */
        public TabNode() {

            super();

            setPickable(false);

            label = new PText("Subnetwork");
            background = PPath.createRectangle(0.0f, 0.0f, 200.0f, 22.0f);

            label.setPickable(false);
            background.setPickable(false);

            // offset from parent in parent's local coordinate system
            label.offset(5.0f, 6.0f);

            background.setPaint(Color.LIGHT_GRAY);
            background.setStrokePaint(Color.DARK_GRAY);

            addChild(background);
            addChild(label);

            setBounds(background.getBounds());
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
}