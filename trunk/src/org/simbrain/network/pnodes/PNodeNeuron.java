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
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;

import org.simbrain.network.*;
import org.simbrain.world.Agent;
import org.simbrain.coupling.*;
import org.simbrain.gauge.GaugeSource;

import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.SpikingNeuron;
import org.simnet.neurons.StandardNeuron;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PPaintContext;

/**
 * <b>PNodeNeuron</b> is a Piccolo PNode corresponding to a Neuron in the 
 * neural network model.  It is a GUI representation of a connection node
 * 
 * @author Mai Ngoc Thang
 */
public class PNodeNeuron extends PPath implements GaugeSource, ScreenElement {

	// The neural network neuron this PNode represents
	private Neuron neuron;
	
	public NetworkPanel parentPanel;

	private String id = null;
	private SensoryCoupling sensoryCoupling;
	private MotorCoupling motorCoupling;
	
	private static float hotColor = NetworkPreferences.getHotColor();
	private static float coolColor = NetworkPreferences.getCoolColor();
	public static double neuronScale = 1;
	public static int neuronSize = 24;
	
	public static final int ARROW_LINE = 20; //length of arrow
	
	//Not settable	
	public static final Font NEURON_FONT = new Font("Arial", Font.PLAIN, 11);
	public static final Font NEURON_FONT_BOLD = new Font("Arial", Font.BOLD, 11);
	public static final Font NEURON_FONT_SMALL = new Font("Arial", Font.PLAIN, 9);	
	public static final Font NEURON_FONT_VERYSMALL = new Font("Arial", Font.PLAIN, 7);		
	public static final Font IN_OUT_FONT = new Font("Arial", Font.PLAIN, 9);

	
	public static final int NEURON_HALF = (neuronSize / 2);
	public static final int NEURON_QUARTER = NEURON_HALF / 2;

	private boolean selected = false;

	private PText text;
	private PText in_label = new PText();
	private PText out_label = new PText();
	private PPath inArrow;
	private PPath outArrow;
	private double xpos, ypos;
	
	public PNodeNeuron() {
	}
	
	public PNodeNeuron(double x, double y) {			
		super(
				new Ellipse2D.Float(
					(float) x,
					(float) y,
					neuronSize,
					neuronSize));
	}
	
	/**
	 * Creates a new neuron PNode at the specified point on screen.
	 * 
	 * @param thePoint the point where the new neuron node should be placed
	 * @param np reference to the parent network panel
	 */
	public PNodeNeuron(Point2D thePoint, NetworkPanel np) {
		super(
			new Ellipse2D.Float(
				(float) thePoint.getX(),
				(float) thePoint.getY(),
				neuronSize,
				neuronSize));
		parentPanel = np;
		neuron = new StandardNeuron();
		//neuron = new Neuron("n" + parentPanel.getNetwork().getNeuronCount()+1);
		init();
	}

	/**
	 * Creates a new neuron using the default simnet.neuron constructor 
	 * 
	 * @param x x location of new neuron
	 * @param y y location of new neuron
	 * @see org.simbrain.simnet.Neuron
	 */
	public PNodeNeuron(double x, double y, NetworkPanel np) {
		super(
			new Ellipse2D.Float(
				(float) x,
				(float) y,
				neuronSize,
				neuronSize));
		parentPanel = np;
		neuron = new StandardNeuron();
		init();
	}

	/**
	 * Creates a PNodeNeuron representing an already constructed Neuron
	 * 
	 * @param x x location of neuron
	 * @param y y location of neuron
	 * @param n reference to the logical neuron this PNodeNeuron represents
	 * @param np reference to the parent network panel
	 * @see org.simbrain.simnet.Neuron
	 */
	public PNodeNeuron(double x, double y, Neuron n, NetworkPanel np) {
		super(
			new Ellipse2D.Float(
				(float) x,
				(float) y,
				neuronSize,
				neuronSize));
		parentPanel = np;		
		neuron = n;
		init();
	}

