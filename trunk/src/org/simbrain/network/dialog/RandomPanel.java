/*
 * Created on Jul 26, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.simbrain.network.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simnet.neurons.RandomNeuron;

/**
 * @author jyoshimi
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RandomPanel extends LabelledItemPanel implements ActionListener { 

	private JComboBox cbDistribution = new JComboBox(RandomNeuron.getFunctionList());
	private JTextField tfUpBound = new JTextField();
	private JTextField tfLowBound = new JTextField();
	private JTextField tfMean = new JTextField();
	private JTextField tfStandardDeviation = new JTextField();
	private JCheckBox isUseBoundsBox = new JCheckBox();

	public RandomPanel() {
        cbDistribution.addActionListener(this);
        isUseBoundsBox.addActionListener(this);
        isUseBoundsBox.setActionCommand("useBounds");
        
        this.addItem("Distribution", cbDistribution);
        this.addItem("Upper bound", tfUpBound);
        this.addItem("Lower bound", tfLowBound);
        this.addItem("Mean value", tfMean);
        this.addItem("Standard deviation", tfStandardDeviation);
        this.addItem("Use bounds", isUseBoundsBox);

        init();
	}
    
    public void init(){
	    if(cbDistribution.getSelectedIndex() == 0){
	        tfUpBound.setEnabled(true);
	        tfLowBound.setEnabled(true);
	        tfMean.setEnabled(false);
	        tfStandardDeviation.setEnabled(false);
	        isUseBoundsBox.setSelected(true);
	        isUseBoundsBox.setEnabled(false);
	    } else if (cbDistribution.getSelectedIndex() == 1){
	        tfMean.setEnabled(true);
	        tfStandardDeviation.setEnabled(true);
	        isUseBoundsBox.setEnabled(true);
	        checkBounds();
	    } 
    }
	
    /**
     * Enable or disable the upper and lower bounds fields depending on state of rounding button
     *
     */
    private void checkBounds() {
        if (isUseBoundsBox.isSelected() == false) {
            tfLowBound.setEnabled(false);
            tfUpBound.setEnabled(false);
        } else {
            tfLowBound.setEnabled(true);
            tfUpBound.setEnabled(true);
        }
    }

	public void actionPerformed(ActionEvent e){

	    if(e.getActionCommand().equals("useBounds")){
	        checkBounds();
	    }
	    init();
	}
	/**
	 * @return Returns the cbDistribution.
	 */
	public JComboBox getCbDistribution() {
		return cbDistribution;
	}
	/**
	 * @param cbDistribution The cbDistribution to set.
	 */
	public void setCbDistribution(JComboBox cbDistribution) {
		this.cbDistribution = cbDistribution;
	}
	/**
	 * @return Returns the isUseBoundsBox.
	 */
	public JCheckBox getIsUseBoundsBox() {
		return isUseBoundsBox;
	}
	/**
	 * @param isUseBoundsBox The isUseBoundsBox to set.
	 */
	public void setIsUseBoundsBox(JCheckBox isUseBoundsBox) {
		this.isUseBoundsBox = isUseBoundsBox;
	}
	/**
	 * @return Returns the tfLowBound.
	 */
	public JTextField getTfLowBound() {
		return tfLowBound;
	}
	/**
	 * @param tfLowBound The tfLowBound to set.
	 */
	public void setTfLowBound(JTextField tfLowBound) {
		this.tfLowBound = tfLowBound;
	}
	/**
	 * @return Returns the tfMean.
	 */
	public JTextField getTfMean() {
		return tfMean;
	}
	/**
	 * @param tfMean The tfMean to set.
	 */
	public void setTfMean(JTextField tfMean) {
		this.tfMean = tfMean;
	}
	/**
	 * @return Returns the tfStandardDeviation.
	 */
	public JTextField getTfStandardDeviation() {
		return tfStandardDeviation;
	}
	/**
	 * @param tfStandardDeviation The tfStandardDeviation to set.
	 */
	public void setTfStandardDeviation(JTextField tfStandardDeviation) {
		this.tfStandardDeviation = tfStandardDeviation;
	}
	/**
	 * @return Returns the tfUpBound.
	 */
	public JTextField getTfUpBound() {
		return tfUpBound;
	}
	/**
	 * @param tfUpBound The tfUpBound to set.
	 */
	public void setTfUpBound(JTextField tfUpBound) {
		this.tfUpBound = tfUpBound;
	}
}