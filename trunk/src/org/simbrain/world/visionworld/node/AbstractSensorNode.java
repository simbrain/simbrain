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
package org.simbrain.world.visionworld.node;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;

import java.awt.event.ActionEvent;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;

import edu.umd.cs.piccolo.util.PPaintContext;

import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;

import edu.umd.cs.piccolox.util.PFixedWidthStroke;

import org.apache.commons.lang.SystemUtils;

import org.simbrain.world.visionworld.Sensor;

import org.simbrain.world.visionworld.dialog.EditSensorDialog;

/**
 * Abstract sensor node.
 */
abstract class AbstractSensorNode
    extends PNode {

    /** Sensor. */
    private final Sensor sensor;

    /** Default mouseover paint. */
    private static final Paint DEFAULT_MOUSEOVER_PAINT = Color.GRAY;

    /** Default outline paint. */
    private static final Paint DEFAULT_OUTLINE_PAINT = Color.BLACK;

    /** Default outline stroke. */
    private static final Stroke DEFAULT_OUTLINE_STROKE = SystemUtils.IS_OS_MAC_OSX ? new BasicStroke(0.5f) : new PFixedWidthStroke(0.5f);

    /** Default selected paint. */
    private static final Paint DEFAULT_SELECTED_PAINT = Color.GRAY;

    /** Mouseover paint. */
    private Paint mouseoverPaint = DEFAULT_MOUSEOVER_PAINT;

    /** Outline paint. */
    private Paint outlinePaint = DEFAULT_OUTLINE_PAINT;

    /** Outline stroke. */
    private Stroke outlineStroke = DEFAULT_OUTLINE_STROKE;

    /** Selected paint. */
    private Paint selectedPaint = DEFAULT_SELECTED_PAINT;

    /** True if the mouse is over this sensor node. */
    private boolean mouseover = false;

    /** True if this sensor node is selected. */
    private boolean selected = false;

    /** Context menu. */
    private JPopupMenu contextMenu;


    /**
     * Create a new abstract sensor node with the specified sensor.
     *
     * @param sensor sensor, must not be null
     */
    protected AbstractSensorNode(final Sensor sensor) {
        super();
        if (sensor == null) {
            throw new IllegalArgumentException("sensor must not be null");
        }
        this.sensor = sensor;
        setHeight(this.sensor.getReceptiveField().getHeight());
        setWidth(this.sensor.getReceptiveField().getWidth());

        contextMenu = new JPopupMenu("Context menu");
        contextMenu.add(new EditSensorAction());
        //contextMenu.add(new JMenuItem("Edit selected sensor(s)..."));

        setPaint(new Color(0, 0, 0, 0));
        setOutlinePaint(new Color(0, 0, 0, 0));
        setMouseoverPaint(new Color(80, 80, 80, 40));
        setSelectedPaint(new Color(80, 80, 80, 80));

        addInputEventListener(new ToolTipTextUpdater());
        addInputEventListener(new ContextMenuEventHandler());
    }


    /**
     * Return the tool tip text for this sensor node.
     *
     * @return the tool tip text for this sensor node
     */
    protected String getToolTipText() {
        StringBuffer sb = new StringBuffer();
        sb.append("Sensor");
        // todo:  call sample if I can find a reference to pixelMatrix
        sb.append("\n  Filter:  ");
        sb.append(sensor.getFilter().getClass().getSimpleName());
        sb.append("\n  Receptive field:  ");
        sb.append(sensor.getReceptiveField().getHeight());
        sb.append("x");
        sb.append(sensor.getReceptiveField().getWidth());
        sb.append("\n  Location:  (");
        sb.append(sensor.getReceptiveField().getX());
        sb.append(", ");
        sb.append(sensor.getReceptiveField().getY());
        sb.append(")");
        return sb.toString();
    }

    /**
     * Return the context menu for this sensor node.
     *
     * @return the context menu for this sensor node
     */
    protected JPopupMenu getContextMenu() {
        return contextMenu;
    }

    /**
     * Return the sensor for this sensor node.
     * The sensor will not be null.
     *
     * @return the sensor for this sensor node
     */
    public final Sensor getSensor() {
        return sensor;
    }

    /**
     * Edit the sensor for this sensor node.
     */
    public final void edit() {
        EditSensorDialog d = new EditSensorDialog(sensor);
        d.setBounds(100, 100, 450, 500);
        d.setVisible(true);
    }

    /**
     * Return true if the mouse is over this sensor node.
     *
     * @return true if the mouse is over this sensor node
     */
    protected final boolean isMouseover() {
        return mouseover;
    }

    /**
     * Set to true if the mouse is over this sensor node.
     *
     * @param true if the mouse is over this sensor node
     */
    protected final void setMouseover(final boolean mouseover) {
        this.mouseover = mouseover;
    }

    /**
     * Return true if this sensor node is selected.
     *
     * @return true if this sensor node is selected
     */
    protected final boolean isSelected() {
        return selected;
    }

    /**
     * Set to true if this sensor node is selected.
     *
     * @param selected true if this sensor node is selected
     */
    public final void setSelected(final boolean selected) {
        this.selected = selected;
    }

    // todo:  add context menu, tooltip text
    // todo:  add action methods, edit sensor, add coupling, edit properties, etc.

    /**
     * Return the mouseover paint for this sensor node.
     *
     * @return the mouseover paint for this sensor node
     */
    public final Paint getMouseoverPaint() {
        return mouseoverPaint;
    }

    /**
     * Set the mouseover paint for this sensor node to <code>mouseoverPaint</code>.
     *
     * @param mouseoverPaint mouseover paint for this sensor node
     */
    public final void setMouseoverPaint(final Paint mouseoverPaint) {
        Paint oldMouseoverPaint = this.mouseoverPaint;
        this.mouseoverPaint = mouseoverPaint;
        firePropertyChange("mouseoverPaint", oldMouseoverPaint, this.mouseoverPaint);
    }

    /**
     * Return the outline paint for this sensor node.
     *
     * @return the outline paint for this sensor node
     */
    public final Paint getOutlinePaint() {
        return outlinePaint;
    }

    /**
     * Set the outline paint for this sensor node to <code>outlinePaint</code>.
     *
     * @param outlinePaint outline paint for this sensor node
     */
    public final void setOutlinePaint(final Paint outlinePaint) {
        Paint oldOutlinePaint = this.outlinePaint;
        this.outlinePaint = outlinePaint;
        firePropertyChange("outlinePaint", oldOutlinePaint, this.outlinePaint);
    }

    /**
     * Return the outline stroke for this sensor node.
     * The outline stroke will not be null.
     *
     * @return the outline stroke for this sensor node
     */
    public final Stroke getOutlineStroke() {
        return outlineStroke;
    }

    /**
     * Set the outline stroke for this sensor node to <code>outlineStroke</code>.
     *
     * @param outlineStroke outline stroke for this sensor node, must not be null
     */
    public final void setOutlineStroke(final Stroke outlineStroke) {
        if (outlineStroke == null) {
            throw new IllegalArgumentException("outlineStroke must not be null");
        }
        Stroke oldOutlineStroke = this.outlineStroke;
        this.outlineStroke = outlineStroke;
        firePropertyChange("outlineStroke", oldOutlineStroke, this.outlineStroke);
    }

    /**
     * Return the selected paint for this sensor node.
     *
     * @return the selected paint for this sensor node
     */
    public final Paint getSelectedPaint() {
        return selectedPaint;
    }

    /**
     * Set the selected paint for this sensor node to <code>selectedPaint</code>.
     *
     * @param selectedPaint selected paint for this sensor node
     */
    public final void setSelectedPaint(final Paint selectedPaint) {
        Paint oldSelectedPaint = this.selectedPaint;
        this.selectedPaint = selectedPaint;
        firePropertyChange("selectedPaint", oldSelectedPaint, this.selectedPaint);
    }

    /** {@inheritDoc} */
    protected final void paint(final PPaintContext paintContext) {

        Graphics2D g = paintContext.getGraphics();
        Rectangle2D rect = getBounds();

        g.setPaint(getPaint());

        if (mouseover && (mouseoverPaint != null)) {
            g.setPaint(mouseoverPaint);
        }
        if (selected && (selectedPaint != null)) {
            g.setPaint(selectedPaint);
        }
        g.fill(rect);

        if (outlinePaint != null) {
            g.setPaint(outlinePaint);
            g.setStroke(StrokeUtils.prepareStroke(outlineStroke, paintContext));
            g.draw(rect);
        }
    }

    /**
     * Tool tip text updater.
     */
    private class ToolTipTextUpdater
        extends PBasicInputEventHandler {

        /** {@inheritDoc} */
        public void mouseEntered(final PInputEvent event) {
            //event.setHandled(true);
            //setMouseover(true);
            ((PCanvas) event.getComponent()).setToolTipText(getToolTipText());
        }

        /** {@inheritDoc} */
        public void mouseExited(final PInputEvent event) {
            //event.setHandled(true);
            //setMouseover(false);
            ((PCanvas) event.getComponent()).setToolTipText(null);
        }
    }


    /**
     * Context menu event handler.
     */
    private class ContextMenuEventHandler
        extends PBasicInputEventHandler {

        /**
         * Show the context menu.
         *
         * @param event event
         */
        private void showContextMenu(final PInputEvent event) {
            //event.setHandled(true);
            JPopupMenu contextMenu = getContextMenu();
            Point2D canvasPosition = event.getCanvasPosition();
            contextMenu.show((PCanvas) event.getComponent(), (int) canvasPosition.getX(), (int) canvasPosition.getY());
        }

        /** {@inheritDoc} */
        public void mousePressed(final PInputEvent event) {
            if (event.isPopupTrigger()) {
                showContextMenu(event);
            }
        }

        /** {@inheritDoc} */
        public void mouseReleased(final PInputEvent event) {
            if (event.isPopupTrigger()) {
                showContextMenu(event);
            }
        }
    }

    /**
     * Edit sensor action.
     */
    private class EditSensorAction
        extends AbstractAction {

        /**
         * Create a new edit sensor action.
         */
        EditSensorAction() {
            super("Edit sensor...");
        }


        /** {@inheritDoc} */
        public void actionPerformed(final ActionEvent event) {
            edit();
        }
    }
}
