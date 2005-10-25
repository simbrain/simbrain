package org.simbrain.world.visionworld;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

// Need javadoc comment here
public class Pixel extends Rectangle {
	
	Color ON_COLOR = Color.BLACK;
	Color OFF_COLOR = Color.WHITE;
	public static final boolean ON = true;
	public static final boolean OFF = false;
	private boolean state = true;
	
	/**
	* @val if true, show this pixel, else don't
	*/
	public void show(Graphics g) {
		
		if(state == ON) {
			g.setColor(ON_COLOR);
		} else if(state == OFF) {
			g.setColor(OFF_COLOR);
		}

		g.fillRect(this.x, this.y, this.width, this.height);
		
		return;
	}
	
	public void switchState() {
		state = !state;
	}

	public boolean getState() {
		return state;
	}

	public void setState(boolean state) {
		this.state = state;
	}

}
