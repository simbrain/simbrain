package org.simbrain.network;

//TODO: Change name to networkGuiElement?
public interface ScreenElement {

	public void addToPanel(NetworkPanel np);
	public void drawBoundary();
	public boolean isSelectable();
	
	//randomize, increment, decrement
	
}
