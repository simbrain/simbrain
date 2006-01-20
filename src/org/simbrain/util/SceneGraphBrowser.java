
package org.simbrain.util;

import java.awt.Component;

import java.awt.event.ActionEvent;

import java.text.NumberFormat;
import java.text.DecimalFormat;

import javax.swing.JTree;
import javax.swing.JLabel;
import javax.swing.Action;
import javax.swing.AbstractAction;

import javax.swing.tree.TreePath;
import javax.swing.tree.TreeModel;
import javax.swing.tree.DefaultTreeCellRenderer;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelListener;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.PRoot;

import edu.umd.cs.piccolo.util.PBounds;

/**
 * Piccolo scene graph browser component.
 */
public class SceneGraphBrowser
    extends JTree {

    /** Root node. */
    private final PRoot root;

    /** Number format. */
    private final NumberFormat nf;

    /** Refresh action. */
    private final Action refresh;


    /**
     * Create a new scene graph browser component with the specified
     * root node.
     *
     * @param root root node
     */
    public SceneGraphBrowser(final PRoot root) {

        super();

        this.root = root;
        this.nf = NumberFormat.getInstance();

        if (nf instanceof DecimalFormat) {
            ((DecimalFormat) nf).setMinimumFractionDigits(1);
            ((DecimalFormat) nf).setMaximumFractionDigits(1);
        }

        refresh = new AbstractAction("Refresh") {
                /** @see AbstractAction */
                public void actionPerformed(final ActionEvent e) {
                    ((Model) getModel()).fireRefresh();
                }
            };

        setModel(new Model());
        setCellRenderer(new Renderer());
    }


    /**
     * Return the refresh action for this scene graph browser.
     *
     * @return the refresh action for this scene graph browser
     */
    public Action getRefreshAction() {
        return refresh;
    }

    /**
     * Renderer for <code>PNode</code>s.
     */
    private class Renderer
        extends DefaultTreeCellRenderer {

        /**
         * Create a new renderer.
         */
        public Renderer() {
            super();
        }


        /**
         * Format bounds with the specified label to the specified string buffer.
         *
         * @param sb string buffer
         * @param label bounds label
         * @param bounds bounds
         */
        private void formatBounds(final StringBuffer sb, final String label, final PBounds bounds) {
            sb.append(" ");
            sb.append(label);
            sb.append("=[");
            sb.append(nf.format(bounds.getWidth()));
            sb.append("x");
            sb.append(nf.format(bounds.getHeight()));
            sb.append("@");
            sb.append(nf.format(bounds.getX()));
            sb.append(",");
            sb.append(nf.format(bounds.getY()));
            sb.append("]");
        }

        /** @see DefaultTreeCellRenderer */
        public Component getTreeCellRendererComponent(final JTree tree,
                final Object value, final boolean isSelected,
                final boolean isExpanded, final boolean isLeaf, final int row,
                final boolean hasFocus) {

            JLabel l = (JLabel) super.getTreeCellRendererComponent(tree, value, isSelected, isExpanded,
                                                                   isLeaf, row, hasFocus);
            PNode node = (PNode) value;
            PBounds bounds = null;
            StringBuffer sb = new StringBuffer();
            sb.append(node.getClass().getName().substring(node.getClass().getName().lastIndexOf(".") + 1));

            formatBounds(sb, "bounds", node.getBoundsReference());
            formatBounds(sb, "fullBounds", node.getFullBoundsReference());
            //formatBounds(sb, "global", node.getGlobalBounds());  method only in cvs version of piccolo
            formatBounds(sb, "globalFull", node.getGlobalFullBounds());

            l.setText(sb.toString());

            return l;
        }
    }

    /**
     * Model that wraps the piccolo scene graph.
     */
    private class Model
        implements TreeModel {

        /** Listener list. */
        private EventListenerList listenerList;


        /**
         * Create a new model.
         */
        public Model() {
            listenerList = new EventListenerList();
        }


        /** @see TreeModel */
        public Object getChild(final Object parent, final int index) {
            return ((PNode) parent).getChild(index);
        }

        /** @see TreeModel */
        public int getChildCount(final Object parent) {
            return ((PNode) parent).getChildrenCount();
        }

        /** @see TreeModel */
        public int getIndexOfChild(final Object parent, final Object child) {
            return -1;
        }

        /** @see TreeModel */
        public Object getRoot() {
            return root;
        }

        /** @see TreeModel */
        public boolean isLeaf(final Object node) {
            return (getChildCount(node) == 0);
        }

        /** @see TreeModel */
        public void addTreeModelListener(final TreeModelListener l) {
            listenerList.add(TreeModelListener.class, l);
        }

        /** @see TreeModel */
        public void removeTreeModelListener(final TreeModelListener l) {
            listenerList.remove(TreeModelListener.class, l);
        }

        /** @see TreeModel */
        public void valueForPathChanged(final TreePath path, final Object newValue) {
            // empty
        }

        /**
         * Fire a wholesale refresh event.  This is an inefficient way
         * of notifying listeners of changes, but is adequate enough for a debug
         * component like this one.
         */
        public void fireRefresh() {
            TreeModelEvent e = null;
            Object[] listeners = listenerList.getListenerList();
            for (int i = (listeners.length - 2); i >= 0; i -= 2) {
                if (listeners[i] == TreeModelListener.class) {
                    if (e == null) {
                        e = new TreeModelEvent(this, new Object[] {root});
                    }
                    ((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
                }
            }
        }
    }
}