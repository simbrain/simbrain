package org.simbrain.world.odorworld.sensors;

import org.simbrain.util.UserParameter;
import org.simbrain.util.piccolo.TileMap;
import org.simbrain.util.propertyeditor2.EditableObject;
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
            defaultValue = "1", order = 3)
    private double outputAmount = 1;

    /**
     * The tile id of the tile to sense
     */
    @UserParameter(label = "Tile ID",
            description = "The ID of the tile this sensor should sense.",
            defaultValue = "0", order = 4)
    private int tileIdToSense = 0;

    /**
     * Construct a tile sensor.
     *
     * @param parent the parent entity
     */
    public TileSensor(OdorWorldEntity parent) {
        super(parent, "Tile Sensor");
    }

    /**
     * Construct a tile sensor to sense a specific tile of the given ID.
     *
     * @param parent the parent entity
     * @param tileId the tile id of the tile to sense
     */
    public TileSensor(OdorWorldEntity parent, int tileId) {
        this(parent);
        this.tileIdToSense = tileId;
    }

    /**
     * Default constructor for {@link org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor}.
     *
     * NOTE:
     * {@link org.simbrain.world.odorworld.dialogs.AddSensorDialog} handles the set up of {@link #parent}.
     * When calling this directly, remember to set up the required field {@link #parent} accordingly.
     */
    public TileSensor() {
        super();
    }

    @Override
    public void update() {
        value = 0;

        TileMap tileMap = parent.getParentWorld().getTileMap();

        if (tileMap.hasTileIdAtPixel(tileIdToSense, parent.getCenterX(), parent.getCenterY())) {
            value = outputAmount;
        }
    }

    @Producible(idMethod = "getId", customDescriptionMethod = "getAttributeDescription")
    public double getCurrentValue() {
        return value;
    }

    @Override
    public String getTypeDescription() {
        return "Tile";
    }

    @Override
    public void setParent(OdorWorldEntity parent) {
        this.parent = parent;
    }

    @Override
    public String getName() {
        return "Tile";
    }

    @Override
    public EditableObject copy() {
        return new TileSensor(parent, tileIdToSense);
    }
}
