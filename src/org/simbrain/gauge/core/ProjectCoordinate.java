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
package org.simbrain.gauge.core;

/**
 * <b>Project Coordinate</b> is perhaps the simplest possible projection algorithm; It 
 * simply takes two specificed dimensions in the high dimensional space, and uses these
 * as the basis for the low-dimensional space.  In effect it just takes a 2-dimensional 
 * subspace of the high-dimensional space.
 */
public class ProjectCoordinate extends Projector {

	public ProjectCoordinate() {
	}
	
	public ProjectCoordinate(Settings set) {
		theSettings = set;
	}
	
	/* (non-Javadoc)
	 * @see org.hisee.core.Projector#init(org.hisee.core.Dataset, org.hisee.core.Dataset)
	 */
	public void init(Dataset up, Dataset down) {
		
		super.init(up, down);
		
		if ((upstairs.getNumPoints() > 1) && (theSettings.isAutoFind()== true)) {
			theSettings.setHi_d1(upstairs.getKthVariantDimension(1));
			theSettings.setHi_d2(upstairs.getKthVariantDimension(2));			
		}	
		
		checkCoordinates();
	
	}

	/* (non-Javadoc)
	 * @see org.hisee.core.Projector#project()
	 */
	public void project() {

		if (upstairs.getNumPoints() < 1) {
			return;
		}
		
		checkCoordinates();
				
		for (int i = 0; i < upstairs.getNumPoints(); i++) {
			double[] newLowDPoint = {upstairs.getComponent(i, theSettings.getHi_d1()), upstairs.getComponent(i, theSettings.getHi_d2())};
			downstairs.setPoint(i, newLowDPoint);
		}
		//System.out.println("-->" + hi_d1);
		//System.out.println("-->" + hi_d2);
	}

	/**
	 * If the current coordinate axes are outside acceptable bounds, set them to 
	 * acceptable values (currently 0 and 1).
	 */
	public void checkCoordinates() {

		if (theSettings.getHi_d1() >= upstairs.getDimensions()) {
			theSettings.setHi_d1(0);
		}
		if (theSettings.getHi_d2() >= upstairs.getDimensions()) {
			theSettings.setHi_d2(1);
		}	
	}
	
	public boolean isExtendable() {
		return true;
	}

	public boolean isIterable() {
		return false;
	}

	public double iterate() {
		return 0;
	}

	/**
	 * @return the first coordinate projected onto
	 */
	public int getHi_d1() {
		return theSettings.getHi_d1();
	}

	/**
	 * @return the second coordinate projected onto
	 */
	public int getHi_d2() {
		return theSettings.getHi_d2();
	}

	/**
	 * @param i the first coordinate to project onto
	 */
	public void setHi_d1(int i) {
		checkCoordinates();
		theSettings.setHi_d1(i);
	}

	/**
	 * @param i the second coordinate to project onto
	 */
	public void setHi_d2(int i) {
		checkCoordinates();
		theSettings.setHi_d2(i);
	}

	/**
	 * In auto-find the projection automatically uses the most variant dimensions
	 * 
	 * @return true if in auto-find mode, false otherwise 
	 */
	public boolean isAutoFind() {
		return theSettings.isAutoFind();
	}

	/**
	 * In auto-find the projection automatically uses the most variant dimensions
	 * 
	 * @param b whether to use auto-find mode
	 */
	public void setAutoFind(boolean b) {
		theSettings.setAutoFind(b);
	}

}
