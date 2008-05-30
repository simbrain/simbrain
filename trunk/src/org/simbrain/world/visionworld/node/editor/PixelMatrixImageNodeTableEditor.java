/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2006 Jeff Yoshimi <www.jeffyoshimi.net>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.world.visionworld.node.editor;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Stroke;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import edu.umd.cs.piccolox.util.PFixedWidthStroke;

import org.apache.commons.lang.SystemUtils;

import org.simbrain.world.visionworld.PixelMatrix;

import org.simbrain.world.visionworld.node.PixelMatrixImageNode;

/**
 * Pixel matrix node table editor.
 */
public final class PixelMatrixImageNodeTableEditor
    extends JPanel
    implements PixelMatrixImageNodeEditor {

    /** Editor table. */
    private JTable editorTable;

    /** Pixel matrix node. */
    private PixelMatrixImageNode pixelMatrixImageNode;

    /** Zero width stroke. */
    private static final Stroke ZERO_WIDTH_STROKE = new BasicStroke(0.0f);


    /**
     * Create a new pixel matrix image node table editor.
     */
    public PixelMatrixImageNodeTableEditor() {
        super();

        initComponents();
        layoutComponents();
    }

    /**
     * Paint cell renderer.
     */
    private class PaintCellRenderer
        implements TableCellRenderer {

        /** Paint cell renderer component. */
        private JPanel panel;


        /**
         * Create a new paint cell renderer.
         */
        public PaintCellRenderer() {
            panel = new JPanel();
            panel.setOpaque(true);
        }


        /** {@inheritDoc} */
        public Component getTableCellRendererComponent(final JTable table,
                                                       final Object value,
                                                       final boolean isSelected,
                                                       final boolean hasFocus,
                                                       final int row,
                                                       final int column) {
            Paint paint = (Paint) value;
            panel.setBackground((Color) paint);
            return panel;
        }
    }

    /**
     * Color cell renderer.
     */
    private class ColorCellRenderer
        implements TableCellRenderer {

        /** Color cell renderer component. */
        private JPanel panel;


        /**
         * Create a new color cell renderer.
         */
        public ColorCellRenderer() {
            panel = new JPanel();
            panel.setOpaque(true);
        }


        /** {@inheritDoc} */
        public Component getTableCellRendererComponent(final JTable table,
                                                       final Object value,
                                                       final boolean isSelected,
                                                       final boolean hasFocus,
                                                       final int row,
                                                       final int column) {
            Color color = (Color) value;
            panel.setBackground(color);
            return panel;
        }
    }

    /**
     * Stroke cell renderer.
     */
    private class StrokeCellRenderer
        implements TableCellRenderer {

        /** Stroke cell renderer component. */
        private JPanel panel;

        /**
         * Create a new stroke cell renderer.
         */
        public StrokeCellRenderer() {
            panel = new JPanel();
            panel.setOpaque(true);
        }


        /** {@inheritDoc} */
        public Component getTableCellRendererComponent(final JTable table,
                                                       final Object value,
                                                       final boolean isSelected,
                                                       final boolean hasFocus,
                                                       final int row,
                                                       final int column) {
            Paint paint = (Paint) value;
            panel.setBackground((Color) paint);
            return panel;
        }
    }

    /**
     * Initialize components.
     */
    private void initComponents() {
        editorTable = new JTable(new EditorTableModel());
        int rowHeight = editorTable.getRowHeight();
        int rowMargin = editorTable.getRowMargin();
        editorTable.setPreferredScrollableViewportSize(new Dimension(0, 2 * (rowHeight + (2 * rowMargin))));
        editorTable.setDefaultRenderer(Paint.class, new PaintCellRenderer());
        editorTable.setDefaultRenderer(Color.class, new ColorCellRenderer());
        editorTable.setDefaultRenderer(Stroke.class, new StrokeCellRenderer());
    }

    /**
     * Layout components.
     */
    private void layoutComponents() {
        setLayout(new BorderLayout());
        add("Center", new JScrollPane(editorTable));
    }

    /** {@inheritDoc} */
    public Component getEditorComponent() {
        return this;
    }

    /** {@inheritDoc} */
    public void setPixelMatrixImageNode(final PixelMatrixImageNode pixelMatrixImageNode) {
        PixelMatrixImageNode oldPixelMatrixImageNode = this.pixelMatrixImageNode;
        this.pixelMatrixImageNode = pixelMatrixImageNode;
        firePropertyChange("pixelMatrixImageNode", oldPixelMatrixImageNode, this.pixelMatrixImageNode);
        updateEditor();
    }

    /**
     * Update editor.
     */
    private void updateEditor() {
        EditorTableModel editorTableModel = (EditorTableModel) editorTable.getModel();
        editorTableModel.fireTableDataChanged();
        editorTable.setEnabled(pixelMatrixImageNode != null);
    }

    /**
     * Editor table model.
     */
    private class EditorTableModel
        extends AbstractTableModel {

        /** {@inheritDoc} */
        public int getRowCount() {
            return (pixelMatrixImageNode == null) ? 0 : 1;
        }

        /** {@inheritDoc} */
        public int getColumnCount() {
            return 8;
        }

        /** {@inheritDoc} */
        public String getColumnName(final int column) {
            switch (column)
            {
            case 0:
                return "Pixel matrix";
            case 1:
                return "Dimensions";
            case 2:
                return "Visible";
            case 3:
                return "Transparency";
            case 4:
                return "Outline paint";
            case 5:
                return "Outline stroke";
            case 6:
                return "Pen foreground";
            case 7:
                return "Pen background";
            default:
                return null;
            }
        }

        /** {@inheritDoc} */
        public Class getColumnClass(final int column) {
            switch (column)
            {
            case 0:
                return String.class;
            case 1:
                return String.class;
            case 2:
                return Boolean.class;
            case 3:
                return Float.class;
            case 4:
                return Paint.class;
            case 5:
                return Stroke.class;
            case 6:
                return Color.class;
            case 7:
                return Color.class;
            default:
                return null;
            }
        }

        /** {@inheritDoc} */
        public boolean isCellEditable(final int row, final int column) {
            if (pixelMatrixImageNode == null) {
                return false;
            }
            switch (column) {
            case 0:
                return false;
            case 1:
                return false;
            case 2:
                return true;
            case 3:
                return true;
            case 4:
                return true;
            case 5:
                return true;
            case 6:
                return true;
            case 7:
                return true;
            default:
                return false;
            }
        }

        /** {@inheritDoc} */
        public void setValueAt(final Object value, final int row, final int column) {
            if (pixelMatrixImageNode != null) {
                switch (column) {
                case 0:
                    break;
                case 1:
                    break;
                case 2:
                    pixelMatrixImageNode.setVisible((Boolean) value);
                    break;
                case 3:
                    pixelMatrixImageNode.setTransparency((Float) value);
                    break;
                case 4:
                    pixelMatrixImageNode.setOutlinePaint((Paint) value);
                    break;
                case 5:
                    float strokeWidth = ((Float) value).floatValue();
                    Stroke outlineStroke = SystemUtils.IS_OS_MAC_OSX ?
                        new BasicStroke(strokeWidth) : new PFixedWidthStroke(strokeWidth);
                    pixelMatrixImageNode.setOutlineStroke(outlineStroke);
                    break;
                case 6:
                    pixelMatrixImageNode.setPenForeground((Color) value);
                    break;
                case 7:
                    pixelMatrixImageNode.setPenBackground((Color) value);
                    break;
                default:
                    break;
                }
            }
        }

        /** {@inheritDoc} */
        public Object getValueAt(final int row, final int column) {
            PixelMatrix pixelMatrix = pixelMatrixImageNode.getPixelMatrix();
            switch (column) {
            case 0:
                return (pixelMatrixImageNode == null) ? " (none)" : "Pixel matrix";
            case 1:
                return (pixelMatrixImageNode == null) ? "" : pixelMatrix.getWidth() + " x " + pixelMatrix.getHeight();
            case 2:
                return (pixelMatrixImageNode == null) ? Boolean.FALSE : pixelMatrixImageNode.getVisible();
            case 3:
                return (pixelMatrixImageNode == null) ? Float.valueOf(0.0f) : pixelMatrixImageNode.getTransparency();
            case 4:
                return (pixelMatrixImageNode == null) ? Color.BLACK : pixelMatrixImageNode.getOutlinePaint();
            case 5:
                return (pixelMatrixImageNode == null) ? ZERO_WIDTH_STROKE : pixelMatrixImageNode.getOutlineStroke();
            case 6:
                return (pixelMatrixImageNode == null) ? Color.BLACK : pixelMatrixImageNode.getPenForeground();
            case 7:
                return (pixelMatrixImageNode == null) ? Color.BLACK : pixelMatrixImageNode.getPenBackground();
            default:
                return null;
            }
        }
    }
}