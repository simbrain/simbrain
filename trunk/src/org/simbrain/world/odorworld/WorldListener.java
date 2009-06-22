package org.simbrain.world.odorworld;

import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.Sensor;

public interface WorldListener {

    public void updated();

    public void effectorAdded(final Effector effector);

    public void entityAdded(final OdorWorldEntity entity);

    public void sensorAdded(final Sensor sensor);

    public void entityRemoved(final OdorWorldEntity entity);

    public void sensorRemoved(final Sensor sensor);

    public void effectorRemoved(final Effector effector);
    
}
