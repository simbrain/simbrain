package org.simbrain.world.odorworld.gui;

import org.piccolo2d.nodes.PPath;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.GridSensor;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

public class GridSensorNode extends EntityAttributeNode {

    /**
     * Sensor diameter
     */
    private static final int SENSOR_RADIUS = 4;

    /**
     * Reference to the sensor this node is representing
     */
    private GridSensor sensor;

    /**
     * A cached grid width for comparision to see if the graphics need to be updated.
     */
    private double gridWidth;

    /**
     * A cached grid height for comparision to see if the graphics need to be updated.
     */
    private double gridHeight;

    /**
     * A cached grid column count for comparision to see if the graphics need to be updated.
     */
    private int gridColumns;

    /**
     * A cached grid row count for comparision to see if the graphics need to be updated.
     */
    private int gridRows;

    /**
     * A cached visibility value for comparision to see if the graphics need to be updated.
     */
    private boolean gridVisibility;

    /**
     * The shape of this node
     */
    private PPath shape;

    private List<PPath> highlightedGrids = new ArrayList<>();

    public GridSensorNode(GridSensor sensor) {

        this.sensor = sensor;
        GeneralPath crossPath = new GeneralPath();
        crossPath.moveTo(-SENSOR_RADIUS, 0);
        crossPath.lineTo(SENSOR_RADIUS, 0);
        crossPath.moveTo(0, -SENSOR_RADIUS);
        crossPath.lineTo(0, SENSOR_RADIUS);
        this.shape = new PPath.Float(crossPath);
        shape.setStroke(new BasicStroke(2f));

        redrawGrid();
        updateGridSizeInfo();
        setPickable(false);
        shape.setPickable(false);
        addChild(shape);

        updateGrid();
    }

    /**
     * Update the cached grid size info
     */
    private void updateGridSizeInfo() {
        this.gridWidth = sensor.getWidth();
        this.gridHeight = sensor.getHeight();
        this.gridColumns = sensor.getColumns();
        this.gridRows = sensor.getRows();
        this.gridVisibility = sensor.getGridVisibility();
    }

    /**
     * Check if the grid size was updated
     *
     * @return true if not updated
     */
    private boolean isGridSizeConsistent() {
        return this.gridWidth == sensor.getWidth()
            && this.gridHeight == sensor.getHeight()
            && this.gridColumns == sensor.getColumns()
            && this.gridRows == sensor.getRows()
            && this.gridVisibility == sensor.getGridVisibility();
    }

    /**
     * Redraw the highlighted
     */
    private void redrawGrid() {
        highlightedGrids.forEach(this::removeChild);
        if (sensor.getGridVisibility()) {
            highlightedGrids = new ArrayList<>();
            for (int i = 0; i < sensor.getColumns() * sensor.getRows(); i++) {
                double x = (i % sensor.getColumns()) * sensor.getWidth();
                double y = (i / sensor.getRows()) * sensor.getHeight();
                PPath newGrid = PPath.createRectangle(x, y, sensor.getWidth(), sensor.getHeight());
                highlightedGrids.add(newGrid);
                addChild(newGrid);
                newGrid.setPickable(false);
                lowerToBottom(newGrid);
            }
        }
    }

    private void updateGrid() {
        if (sensor.getGridVisibility()) {
            double dx = sensor.getX();
            double dy = sensor.getY();
            // double dx = sensor.getX() - sensor.getParent().getX();
            // double dy = sensor.getY() - sensor.getParent().getY();

            for (int i = 0; i < sensor.getColumns() * sensor.getRows(); i++) {
                PPath highlightedGrid = highlightedGrids.get(i);
                highlightedGrid.setOffset(dx, dy);
                Color paint;

                if (i < sensor.getValues().length && sensor.getValues()[i] == sensor.getActivationAmount()) {
                    paint = new Color(255, 0, 0, 128);
                } else {
                    paint = new Color(0, 0, 0, 0);
                }

                highlightedGrid.setPaint(paint);
            }
        }
    }

    @Override
    public void update(OdorWorldEntity entity) {
        if (!isGridSizeConsistent()) {
            redrawGrid();
        }
        updateGridSizeInfo();
        // shape.setOffset(sensor.computeRelativeLocation(entity)); // TODO
        updateGrid();
    }
}
