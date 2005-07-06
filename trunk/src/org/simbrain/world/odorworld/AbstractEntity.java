package org.simbrain.world.odorworld;

import java.awt.Graphics;
import java.awt.Rectangle;


public abstract class AbstractEntity {
	

	public abstract int getX();
	
	public abstract int getY();
	
	public abstract void setX(int x);
	
	public abstract void setY(int y);
	
	public abstract int getWidth();
	
	public abstract int getHeight();
	
	public abstract Rectangle getRectangle();
	
	public abstract OdorWorld getParent();
	
	public abstract void setParent(OdorWorld world);
	
	public abstract void paintThis(Graphics g);
}
