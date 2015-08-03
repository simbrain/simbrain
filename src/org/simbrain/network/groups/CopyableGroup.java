/*
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
package org.simbrain.network.groups;

import org.simbrain.network.core.Network;

/**
 * 
 * An interface so that subclasses of groups can be copied accurately.
 * 
 * @author Zach Tosi
 *
 * @param <T>
 */
public interface CopyableGroup<T extends Group> {

	/**
     * Returns a deep copy of a copyable group using the same root network as
     * the original.
     * 
     * @param parentNetwork the parent network for this group, potentially
     *            different from the original (used when copying and pasting
     *            from one network to another)
     * @return a deep copy of a group
     */
	T deepCopy(Network parentNetwork);
	
}
