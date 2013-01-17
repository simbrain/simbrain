package org.simbrain.util;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import org.simbrain.resource.ResourceManager;

public class StopLight extends JPanel implements ActionListener {

	private ArrayList<Image> imgs = new ArrayList<Image>();
	private ImageIcon go = ResourceManager.getImageIcon("GreenCheck.png");
	private ImageIcon stop = ResourceManager.getImageIcon("RedX.png");
	private Image img;
	private boolean greenlight;
	
	public StopLight(){
		img = stop.getImage();
		imgs.add(img);
		Dimension dim = new Dimension(img.getWidth(null),
				img.getHeight(null));
		setPreferredSize(dim);
		setMinimumSize(dim);
		setMaximumSize(dim);
		setSize(dim);
		setLayout(null);
		repaint();
	}
	
	public StopLight(boolean greenlight) {
		this.greenlight = greenlight;
		if(greenlight) {
			img = go.getImage();
		} else {
			img = stop.getImage();
		}
		imgs.add(img);
		Dimension dim = new Dimension(img.getWidth(null),
				img.getHeight(null));
		setPreferredSize(dim);
		setMinimumSize(dim);
		setMaximumSize(dim);
		setSize(dim);
		setLayout(null);
		repaint();
	}
	
	public void paintComponent(Graphics g) {
		removeAll();
		super.paintComponent(g);
		g.drawImage(img,  0, 0, null);
	}
	
	public void flip(){
		if(greenlight) {
			img = null;
			repaint();
			img = stop.getImage();   			
		} else {
			img = null;
			repaint();
			img = go.getImage();			
		}
		greenlight = !greenlight;
		firePropertyChange("State", !greenlight, greenlight);
	}

	public void setState(boolean greenlight) {
		this.greenlight = greenlight;
		if(greenlight) {
			img = null;
			repaint();
			img = go.getImage();
		} else {
			img = null;
			repaint();
			img = stop.getImage();
		}
		firePropertyChange("State", !greenlight, greenlight);
	}
	
	public boolean getState(){
		return greenlight;
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		setState(true);
		repaint();		
	}
}
