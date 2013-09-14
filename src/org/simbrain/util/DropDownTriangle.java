package org.simbrain.util;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import org.simbrain.resource.ResourceManager;

public class DropDownTriangle extends JPanel implements MouseListener {

	public static final short LEFT = -1;
	
	public static final short DOWN = 0;
	
	public static final short RIGHT = 1;
	
	private boolean down;
	
	private ImageIcon downTriangle = ResourceManager
			.getImageIcon("DownTriangle.png");
	
	private ImageIcon leftTriangle = ResourceManager
			.getImageIcon("LeftTriangle.png");
	
	private ImageIcon rightTriangle = ResourceManager
			.getImageIcon("RightTriangle.png");
	
	private ImageIcon upTriangle;
	
	private Image triangle;
	
	public DropDownTriangle(short upState, boolean down) {
		
		switch(upState) {
			case LEFT : upTriangle = leftTriangle; break;
			case RIGHT : upTriangle = rightTriangle; break;
			default : throw new IllegalArgumentException("Invalid starting" +
					" state. Valid starting states are: Left (-1)" +
					" or Right(1)");
		}
		
		this.down = down;	
		triangle = down ? downTriangle.getImage() : upTriangle.getImage();	
		addMouseListener(this);
		setSize();
		setLayout(null);	
		repaint();
		
	}
	
	private void setSize() {
		Dimension size = new Dimension((int) triangle.getWidth(null),
				(int) triangle.getHeight(null));
		setPreferredSize(size);
		setMaximumSize(size);
		setMinimumSize(size);
		setSize(size);
	}
	
	
	public void changeState() {
		down = !down;
		
		triangle = down ? downTriangle.getImage() : upTriangle.getImage();
		
		repaint();
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void paintComponent(Graphics g) {
		removeAll();
		super.paintComponent(g);
		setSize();
		g.drawImage(triangle, 0, 0, null);
	}
	
	@Override
	public void mouseClicked(MouseEvent arg0) {
		
		changeState();
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public boolean isDown() {
		return down;
	}	
	
}