	/**
	 * Static factory method used in lieu of clone, which creates duplicate PNodeNeurons.
	 * Used, for example, in copy/paste.
	 * 
	 * @param toCopy PNodeNeuron to duplicate
	 * @param np reference to the parent network panel
	 * @return duplicate PNodeNeuron
	 */
	public static PNodeNeuron getDuplicate(PNodeNeuron toCopy, NetworkPanel np) {
		PNodeNeuron ret = new PNodeNeuron(NetworkPanel.getGlobalX(toCopy), NetworkPanel.getGlobalY(toCopy), np);
		ret.setNeuron(toCopy.getNeuron().duplicate());
		ret.setMotorCoupling(toCopy.getMotorCoupling());
		ret.setSensoryCoupling(toCopy.getSensoryCoupling());
		return ret;		
	}
	
	/**
	 * Initializes the PNodeNeuron by setting basic Piccolo parameters and initializing 
	 * children nodes.
	 */
	public void init() {	
		createText();
		this.setChildrenPickable(false);
		this.setStrokePaint(PNodeLine.getLineColor());
		this.inArrow = new PPath();
		this.outArrow = new PPath();
		this.inArrow.setStrokePaint(PNodeLine.getLineColor());
		this.outArrow.setStrokePaint(PNodeLine.getLineColor());
		this.addChild(inArrow);
		this.addChild(outArrow);
		this.addPropertyChangeListener(parentPanel);	
		
		in_label.setFont(IN_OUT_FONT);
		in_label.setPaint(PNodeLine.getLineColor());
		in_label.translate(xpos, ypos + NEURON_HALF + ARROW_LINE + 15);
		this.addChild(in_label);
		
		out_label.setFont(IN_OUT_FONT);
		out_label.setPaint(PNodeLine.getLineColor());
		out_label.translate(xpos, ypos - NEURON_HALF - ARROW_LINE - 5);
		this.addChild(out_label);

		
		
	}

	/**
	 * Set basic position of text int the PNode, which is then adjusted depending on the size of the text
	 */
	private void setPosition() {
		if (text == null) return;
		xpos = NetworkPanel.getGlobalX(this) - NetworkPanel.getGlobalX(text);
		ypos = NetworkPanel.getGlobalY(this) - NetworkPanel.getGlobalY(text);
		text.translate(xpos + NEURON_QUARTER + 2, ypos + NEURON_QUARTER + 1);
		
	}
	
	/**
	 * Creates the PText object that represents the activation level of the neuron
	 */
	private void createText() {

		text = new PText(String.valueOf((int) Math.round(neuron.getActivation())));
		text.setFont(NEURON_FONT);
		setPosition();		
		
		this.addChild(text);

	}
	
	/**
	 * Update the label showing sensory coupling information
	 */
	public void updateInputLabel(){
		if(parentPanel.getInOutMode() == true) {
			if(isInput()){
				in_label.setText(this.getSensoryCoupling().getShortLabel());
				in_label.setVisible(true);
			} else {
				in_label.setVisible(false);			
			}			
		} else {
			in_label.setVisible(false);
		}
	}
	
	/**
	 * Change the color of input and output nodes to reflect
	 * whether they are 'attached' to an agent in a world
	 */
	public void updateAttachmentStatus() {
		if (sensoryCoupling != null) {
			if (sensoryCoupling.isAttached() == true) {
				inArrow.setStrokePaint(PNodeLine.getLineColor());
			} else {
				inArrow.setStrokePaint(Color.GRAY);
			}			
		}
		if (motorCoupling != null) {
			if (motorCoupling.isAttached() == true) {
				outArrow.setStrokePaint(PNodeLine.getLineColor());
			} else {
				outArrow.setStrokePaint(Color.GRAY);
			}	
		}
	}
	
	/**
	 * Update the label showing motor coupling information
	 */
	public void updateOutputLabel(){
		if(parentPanel.getInOutMode() == true) {
			if(isOutput()){
				out_label.setText(this.getMotorCoupling().getShortLabel());
				out_label.setVisible(true);
			} else {
				out_label.setVisible(false);			
			}
		} else {
			out_label.setVisible(false);
		}
	}
	
