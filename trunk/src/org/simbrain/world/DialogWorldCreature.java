
package org.simbrain.world;

import javax.swing.JTextField;
import java.awt.Dimension;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

public class DialogWorldCreature extends StandardDialog{

	private Agent entityRef = null;
	private LabelledItemPanel myContentPane = new LabelledItemPanel();
	
	private JTextField tfWhiskerAngle = new JTextField();
	private JTextField tfWhiskerLength = new JTextField();
	private JTextField tfTurnIncrement = new JTextField();
	private JTextField tfStraightMovementIncrement= new JTextField();
	private JTextField tfAbsoluteMovementIncrement = new JTextField();
	
	public DialogWorldCreature(Agent we){
	    entityRef = we;
	    init();
	}
	
	public void init(){
	    
	    setTitle("Creature Dialog");
	    
	    fillFieldValues();
	    
	    myContentPane.addItem("Whisker angle", this.tfWhiskerAngle);
	    myContentPane.addItem("Whisker length", this.tfWhiskerLength);
	    myContentPane.addItem("Turn Increment", this.tfTurnIncrement);
	    myContentPane.addItem("Straight movement increment", this.tfStraightMovementIncrement);
	    myContentPane.addItem("Absolute movement increment", this.tfAbsoluteMovementIncrement);
	    
	    setContentPane(myContentPane);	    
	}
	
	public void fillFieldValues(){
	    
	    tfWhiskerAngle.setText(Double.toString(entityRef.getWhiskerAngle() * 180 / Math.PI));
	    tfWhiskerLength.setText(Double.toString(entityRef.getWhiskerLength()));
	    tfTurnIncrement.setText(Double.toString(entityRef.getTurnIncrement()));
	    tfStraightMovementIncrement.setText(Double.toString(entityRef.getStraightMovementIncrement()));
	    tfAbsoluteMovementIncrement.setText(Double.toString(entityRef.getAbsoluteMovementIncrement()));
	    
	}
	
	public void getValues(){
	    
	    entityRef.setWhiskerAngle(Double.parseDouble(tfWhiskerAngle.getText()) * Math.PI / 180);
	    entityRef.setWhiskerLength(Double.parseDouble(tfWhiskerLength.getText()));
	    entityRef.setTurnIncrement(Double.parseDouble(tfTurnIncrement.getText()));
	    entityRef.setStraightMovementIncrement(Double.parseDouble(tfStraightMovementIncrement.getText()));
	    entityRef.setAbsoluteMovementIncrement(Integer.parseInt(tfAbsoluteMovementIncrement.getText()));
	    
	}
}
