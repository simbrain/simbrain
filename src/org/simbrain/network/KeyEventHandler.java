/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2003 Jeff Yoshimi <www.jeffyoshimi.net>
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
 
package org.simbrain.network;

import java.awt.event.KeyEvent;

import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;

/**
 * <b>NetworkKeyEventHandler</b> handles key events in the network panel
 * TODO: Change name to KeyEventHandler; similarly with other classes
 */
public class KeyEventHandler extends PBasicInputEventHandler {

	NetworkPanel netPanel;
	MouseEventHandler netSelect;
	
	//TODO: Move  methods from NetSelectionEventHandler here from that class, rather then forwarding them

	public KeyEventHandler(NetworkPanel np) {
		netPanel = np;
		netSelect = np.getHandle();

	}

	/**
	 * Note that some key handling is taken care if via control-characters in Swing menus
	 */
	public void keyPressed(PInputEvent e) {
		int keycode = e.getKeyCode();

		switch (keycode) {
			case KeyEvent.VK_BACK_SPACE :
			case KeyEvent.VK_DELETE :
				netPanel.deleteSelection();
				break;
			case KeyEvent.VK_C :
				netPanel.clearSelection();
				break;
			case KeyEvent.VK_X :
				if (e.isControlDown()) {
					netSelect.cutToClipboard();
				}	
				break;
			case KeyEvent.VK_V :
				netPanel.setCursorMode(NetworkPanel.NORMAL);
				break;
			case KeyEvent.VK_H :
				netPanel.setCursorMode(NetworkPanel.PAN);
				break;
			case KeyEvent.VK_Y :
				netPanel.centerCamera();
				break;
			case KeyEvent.VK_Z :
				if (netPanel.getCursorMode() == NetworkPanel.ZOOMIN)
					netPanel.setCursorMode(NetworkPanel.ZOOMOUT);
				else netPanel.setCursorMode(NetworkPanel.ZOOMIN);
				break;
	
			case KeyEvent.VK_D :	
				netPanel.debug();
				break;
			case KeyEvent.VK_I :
				netPanel.setInputs();
				break;
			case KeyEvent.VK_O :
				netPanel.setOutputs();
				break;
			case KeyEvent.VK_P :
				netPanel.addNeuron();
				break;
			case KeyEvent.VK_R :
				netPanel.randomizeSelection();
				break;
			case KeyEvent.VK_U :
				netSelect.unselectAll();
				break;
			case KeyEvent.VK_S :
				if (e.isControlDown() == false) {
					netPanel.updateNetwork();
				}
				break;
			case KeyEvent.VK_SPACE :
				netPanel.updateNetworkAndWorld();
				break;
			case KeyEvent.VK_A :
				netSelect.selectAll();
				break;	
			case KeyEvent.VK_N :
				netSelect.selectAllNeurons();
				break;
			case KeyEvent.VK_W :
				netSelect.selectAllWeights();
				break;
			case KeyEvent.VK_T :
				netPanel.addText("test");
				break;
			case KeyEvent.VK_LEFT :
				if (e.isShiftDown()) {
					netPanel.nudge(-1, 0);		
				} 
				else {
					netPanel.decrementSelectedObjects();
				}
				break;
			case KeyEvent.VK_RIGHT :
				if (e.isShiftDown()) {
					netPanel.nudge(1, 0);		
				} 
				else {
					netPanel.incrementSelectedObjects();
				}
				break;
			case KeyEvent.VK_UP:
				if (e.isShiftDown()) {
					netPanel.nudge(0, -1);		
				} 
				else {
					netPanel.incrementSelectedObjects();
				}
				break;
			case KeyEvent.VK_DOWN :
				if (e.isShiftDown()) {
					netPanel.nudge(0, 1);		
				} 
				else {
					netPanel.decrementSelectedObjects();
				}			
		}
	}

	
}