	/**
	 * Determine what color and and font to use for this neuron based in its activation level
	 */
	private void updateText() {

		double act = neuron.getActivation();
		setPosition();
		text.setScale(1);

		//TODO: Use text.scale() to continuously transform the text size
		
		// 0 (or close to it) is a special case--a black font
		if (act > -.1 && act < .1 ) {
			//text.setPaint(Color.black);
			text.setFont(NEURON_FONT);
			text.setText("0");
		// In all other cases the background color of the neuron is white
		// Between 0 and 1
		} else if ((act > 0) && (neuron.getActivation() < 1)) {
			//text.setPaint(Color.white);
			text.setFont(NEURON_FONT_BOLD);
			text.setText(
				String.valueOf(act).substring(1, 3));
		} // Between 0 and -.1
		else if ((act < 0) && (act > -1)) {
			//text.setPaint(Color.white);
			text.setFont(NEURON_FONT_BOLD);
			text.setText("-" + String.valueOf(act).substring(2, 4));
		} 
     	else // greater than 1 or less than -1
     	{	
			//text.setPaint(Color.white);
			text.setFont(NEURON_FONT_BOLD);
			if (Math.abs(act) < 10 ) {
				text.scale(.9);
			} else if (Math.abs(act) < 100) {
				text.scale(.8);
				text.translate(1, 1);
			} else {
				text.scale(.7);
				text.translate(-1, 2);
			}			
			text.setText(String.valueOf((int) Math.round(act)));
		}
		
	}

	/**
	 * Sets the color of this neuron based on its activation level
	 */
	private void applyColor() {

		double activation = neuron.getActivation();

		//Force to blank if 0
		if (activation > -.1 && activation < .1) {
			this.setPaint(Color.white);
		}					
		else if (activation > 0) {
			float saturation = checkValid((float)Math.abs(activation/neuron.getUpperBound()));
			this.setPaint(Color.getHSBColor((float)hotColor, saturation, (float)1));
		} else if (activation < 0) {
			float saturation = checkValid((float)Math.abs(activation/neuron.getLowerBound()));
			this.setPaint(Color.getHSBColor((float)coolColor, saturation, (float)1));
		}
		
		if (this.isSelected() == true) {
			this.setPaint(SelectionHandle.getSelectionColor());
		}
	
		
		if(neuron instanceof SpikingNeuron) {
			if (((SpikingNeuron)neuron).hasSpiked()) {
				this.setStrokePaint(Color.YELLOW);
				outArrow.setStrokePaint(Color.YELLOW);
			} else {
				this.setStrokePaint(PNodeLine.getLineColor());
				outArrow.setStrokePaint(PNodeLine.getLineColor());
			}
		}

	}
	
	private static float checkValid(float val) {
		if(val > 1) {
			val = 1;
		} 
		if(val < 0) {
			 val = 0; 
		 } 
		return val; 
	}

	/**
	 * Updates the color and text of this PNodeNeuron.  Called whenever the state of the neuron
	 * changes so that the graphical representation of the neuron is up to date.
	 */
	public void render() {
		updateAttachmentStatus();
		this.applyColor();
		this.updateText();
		updateInputLabel();
		updateOutputLabel();
	}

	protected void paint(PPaintContext paintContext) {
		super.paint(paintContext);
	}

	/**
	 * Returns a reference to the logical neuron this PNodeNeuron represents
	 * 
	 * @return the neuron this PNodeNeuron represents
	 */
	public Neuron getNeuron() {
		return neuron;
	}

	/**
	 * Creates an arrow which designates an on-screen neuron as an input node,
	 * which receives signals from an external environment (the world object)
	 * 
	 * @return an object representing the input arrow of a PNodeNeuron
	 * @see org.simbrain.sim.world
	 */
	private GeneralPath createInArrow() {
		GeneralPath arrow = new GeneralPath();
		float cx = (float) getX() + NEURON_HALF;
		float cy = (float) getY() + NEURON_HALF;
		float top = cy + NEURON_HALF + 1;
		arrow.moveTo(cx, top);
		arrow.lineTo(cx,  top + ARROW_LINE);

		arrow.moveTo(cx, top);
		arrow.lineTo(cx - NEURON_QUARTER, cy + NEURON_HALF + NEURON_QUARTER);

		arrow.moveTo(cx, top);
		arrow.lineTo(cx + NEURON_QUARTER, top + NEURON_QUARTER);
		return arrow;
	}

