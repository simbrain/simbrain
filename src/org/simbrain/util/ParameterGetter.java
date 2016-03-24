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

import org.simbrain.network.gui.NetworkUtils;

/**
 * Functional interface for lambdas used primarily in checking consistency 
 * of an objects parameter via {@linkplain NetworkUtils}.
 *
 * @param <O> the type parameter of the object from which a parameter will
 * be retrieved
 * @param <V> the type of the retrieved parameter
 *  
 * @author Zach Tosi
 */
public interface ParameterGetter <O, V> {
	
	/**
	 * A generic method set up with the intention of allowing programmers
	 * to create a simple function which retrieves an arbitrary parameter
	 * of an arbitrary type of object.
	 *
	 * @param source the object returning a value
	 * @return the value on the source object 
	 */
	V getParameter(O source);
	
}
