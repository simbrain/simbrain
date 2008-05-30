package org.simbrain.world.threedee.sensors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Semaphore;

import org.simbrain.world.threedee.Agent;
import org.simbrain.world.threedee.Sensor;
import org.simbrain.world.visionworld.PixelMatrix;
import org.simbrain.world.visionworld.SensorMatrix;
import org.simbrain.world.visionworld.dialog.AbtractSensorMatrixDialog;
import org.simbrain.world.visionworld.pixelmatrix.BufferedImagePixelMatrix;

public class Sight {
    Agent agent;
    int height;
    int width;
    volatile BufferedImagePixelMatrix image;
    SensorMatrix matrix;
    
    public Sight(final Agent agent) {
        this.agent = agent;
        
        width = agent.getWidth();
        height = agent.getHeight();
        
//        int rWidth = width / 5;
//        int rHeight = height / 5;
        
        final Semaphore semaphore = new Semaphore(1);
        
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        AbtractSensorMatrixDialog dialog = new AbtractSensorMatrixDialog() {
            /** Serial Version ID */
            private static final long serialVersionUID = 1L;

            @Override
            protected PixelMatrix getPixelMatrix() {
                return new BufferedImagePixelMatrix(agent.getSnapshot());
            }

            @Override
            protected void ok(SensorMatrix sensorMatrix) {
                matrix = sensorMatrix;
                semaphore.release();
            }
        };
        
        dialog.init();
        dialog.setBounds(100, 100, 450, 550);
        dialog.setVisible(true);
        
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            
        }
    }
    
    public Collection<Sensor> getProducingAttributes() {
        Collection<Sensor> attributes = new ArrayList<Sensor>();
        
        int width = agent.getWidth();
        int height = agent.getHeight();
        
        if (this.height != height || this.width != width) {
            this.height = height;
            this.width = width;
            
//            int rWidth = width / 5;
//            int rHeight = height / 5;
            
//            matrix = new DenseSensorMatrix(5, 5, 
//                rWidth, rHeight, new RgbFilter(-50, 200, -50));
            
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