	/**
	 * Creates an arrow which designates an on-screen neuron as an output node,
	 * which sends signals to an external environment (the world object)
	 * 
	 * @return an object representing the input arrow of a PNodeNeuron
	 * @see org.simbrain.sim.world
	 */
	private GeneralPath createOutArrow() {

		GeneralPath arrow = new GeneralPath();
		float cx = (float) getX() + NEURON_HALF;
		float cy = (float) getY() + NEURON_HALF;
		arrow.moveTo(cx, cy - NEURON_HALF);
		arrow.lineTo(cx, cy - NEURON_HALF - ARROW_LINE);

		arrow.moveTo(cx, cy - NEURON_HALF - ARROW_LINE);
		arrow.lineTo(cx - NEURON_QUARTER, cy - neuronSize);

		arrow.moveTo(cx, cy - NEURON_HALF - ARROW_LINE);
		arrow.lineTo(cx + NEURON_QUARTER, cy - neuronSize);
		return arrow;

	}

	public void resetLineColors() {
		this.setStrokePaint(PNodeLine.getLineColor());
		inArrow.setStrokePaint(PNodeLine.getLineColor());
		outArrow.setStrokePaint(PNodeLine.getLineColor());
	}
	
	/**
	 * @return true if this is PNode represents an input neuron
	 */
	public boolean isInput() {
		if (sensoryCoupling == null) {
			return false;
		} else {
			return true;
		}
	}

	public void showInOut(boolean b) {
		if (b == true) {
			if(isInput()) {in_label.setVisible(true);} 
			if(isOutput()) {out_label.setVisible(true);} 
		} else {
			if(isInput()){in_label.setVisible(false);}
			if(isOutput()){out_label.setVisible(false);}
		}
		
	}
	
	/**
	 * @return true if this is PNode represents an output neuron
	 */
	public boolean isOutput() {
		if (motorCoupling == null) {
			return false;
		} else {
			return true;
		}

	}
	/**
	 * Registers the associated Neuron as an input neuron or not
	 * 
	 * @param in true if this is an input neuron, false otherwise 
	 */
	public void setInput(boolean in) {

		if (in == true) {
			parentPanel.getInputList().add(this);
			this.getNeuron().setInput(true);
			if(sensoryCoupling != null) {
				if(sensoryCoupling.getAgent() != null) {
					sensoryCoupling.getAgent().getParentWorld().addCommandTarget(this.parentPanel);									
				}
			}

		} else {
			parentPanel.getInputList().remove(this);
			this.getNeuron().setInput(false);
			if(sensoryCoupling != null) {
				if(sensoryCoupling.getAgent() != null) {
					sensoryCoupling.getAgent().getParentWorld().removeCommandTarget(this.parentPanel);				
				}
			}
			sensoryCoupling = null;
		}	
		
		updateInArrow();
		render();


	}
	
	
	/**
	 * Registers the associated Neuron as an output neuron or not
	 * 
	 * @param in true if this is an output neuron, false otherwise
	 */
	public void setOutput(boolean out) {
				
		if (out == true) {
			parentPanel.getOutputList().add(this);
		} else {
			parentPanel.getOutputList().remove(this);
			motorCoupling = null;
		}	
		
		updateOutArrow();
		render();
	}

	/**
	 * Updates graphics depending on whether this is an input node or not
	 */
	private void updateInArrow() {
		if (isInput()) {
			GeneralPath ia = createInArrow();
			inArrow.reset();
			inArrow.append(ia, false);
		} else {
			this.inArrow.reset();
		}
	}

	

	/**
	 * Updates graphics depending on whether this is an output node or not
	 */
	private void updateOutArrow() {

		if (isOutput()) {
			GeneralPath ia = createOutArrow();
			outArrow.reset();
			outArrow.append(ia, false);
		} else {
			outArrow.reset();
		}
	}

	/**
	 * Increments the activation level of the associated Neuron and updates the PNode graphics to reflect the new 
	 * activation level
	 * 
	 */
	public void upArrow() {
		neuron.incrementActivation();
		this.render();
	}

	/**
	 * Decrements the activation level of the associated Neuron and updates the PNode graphics to reflect the new 
	 * activation level
	 */
	public void downArrow() {
		neuron.decrementActivation();
		this.render();
	}

	/**
	 * Randomizes the associated Neuron and updates the PNode graphics
	 */
	public void randomize() {
		neuron.randomize();
		this.render();

	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean sel) {
		selected = sel;
	}

	public void remove() {
		this.remove();
	}

