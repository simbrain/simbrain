/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
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
package org.simbrain.util;

import java.awt.event.ActionListener;

/**
 * A custom action listener which can be turned off and on. Essentially,
 * all that has been added is a boolean variable which is meant to show
 * whether or not this listener is active (true by default). When supplying
 * the method actionPerformed(...) the option is then available to use this
 * flag to define conditions under which the performed action should or 
 * should not be executed. The intention is to allow for an easy way for
 * components to make changes to other components in the UI without setting
 * off their action listener, thus starting an infinite loop. In practice
 * it allows for easy differentiating on the part of the code between user
 * induced actions and model-induced actions.
 * 
 * 
 * @author ztosi
 *
 */
public abstract class SwitchableActionListener implements ActionListener {
	
	/** 
	 * An active flag used to set conditions on when actions are
	 * performed.
	 */
	private boolean active = true;
	
	/**
	 * Sets the active flag to true.
	 */
	public void enable(){
		active  = true;
	}
	
	/**
	 * Sets the active flag to false.
	 */
	public void disable(){
		active = false;
	}  	
	
	/**
	 * @return the active flag.
	 */
	public boolean isEnabled(){
		return active;
	}
}
