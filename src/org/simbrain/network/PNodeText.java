/*
 * Created on Aug 31, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simbrain.network;

import java.awt.Color;
import java.awt.Font;
import edu.umd.cs.piccolox.event.PStyledTextEventHandler;

import javax.swing.text.*;

import edu.umd.cs.piccolox.nodes.PStyledText;

/**
 * @author yoshimi
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
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
		int x = (int)np.selectionEventHandler.getLastLeftClicked().getX();
		int y = (int)np.selectionEventHandler.getLastLeftClicked().getY();
		setBounds(x,y,textHeight,textWidth);
		np.addNode(this, false);
		np.getLayer().addChild(this);
	}
	
	public boolean isSelectable() {
		return true;
	}

}