	/**
	 * Registers the neuron which this PNodeNeuron represents
	 * 
	 * @param n
	 */
	public void setNeuron(Neuron n) {
		neuron = n;
	}
	
	public static void setNeuronSize(int s) {
		neuronSize = s;
		
	}
	
	public static int getNeuronSize() {
		return neuronSize;
	}
	/**
	 * @return Returns the xpos.
	 */
	public double getXpos() {
		return NetworkPanel.getGlobalX(this);
	}
	/**
	 * @param xpos The xpos to set.
	 */
	public void setXpos(double xpos) {
		Point2D p = new Point2D.Double(xpos, getYpos());
		globalToLocal(p);
		this.setBounds(p.getX(), p.getY(), this.getWidth(), this.getHeight());
		updateInArrow();
		updateOutArrow();
		
	}
	/**
	 * @return Returns the ypos.
	 */
	public double getYpos() {
		return NetworkPanel.getGlobalY(this);
	}
	/**
	 * @param ypos The ypos to set.
	 */
	public void setYpos(double ypos) {
		Point2D p = new Point2D.Double(getXpos(), ypos);
		globalToLocal(p);
		this.setBounds(p.getX(), p.getY(), this.getWidth(), this.getHeight());
		updateInArrow();
		updateOutArrow();
	}

    
	/**
	 * Change the type of neuron this pnode is associated with
	 * It is assumed that the basic properties of the new neuron have been set
	 * 
	 * @param new_neuron the neuron to change to
	 */
	public void changeNeuron(Neuron new_neuron) {
		Network.changeNeuron(neuron, new_neuron);
		neuron = new_neuron;		
	}
	
	/**
	 * @return Returns the net_panel.
	 */
	public NetworkPanel getParentPanel() {
		return parentPanel;
	}
	
	/**
	 * @param net_panel The net_panel to set.
	 */
	public void setParentPanel(NetworkPanel net_panel) {
		this.parentPanel = net_panel;
	}
	/**
	 * @return Returns the coldColor.
	 */
	public static float getCoolColor() {
		return coolColor;
	}
	/**
	 * @param coldColor The coldColor to set.
	 */
	public static void setCoolColor(float coldColor) {
		PNodeNeuron.coolColor = coldColor;
	}
	/**
	 * @return Returns the hotColor.
	 */
	public static float getHotColor() {
		return hotColor;
	}
	/**
	 * @param hotColor The hotColor to set.
	 */
	public static void setHotColor(float hotColor) {
		PNodeNeuron.hotColor = hotColor;
	}
	
	protected void addCoupling(Coupling c) {
		this.getParentPanel().getParentFrame().getWorkspace().getCouplingList().add(c);
	}
	
	protected void removeCoupling(Coupling c) {
		this.getParentPanel().getParentFrame().getWorkspace().getCouplingList().remove(c);
	}
	
	public void setCoupling(Coupling c) {
		if (c instanceof SensoryCoupling) {
			setSensoryCoupling((SensoryCoupling)c);
		} else if (c instanceof MotorCoupling) {
			setMotorCoupling((MotorCoupling)c);
		}
	}
	
	/**
	 * @return Returns the sensory_coupling.
	 */
	public SensoryCoupling getSensoryCoupling() {
		return sensoryCoupling;
	}
	
	/**
	 * @param sensory_coupling The sensory_coupling to set.  Null if there is none.
	 */
	public void setSensoryCoupling(SensoryCoupling sensory_coupling) {
		
		if(sensory_coupling == null) return;
		
		// When invoked by Castor
		if(getParentPanel() == null) {
			this.sensoryCoupling = sensory_coupling;
			sensoryCoupling.setNeuron(this);
			return;
		}
		
		removeCoupling(this.sensoryCoupling);	
		sensory_coupling.setNeuron(this);
		sensory_coupling.initCastor();
		addCoupling(sensory_coupling);
		this.sensoryCoupling = sensory_coupling;
		setInput(true);
	}
	/**
	 * @return Returns the motor_coupling.
	 */
	public MotorCoupling getMotorCoupling() {
		return motorCoupling;
	}

