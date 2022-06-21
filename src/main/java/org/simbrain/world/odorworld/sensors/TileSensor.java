package org.simbrain.world.odorworld.sensors;

import org.simbrain.util.UserParameter;
import org.simbrain.util.piccolo.TileMap;
import org.simbrain.workspace.Producible;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

public class TileSensor extends Sensor {

    /**
     * Current value of the sensor.
     */
    private double value = 0;

    /**
     * The amount of activation when this sensor is activated
     */
    @UserParameter(label = "Output Amount",
            description = "The amount of activation to be sent to a neuron coupled with this sensor.",
            order = 3)
    private double outputAmount = 1;

    /**
     * The tile id of the tile to sense
     */
    @UserParameter(label = "Tile ID",
            description = "The ID of the tile this sensor should sense.",
            order = 4)
    private int tileIdToSense = 0;

    /**
     * Construct a tile sensor to sense a specific tile of the given ID.
     *
     * @param tileId the tile id of the tile to sense
     */
    public TileSensor(int tileId) {
        this.tileIdToSense = tileId;
    }

    public TileSensor() {
        super("Tile Sensor");
    }

    /**
     * Construct a copy of a tile sensor.
     *
     * @param tileSensor the tile sensor to copy
     */
    public TileSensor(TileSensor tileSensor) {
        super(tileSensor);
        this.outputAmount = tileSensor.outputAmount;
        this.tileIdToSense = tileSensor.tileIdToSense;
    }

    @Override
    public void update(OdorWorldEntity parent) {
        value = 0;

        TileMap tileMap = parent.getWorld().getTileMap();

        if (tileMap.hasTileIdAtPixel(tileIdToSense, parent.getLocation().getX(), parent.getLocation().getY())) {
            value = outputAmount;
        }
    }

    @Producible(customDescriptionMethod = "getAttributeDescription")
    public double getCurrentValue() {
        return value;
    }

    @Override
    public String getName() {
        return "Tile Sensor";
    }

    @Override
    public TileSensor copy() {
        return new TileSensor(this);
    }

    @Override
    public String getLabel() {
        if (super.getLabel().isEmpty()) {
            return "Sense tile id " + tileIdToSense;
        } else {
            return super.getLabel();
        }
    }
}