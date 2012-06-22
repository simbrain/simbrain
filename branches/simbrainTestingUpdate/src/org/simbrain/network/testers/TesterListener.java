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
package org.simbrain.network.testers;


/**
 * Classes with implement this interface fire events indicating changes in the
 * status of a trainer.
 *
 * @author jyoshimi
 * @author ztosi 
 */
public interface TesterListener {

	 /**
     * Called when testing begins. Useful for forms of testing that take a
     * while, where indication of progress is useful.
     */
    public void beginTesting();

    /**
     * Called when testing ends. Useful for forms of testing that take a
     * while, where indication of progress is useful.
     */
    public void endTesting();

    /**
     * Called for updates on the progress of a tester. Used currently for
     * progress bars.
     *
     * @param progressUpdate a string message about the current progress on an
     *            update.
     * @param percentComplete how far along an operation is.
     */
    public void progressUpdated(String progressUpdate, int percentComplete);
	
}
