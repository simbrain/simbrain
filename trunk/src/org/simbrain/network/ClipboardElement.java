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

/**
 * <b>ClipbardElement</b> represents an object in the clipboard--a neuron or set of neurons and their
 * connections to be pasted. 
 *
 * @author Mai Ngoc Thang
 */
public class ClipboardElement {

	//	 ClipboardElement contains two variables: sourceObject and clipboardObject,
	//	 which is simply a copy of the  sourceObject.  The purpose of the clipboardObject is to
	//	 provide a link from the copied object to the source object which helps to associate the
	//	 copied PNodeWeight with the new source and target PNodeNeuron.
	 	
    private Object clipboardObject = null;
    private Object sourceObject = null;

    public Object getClipboardObject(){
        return this.clipboardObject;
    }

    public Object getSourceObject(){
        return this.sourceObject;
    }

    public void setClipboardObject(Object obj){
        this.clipboardObject = obj;
    }

    public void setSourceObject(Object obj){
        this.sourceObject = obj;
    }


}
