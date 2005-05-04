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
package org.simbrain.network.pnodes;

import java.awt.Color;
import java.awt.Font;

import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.ScreenElement;

import edu.umd.cs.piccolox.nodes.PStyledText;

public class PNodeText extends PStyledText implements ScreenElement {

	public static Font textFont = new Font("Arial", Font.PLAIN, 11);
	public static int textHeight = 10;
	public static int textWidth = 10;
	SimpleAttributeSet sas = new SimpleAttributeSet();
	
	public PNodeText(String text) {
		DefaultStyledDocument d = new DefaultStyledDocument();
		try {
			d.insertString(0, text, null);
		}
		catch (Exception e) {
		}

		setPaint(Color.white);
		setDocument(d);
		setVisible(true);
	
	}

	public void drawBoundary() {
	
	}
	
	public void addToPanel(NetworkPanel np) {
		int x = (int)np.getLastClicked().getX();
		int y = (int)np.getLastClicked().getY();
		setBounds(x,y,textHeight,textWidth);
		np.addNode(this, false);
		np.getLayer().addChild(this);
	}
	
	public boolean isSelectable() {
		return true;
	}

}
