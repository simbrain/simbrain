/*
 * Part of HiSee, a tool for visualizing high dimensional datasets
 * 
 * Copyright (C) 2004 Scott Hotton <http://www.math.smith.edu/~zeno/> and 
 * Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.gauge.graphics;

import edu.umd.cs.piccolo.nodes.PPath;

import java.awt.*;
import java.awt.geom.*;


/**
* <b>PNodeDatapoint</b> is a Piccolo PNode representing a (projected) point in the dataset  
*/
public class PNodeDatapoint extends PPath {
   
    private int index = 0;
    
	//Currently only handles 2-d points
    public PNodeDatapoint(double[] point, int i) {

		super(new Ellipse2D.Float((float) point[0], (float) -point[1], (float)1, (float)1), null);
		index = i;
		this.setPaint(Color.green);
    }
    
    public  void setColor(Color c) {
    	this.setPaint(c);
    }

	public  void setSize(double s) {
		this.setBounds(0,0,s,s);
	}

	public double getGlobalX() {
		Point2D p = new Point2D.Double(getX(), getY());
		return localToGlobal(p).getX();
	}
	
	public double getGlobalY() {
		Point2D p = new Point2D.Double(getX(), getY());
		return localToGlobal(p).getY();
	}
	
	/**
	 * @return index of this datapoint in the associated dataset
	 */
	public int getIndex() {
		return index;
	}


}