	/**
	 * @param motor_coupling The motor_coupling to set.   Null if there is none.
	 */
	public void setMotorCoupling(MotorCoupling motor_coupling) {
		
		if(motor_coupling == null) return;
		
		// When invoked by castor
		if(getParentPanel() == null) {
			this.motorCoupling = motor_coupling;			
			motorCoupling.setNeuron(this);
			return;
		}
		removeCoupling(this.motorCoupling);	
		motor_coupling.setNeuron(this);
		motor_coupling.initCastor();
		addCoupling(motor_coupling);
		this.motorCoupling = motor_coupling;
		setOutput(true);	

	}
	
	/**
	 * returns the value used by the Gauge
	 */
	public double getGaugeValue() {
		return this.getNeuron().getActivation();
	}
	
	
	public void debug() {
		neuron.debug();
		if(motorCoupling != null) {
			motorCoupling.debug();
		}
		if(sensoryCoupling != null) {
			sensoryCoupling.debug();			
		}
		
	}
	
	/**
	 * @return Returns the id.
	 */
	public String getId() {
		return "p" + neuron.getId();
	}
	
	/**
	 * @param id The id to set.
	 */
	public void setId(String theId) {
		this.id = theId;
	}
	
	
	/**
	 * Perform initialization needed when this object is added to the network.
	 */
	public void addToNetwork(NetworkPanel np) {
		np.getNetwork().addNeuron(getNeuron());
	}
	
	public void delete() {

		ArrayList fanOut = neuron.getFanOut();
		ArrayList fanIn = neuron.getFanIn();
		ArrayList toDelete = new ArrayList();

		ArrayList nodeList = parentPanel.getNodeList();
		//Identify connected weights to remove
		for (int i = 0; i < nodeList.size(); i++) {
			PNode pn =  (PNode)nodeList.get(i);
			if(pn instanceof PNodeWeight) {
				PNodeWeight pnw = (PNodeWeight)pn;
				if(fanOut.contains(pnw.getWeight())) {
					toDelete.add(pn);
				}
				if(fanIn.contains(pnw.getWeight())) {
					toDelete.add(pn);
				}	
			}
		}
		
		//Remove connected weights
		for (int i = 0; i < toDelete.size(); i++) {
			parentPanel.deleteNode((PNodeWeight)toDelete.get(i));
		}

		parentPanel.getParentFrame().getWorkspace().getCouplingList().remove(sensoryCoupling);
		parentPanel.getParentFrame().getWorkspace().getCouplingList().remove(motorCoupling);
		parentPanel.getNetwork().deleteNeuron(getNeuron());

	}
	
	/**
	 * Set the position of a new neuron, to the right of any selected screen object
	 */
	public void initNewNeuron()
	{

		PNode selectNeuron = parentPanel.getSingleSelection();

		// If a node is selected, put this node to its left
		if (selectNeuron != null) {
			this.setXpos(NetworkPanel.getGlobalX((PNode) selectNeuron) + PNodeNeuron.neuronScale + 45);
			this.setYpos(NetworkPanel.getGlobalY((PNode) selectNeuron));
		}
		parentPanel.getNetwork().addNeuron(getNeuron());
		getNeuron().setNeuronParent(parentPanel.getNetwork());
		setId(getNeuron().getId());	
		parentPanel.addNode(this, true);
	}
	
	public void drawBoundary()
	{
		return;
	}
	
	public boolean isSelectable()
	{
		return true;
	}
	
	/**
	 * @param np Reference to parent NetworkPanel
	 */
	public void initCastor(NetworkPanel np)
	{
		setParentPanel(np);
		init();
		if(getSensoryCoupling() != null) {
			Agent a = np.getParentFrame().getWorkspace().findMatchingAgent(getSensoryCoupling());
			if (a != null) {
				setSensoryCoupling(new SensoryCoupling(a, this, getSensoryCoupling().getSensorArray()));
			}					
		}
		if(getMotorCoupling() != null) {
			 Agent a = np.getParentFrame().getWorkspace().findMatchingAgent(getMotorCoupling());
				if (a != null) {
					setMotorCoupling(new MotorCoupling(a, this, getMotorCoupling().getCommandArray()));
				}					
		}		
	}
	
	public void increment()
	{
		upArrow();
	}
	
	public void decrement()
	{
		downArrow();
	}
	
	public void nudge(int offsetX, int offsetY, double nudgeAmount)
	{
		offset(offsetX * nudgeAmount, offsetY * nudgeAmount);
	}
	
	public void renderNode()
	{
		render();
		moveToFront();
	}
}