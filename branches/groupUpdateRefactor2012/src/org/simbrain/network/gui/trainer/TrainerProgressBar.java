package org.simbrain.network.gui.trainer;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import org.simbrain.network.trainers.ReservoirComputingUtils;
import org.simbrain.network.trainers.Trainer;
import org.simbrain.network.trainers.LMSOffline;

public class TrainerProgressBar extends JPanel 
	implements ActionListener, PropertyChangeListener {

    private JProgressBar progressBar;
    private ListeningTextArea taskOutput;
    JButton initButton;
    private Task task;
	private Trainer trainer;
    
	
	public TrainerProgressBar(Trainer trainer, JButton initButton) {
		super(new BorderLayout());
		
		this.trainer = trainer; 
		if (trainer.isStateHarvester()) {
			ReservoirComputingUtils.setStateListener(this);
		} 
		((LMSOffline) trainer.getTrainingMethod()).addPropertyChangeListener(this);

		this.initButton = initButton;
		initButton.addActionListener(this);
		
		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
		progressBar.setStringPainted(true);
		
		
		taskOutput = new ListeningTextArea();
		taskOutput.setMargin(new Insets(5,5,5,5));
		taskOutput.setEditable(false);
		taskOutput.setText("...");
		((LMSOffline) trainer.getTrainingMethod()).addPropertyChangeListener(taskOutput);
		
		if(trainer.getTrainingMethod() instanceof LMSOffline) {
			((LMSOffline) trainer.getTrainingMethod()).
				addPropertyChangeListener(this);
				addPropertyChangeListener(taskOutput);
		}
		
		add(progressBar, BorderLayout.NORTH);
		add(taskOutput, BorderLayout.SOUTH);
		
	}
	
	class ListeningTextArea extends JTextArea implements PropertyChangeListener {

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			this.setText(evt.getPropertyName());
		}
	}
	
	class Task extends SwingWorker<Void, Void> {

		
		Trainer trainer;
		
		public Task (Trainer trainer) {
			this.trainer = trainer;
		}
		
		@Override
		protected Void doInBackground() throws Exception {
			setProgress(0);
	
			((LMSOffline) trainer.getTrainingMethod()).apply(trainer);
			
			return null;
		}
		
		@Override
		public void done () {
			initButton.setEnabled(true);
			taskOutput.setText("Done!");
			trainer.getNetwork().getRootNetwork().fireNetworkChanged();
		}
		
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		int newVal;
		int oldVal;
		int multiplier;
		if(trainer.isStateHarvester()) {
			multiplier = 50;
		
		} else {
			multiplier = 100;
		}
		
		if(arg0.getPropertyName() == "StateHarvesting"){
			
			newVal = (int) (((Number) arg0.getNewValue()).doubleValue() *
					multiplier);
			oldVal = (int) (((Number) arg0.getOldValue()).doubleValue() *
					multiplier);
			if((newVal - oldVal) >= 1){
				progressBar.setValue(newVal);
			} 
		}
		if(arg0.getPropertyName() == "Training") {

			newVal = (int) (((Number) arg0.getNewValue()).doubleValue() *
					multiplier);
			oldVal = (int) (((Number) arg0.getOldValue()).doubleValue() *
					multiplier);
			if((newVal - oldVal) >= 1){
				if(trainer.isStateHarvester()) {
					progressBar.setValue(progressBar.getValue() + newVal);
				} else {
					progressBar.setValue(newVal);
				}
			} 
		}
		
		
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getSource() == initButton) {
			initButton.setEnabled(false);

			Task task = new Task(trainer);
			task.execute();
		}
		
	}

}
