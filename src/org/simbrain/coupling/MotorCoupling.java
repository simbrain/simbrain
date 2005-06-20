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

import org.simbrain.network.pnodes.PNodeNeuron;
import org.simbrain.world.Agent;


/**
 * <b>Motor Coupling</b> represents a coupling between the output node of a neural network
 * and some form of agent movement.  E.g. a motor neuron and a form of behavior.
 */
public class MotorCoupling extends Coupling {
	
	private String[] commandArray;

	
	public MotorCoupling() {
	}
	
	public MotorCoupling(Agent a, String[] ca ) {
		super(a);
		commandArray = ca;
	}
	
	public MotorCoupling(Agent a, PNodeNeuron n, String[] ca ) {
		super(a, n);
		commandArray = ca;
	}
	public MotorCoupling(PNodeNeuron n, String[] ca ) {
		super(n);
		commandArray = ca;
	}

	/**
	 * @return Returns the motor_id.
	 */
	public String[] getCommandArray() {
		return commandArray;
	}
	/**
	 * @param motor_id The motor_id to set.
	 */
	public void setCommandArray(String[] ca) {
		this.commandArray = ca;
	}

}
