package org.simbrain.world.threedee.sensors;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.simbrain.workspace.Workspace;
import org.simbrain.world.threedee.Sensor;
import org.simbrain.world.threedee.gui.AgentView;
import org.simbrain.world.visionworld.MutableVisionWorldModel;
import org.simbrain.world.visionworld.PixelMatrix;
import org.simbrain.world.visionworld.SensorMatrix;
import org.simbrain.world.visionworld.VisionWorldComponent;
import org.simbrain.world.visionworld.VisionWorldModel;
//import org.simbrain.world.visionworld.dialog.AbstractSensorMatrixDialog;
import org.simbrain.world.visionworld.pixelmatrix.BufferedImagePixelMatrix;

public class Sight {
    final AgentView agent;
    int height;
    int width;
    volatile BufferedImagePixelMatrix image;
    SensorMatrix matrix;
    List<WeakReference<VisionWorldComponent>> components = new ArrayList<WeakReference<VisionWorldComponent>>();
    final Workspace workspace;
    
    public Sight(final AgentView agent, final String name, final Workspace workspace) {
        this.agent = agent;
        this.workspace = workspace;
        
//        width = agent.getWidth();
//        height = agent.getHeight();
        
//        int rWidth = width / 5;
//        int rHeight = height / 5;
        image = new BufferedImagePixelMatrix(agent.getSnapshot());
        
        createVisionWorld();
    }
    
    public void createVisionWorld() {
        final Semaphore semaphore = new Semaphore(1);
        
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        /*
          TODO:  restore visionworld integration here
        AbstractSensorMatrixDialog dialog = new AbstractSensorMatrixDialog() {
            private static final long serialVersionUID = 1L;

            @Override
            protected PixelMatrix getPixelMatrix() {
                return image;// = new BufferedImagePixelMatrix(agent.getSnapshot());
            }

            @Override
            protected void ok(SensorMatrix sensorMatrix) {
                matrix = sensorMatrix;
                VisionWorldModel model = new MutableVisionWorldModel(image, matrix);
                VisionWorldComponent component = new VisionWorldComponent(getName() + " vision", model);
                
                components.add(new WeakReference<VisionWorldComponent>(component));
                
                workspace.addWorkspaceComponent(component);
                semaphore.release();
            }
        };
        
        dialog.init();
        dialog.setBounds(100, 100, 450, 550);
        dialog.setVisible(true);
        */
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            
        }
    }
    
    public void close() {
        for (WeakReference<VisionWorldComponent> ref : components) {
            VisionWorldComponent component = ref.get();
            
            if (component != null) component.close();
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
            
            if (image == null) {
                new Thread(new Runnable() {
                    public void run() {
                        image = new BufferedImagePixelMatrix(agent.getSnapshot());
                    }
                }).start();
            }
        }
        
        for (int column = 0; column < matrix.columns(); column++) {
            for (int row = 0; row < matrix.rows(); row++) {
                attributes.add(new SightSensor(matrix.getSensor(row, column)));
            }
        }
        
        return attributes;
    }
    
    volatile Runnable next;
    
    public void update() {
//        image = new BufferedImagePixelMatrix(agent.getSnapshot());
//        System.out.println("update");
        
        if (next == null) {
            next = new Runnable() {
                public void run() {
                    image.setImage(agent.getSnapshot());
                    next = null;
                }
            };
            
            new Thread(next).start();
        }
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
            return new Double(0);
            //return sensor.sample(image);
        }
    }
}
