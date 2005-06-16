package org.simbrain.world;

import java.awt.Point;

import javax.swing.JPanel;

public class Wall extends JPanel {
	
	private int upperLeftX;
	private int upperLeftY;
	private int width;
	private int height;
	
	/**
	 * @return Returns the height.
	 */
	public int getHeight() {
		return height;
	}
	/**
	 * @param height The height to set.
	 */
	public void setHeight(int height) {
		this.height = height;
	}
	/**
	 * @return Returns the upperLeftX.
	 */
	public int getUpperLeftX() {
		return upperLeftX;
	}
	/**
	 * @param upperLeftX The upperLeftX to set.
	 */
	public void setUpperLeftX(int upperLeftX) {
		this.upperLeftX = upperLeftX;
	}
	/**
	 * @return Returns the upperLeftY.
	 */
	public int getUpperLeftY() {
		return upperLeftY;
	}
	/**
	 * @param upperLeftY The upperLeftY to set.
	 */
	public void setUpperLeftY(int upperLeftY) {
		this.upperLeftY = upperLeftY;
	}
	/**
	 * @return Returns the width.
	 */
	public int getWidth() {
		return width;
	}
	/**
	 * @param width The width to set.
	 */
	public void setWidth(int width) {
		this.width = width;
	}
	public Wall(){
		
	}
	

}
