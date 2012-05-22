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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.Utils;

/**
 * An action  that opens a help file in an external web browser.
 */
public final class ShowHelpAction extends AbstractAction {

    /** Documentation URL. */
    private final String theURL;

    //TODO: Construct with URL; throw exceptions for bad pages

    /**
     * Create a help action that opens the specified URL (relative to Simbrain/docs).
     *
     * @param actionName the name associated with this action
     * @param url the url to open.
     */
    public ShowHelpAction(final String actionName, final String url) {
        super(actionName);
    	this.theURL = url;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Help.png"));
    }

    /**
     * Create a help action that opens the specified URL (relative to the "Pages"
     * directory in Simbrain/docs).
     *
     * @param url the url to open.
     */
    public ShowHelpAction(final String url) {
        super("Help");
    	this.theURL = url;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Help.png"));
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
    	
        SwingUtilities.invokeLater(new Runnable() {
                /** @see Runnable */
                public void run() {
                    Utils.showHelpPage(theURL);
                }
            });
    }


}