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
package org.simbrain.world.textworld;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;

/**
 * <b>Dictionary</b> associates words with neural inputs and outputs.
 *
 */
public class Dictionary extends Hashtable {

    /** Dictionary. */
    private String [][] dictionary;
    /** World frame. */
    private TextWorldDesktopComponent worldFrame;
    /** Entries. */
    private ArrayList entries = new ArrayList();

    /**
     * Creates a new dictionary for the TextWorldComponent.
     * @param theFrame TextWorldComponent
     */
    public Dictionary(final TextWorldDesktopComponent theFrame) {
        worldFrame = theFrame;

    }
    
    // For testing right now...
    public Dictionary() {
        
    }

    public double[] getVector(String key) {
        return (double[])this.get(key);
    }
    
    public double get(String key, int index) {
        if (getVector(key) == null) {
            return 0;
        } else {
            return getVector(key)[index];            
        }
    }
    
    /**
     * Loads an existing dictioary from a file.
     */
    public void loadDictionary() {
        SFileChooser chooser = new SFileChooser(".", "Comma Separated Values", "csv"); // TODO: Use getCurrentDirectory
        File theFile = chooser.showOpenDialog();

        if (theFile == null) {
            return;
        }

        dictionary = Utils.getStringMatrix(theFile);
        worldFrame.getParentFrame().setTitle(theFile.getName());
        fillEntries();
        output();
    }

    /**
     * Outputs dictionary to console.
     */
    public void output() {
       System.out.println(this);
    }

    /**
     * Fills the dictionary entires.
     */
    private void fillEntries() {
        for (int i = 0; i < dictionary.length; i++) {
                entries.add(new Entry(dictionary[i][0], dictionary[i][1]));
        }

        //go through the 2-d dictionary,
        // entries.add(new Entry(dictionary[i][0], dictionary[i][1]));

    }

    /**
     * @return neural outputs as a string.
     */
    public String toString() {
        String ret = new String();
        for (int i = 0; i < entries.size(); i++) {
           ret += (entries.get(i) + "\n");
        }
        return ret;
    }
}
