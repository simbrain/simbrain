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
package org.simbrain.coupling;

import java.util.ArrayList;

import org.simbrain.world.Agent;


/**
 * <b>Sensory Coupling</b> represents an coupling between an agent and a network, 
 */
public class MotorCoupling extends Coupling {
	
	private ArrayList motor_id;
	
	public MotorCoupling(Agent a, ArrayList mid ) {
		super(a);
		motor_id = mid;
	}

	/**
	 * @return Returns the motor_id.
	 */
	public ArrayList getMotorId() {
		return motor_id;
	}
	/**
	 * @param motor_id The motor_id to set.
	 */
	public void setMotorId(ArrayList motor_id) {
		this.motor_id = motor_id;
	}
}
