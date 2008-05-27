package org.simbrain.world.threedee.sensors;

import java.util.ArrayList;
import java.util.Collection;

import org.simbrain.world.threedee.Agent;
import org.simbrain.world.threedee.Sensor;
import org.simbrain.world.visionworld.SensorMatrix;
import org.simbrain.world.visionworld.filter.RgbFilter;
import org.simbrain.world.visionworld.pixelmatrix.BufferedImagePixelMatrix;
import org.simbrain.world.visionworld.sensormatrix.DenseSensorMatrix;

public class Sight {
    Agent agent;
    int height;
    int width;
    volatile BufferedImagePixelMatrix image;
    SensorMatrix matrix;
    
    public Sight(Agent agent) {
        this.agent = agent;
        
        width = agent.getWidth();
        height = agent.getHeight();
        
        int rWidth = width / 5;
        int rHeight = height / 5;
        
        matrix = new DenseSensorMatrix(5, 5, 
            rWidth, rHeight, new RgbFilter(-50, 200, -50));
    }
     
    
    public Collection<Sensor> getProducingAttributes() {
        Collection<Sensor> attributes = new ArrayList<Sensor>();
        
        int width = agent.getWidth();
        int height = agent.getHeight();
        
        if (this.height != height || this.width != width) {
            this.height = height;
            this.width = width;
            
            int rWidth = width / 5;
            int rHeight = height / 5;
            
            matrix = new DenseSensorMatrix(5, 5, 
                rWidth, rHeight, new RgbFilter(-50, 200, -50));
            
            new Thread(new Runnable() {
                public void run() {
                    image = new BufferedImagePixelMatrix(agent.getSnapshot());
                }
            }).start();
        }
        
        for (int column = 0; column < matrix.columns(); column++) {
            for (int row = 0; row < matrix.rows(); row++) {
                attributes.add(new SightSensor(matrix.getSensor(row, column)));
            }
        }
        
        return attributes;
    }
    
    public void update() {
        image = new BufferedImagePixelMatrix(agent.getSnapshot());
    }
    
    class SightSensor implements Sensor {
        private org.simbrain.world.visionworld.Sensor sensor;
        
        SightSensor(org.simbrain.world.visionworld.Sensor sensor) {
            this.sensor = sensor;
        }
        
        public String getDescription() {
            return sensor.getDescription();
        }

        public Double getValue() {
            return sensor.sample(image);
        }
    }
}
