package org.simbrain.world.threedee;

import com.jme.math.Vector3f;

public interface Collision {
   /**
    * retrieves the other element involved
    * in this collision
    * 
    * @return the other element in the collision
    */
   Element other();
   
   /**
    * a vector from the center point of the 
    * element identifying the point of contact
    * @return
    */
   Vector3f point();
}
