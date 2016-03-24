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


/**
 * Functional interface for lambdas used to set values.
 *
 * @param <O> the type parameter of the object from which a parameter will
 * be retrieved
 * @param <V> the type of the set parameter
 *
 */
public interface ParameterSetter <O, V> {
	
    /**
     * Set an arbitrary parameter on an arbitrary type of object.
     *
     * @param source the object from which it is intended that one of
     * its parameters will be returned
     * @param value the value on the source object 
     */
	void setParameter(O source, V value);

}
