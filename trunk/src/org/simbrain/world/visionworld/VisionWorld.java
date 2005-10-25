package org.simbrain.world.visionworld;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JPanel;

import org.simbrain.coupling.CouplingMenuItem;
import org.simbrain.coupling.SensoryCoupling;
import org.simbrain.network.NetworkPanel;
import org.simbrain.world.Agent;
import org.simbrain.world.World;

/**
 * <b>VisionWorld</b> provides visual input to a neural network in the form of a grid of pixels that can be 
 * turned on or off by the user.
 *
 * @author RJB
 *
 */
public class VisionWorld extends JPanel implements World, Agent, MouseListener, MouseMotionListener,ComponentListener {

	private int numPixelsRow = 10;
	private int numPixelsColumn = 10;
	private int pixelSize = 10;
	private ArrayList commandTargets = new ArrayList();
	
	private Pixel[][] pixels = new Pixel[numPixelsColumn][numPixelsRow];
	private VisionWorldFrame parentFrame;
	private String name;
	private boolean mouseInTheHouse = false;
	private boolean currentState = true; // Used when dragging the mouse, so that dragging does not toggle
	
	
	// Stub constructor 
	public VisionWorld(){
		redimension(numPixelsRow,numPixelsColumn);
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addComponentListener(this);
	}
	
	// Construct world with specific numPixelsColumn and numPixelsRow
	public VisionWorld(int w, int h) {
		redimension(w, h);
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addComponentListener(this);
	}

	public VisionWorld(VisionWorldFrame pf){
		redimension(numPixelsRow,numPixelsColumn);
		parentFrame = pf;
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addComponentListener(this);
	}


	/**
	 *  Changes the dimensions of the world
	 * @param h numPixelsColumn in pixels
	 * @param w numPixelsRow in pixels
	 */
	public void redimension(int w,int h){
		numPixelsRow = w;
		numPixelsColumn = h;
		pixels = new Pixel[numPixelsRow][numPixelsColumn];
		for(int i=0;i<numPixelsRow;i++){
			for(int j=0;j<numPixelsColumn;j++){
				pixels[i][j] = new Pixel();
			}
		}
	}
	
	
	public Pixel getPixel(int x, int y) {
	
		return pixels[x][y];
	
	}
	
	
	public Pixel getSelectedPixel(MouseEvent e) {
		if(pixelSize!=0)
			if(e.getX()/pixelSize < numPixelsRow && e.getY()/pixelSize < numPixelsColumn)
				return getPixel(e.getX()/pixelSize,e.getY()/pixelSize);
		return null;
	}
	
	public String getSelectedPixelToolTip(MouseEvent e) {
		if(pixelSize!=0)
			if(e.getX()/pixelSize<numPixelsRow&&e.getY()/pixelSize<numPixelsColumn)
				return ""+(e.getX()/pixelSize+1)+","+(e.getY()/pixelSize+1);
		return null;
	}
	
	public void paint(Graphics g){
		this.setBackground(Color.RED);
		super.paint(g);
		
		for(int i=0;i<pixels.length;i++){
			for(int j=0;j<pixels[i].length;j++){
				pixels[i][j].show(g);
			}
		}
	}
	
	
	public void rebuild(){
		pixelSize = this.getWidth()/numPixelsRow;
		for(int i=0;i<pixels.length;i++){
			for(int j=0;j<pixels[i].length;j++){
				pixels[i][j].setSize(new Dimension(pixelSize, pixelSize));
				pixels[i][j].setLocation(i*pixelSize,j*pixelSize);
			}
		}
		this.setPreferredSize(new Dimension(numPixelsColumn * pixelSize, numPixelsRow * pixelSize));
	}
	
	public World getParentWorld() {
		return this;
	}

	/**
	 * Accepts a string in the form "x","y"
	 */
	public double getStimulus(String[] sensor_id) {
		boolean state = getPixel(Integer.parseInt(sensor_id[0]),Integer.parseInt(sensor_id[1])).getState();
		if (state == Pixel.OFF)
			return 0;
		else {
			return 1;
		}
	}

	
	public void setMotorCommand(String[] commandList, double value) {
		//TODO Auto-generated method stub
	}

	public String getType() {
		return "VisionWorld";
	}

	public ArrayList getAgentList() {
		ArrayList ret = new ArrayList();
		ret.add(this);
		return ret;
	}

	public JMenu getMotorCommandMenu(ActionListener al) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public JMenu getSensorIdMenu(ActionListener al) {
		JMenu ret = new JMenu(this.getName());
		for(int i=1;i<pixels.length;i++){
			JMenu row = new JMenu("Row "+i);
			for(int j=1;j<pixels[i].length;j++){
				CouplingMenuItem cmi = new CouplingMenuItem("Column "+j,new SensoryCoupling(this,new String[]{""+(i-1),""+(j-1)}));
				cmi.addActionListener(al);
				row.add(cmi);
			}
			ret.add(row);
		}
		
		return ret;
	}
	

	public void addCommandTarget(NetworkPanel net) {
		if(commandTargets.contains(net) == false) {
			commandTargets.add(net);
		}
	}

	public void removeCommandTarget(NetworkPanel net) {
		commandTargets.remove(net);
	}

	public ArrayList getCommandTargets() {
		return commandTargets;
	}

	public VisionWorldFrame getParentFrame() {
		return parentFrame;
	}

	public void setParentFrame(VisionWorldFrame parentFrame) {
		this.parentFrame = parentFrame;
	}
	
	public void setName(String name){
		this.getParentFrame().setTitle(name);
		this.name = name;
	}
	
	public String getName(){
		return this.name;
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		Pixel p = getSelectedPixel(e);
		if(p !=null) {
			p.switchState();	
			currentState = p.getState();
		}
		repaint();
	}
	
	public void mouseDragged(MouseEvent e) {
		Pixel p = getSelectedPixel(e);
		if(p != null) {
			p.setState(currentState);
			repaint();
		}			
	}
	public void mouseReleased(MouseEvent arg0) {
	}

	public void mouseEntered(MouseEvent arg0) {
		this.mouseInTheHouse = true;
	}
 
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void componentResized(ComponentEvent arg0) {
	}

	public void componentMoved(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void componentShown(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void componentHidden(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void mouseMoved(MouseEvent arg0) {
		if(this.mouseInTheHouse){
			this.setToolTipText(this.getSelectedPixelToolTip(arg0));
		}
	}

}
