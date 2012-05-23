package org.simbrain.network.gui.trainer;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;

import org.simbrain.network.trainers.LMSOffline;
import org.simbrain.network.trainers.TrainerListener;
import org.simbrain.network.util.RandomSource;

/**
 * Panel for Using an LMSOffline trainer.
 *
 * @author jeffyoshimi
 */
public class LMSOfflinePanel extends JPanel {
	
	
    /** Reference to trainer object. */
    private LMSOffline trainer;
    
    /** The swing worker which executes training in the background. */
    private final SwingWorker<Void,Void> worker;
    
    /** 
     * The main LMS panel, where the start button, progress bar, and solution
     * box reside.
     */
    private JPanel mainPanel = new JPanel();
    
    /** The panel devoted to ridge regression. */
    private JPanel regressionPanel = new JPanel();
    
    /** A titled border for the panel. */
    private TitledBorder mainBorder = BorderFactory
    		.createTitledBorder("LMS Offline");
    
    /** A titled border for the ridge regression panel. */
    private TitledBorder regressionBorder = BorderFactory
    		.createTitledBorder("Ridge Regression");
    
    /** The button which starts training. */
    private JButton applyButton = new JButton("Start");
    
    /** The progress bar, tracking the progress of training. */
    private final JProgressBar progressBar = new JProgressBar();

    /** The solution types as strings within the combo box. */
    private String [] solutions = {"Wiener-Hopf", "Psuedoinverse"};
    
    /** A combo box containing the supported solution types. */
	private final JComboBox solutionTypes = new JComboBox(solutions);
	
	/** A hashmap backing the solution type combo box. */
	private final HashMap<String, LMSOffline.SolutionType> pairing =
			new HashMap<String, LMSOffline.SolutionType>();
	
	//Initializing the solution type hash map...
	{
		pairing.put("Wiener-Hopf", LMSOffline.SolutionType.WIENER_HOPF);
		pairing.put("Psuedoinverse", LMSOffline.SolutionType.MOORE_PENROSE);
	}
	
	//This is flashy a checkbox could do the work... but this is more pretty...
	/** A button regulating the use of ridge regression. */
	private JButton regSwitch = new JButton("Off");
	
	/** A flag for whether or not regression is being used. */
	private boolean regressionActive;
	
	//Initialize pretty button...
	{
		regSwitch.setForeground(Color.red);
	}
	
	/** A text field containing the alpha value. */
	private JTextField alpha = new JTextField(" 0.5 ");
	  
    /**
     * Build the panel.
     *
     * @param trainer the LMSOffline trainer to represent
     */
    public LMSOfflinePanel(final LMSOffline trainer) {
    	
    	this.trainer = trainer;	
		worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				trainer.apply();
				return null;
			}
			
		};   
		
		setLayout(new GridLayout(0,1));
		fillMainPanel();
        fillRegressionPanel();
		add(mainPanel);
		add(regressionPanel);
		
		addActionListeners();
			
	}
    
    /**
     * Creates the main panel, containing the start button, progress bar, and
     * solution type selector.
     */
    private void fillMainPanel() {   	
    	mainPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 25, 5));
        mainPanel.setBorder(mainBorder);   
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);       
        progressBar.setStringPainted(true);
        
        mainPanel.add(applyButton);
		mainPanel.add(progressBar);
		mainPanel.add(solutionTypes);		
    }
    
    /**
     * Creates the regression panel, containing the regression switch, and a
     * field to set alpha levels.
     */
    private void fillRegressionPanel(){
    	regressionPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 25, 5));
    	regressionPanel.setBorder(regressionBorder);
    	
    	regressionPanel.add(regSwitch);
    	alpha.setEnabled(false);
    	regressionPanel.add(alpha);
    }

    /**
     * Adds all the necessary action listeners, including listeners for:
     * trainer, applyButton, solutionTypes, and regSwitch.
     */
    private void addActionListeners() {
    	
    	//Adds listeners which update the progress bars values during training
    	trainer.addListener(new TrainerListener() {

			@Override
			public void beginTraining() {
				//System.out.println("Training Begin");
				// progressBar.setIndeterminate(true);
				progressBar.setValue(0);
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			}

			@Override
			public void endTraining() {
				//System.out.println("Training End");
				// progressBar.setIndeterminate(false);
				progressBar.setValue(100);
				setCursor(null); // Turn off wait cursor
			}

			@Override
			public void progressUpdated(String progressUpdate,
					int percentComplete) {
				//System.out.println(progressUpdate + " -- " + percentComplete
				//		+ "%");
				progressBar.setValue(percentComplete);
				// progressBar.setString(progressUpdate);
			}
		});
    	
    	//Adds a listener for the start button: executes training upon firing.
    	//Also activates ridge regression in LMSOffline and sets the alpha.
    	applyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				trainer.setRidgeRegression(regressionActive);
				if(regressionActive) {
					trainer.setAlpha(Double.parseDouble(alpha.getText()));
				}
				worker.execute();
			}
        });
    	
    	//Combo-box listener...
    	solutionTypes.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				//Sets the solution type in LMSOffline
				trainer.setSolutionType(pairing.get(solutionTypes
						.getSelectedItem()));
				
				//Disables/enabled ridge regression based on selected solution	
				if(pairing.get(solutionTypes.getSelectedItem())
						== LMSOffline.SolutionType.MOORE_PENROSE) {
					regSwitch.setText("Off");
					regSwitch.setForeground(Color.red);
					regSwitch.setEnabled(false);
					alpha.setEnabled(false);
					regressionActive = false;
				} else {
					regSwitch.setEnabled(true);
				}
			}
			
		});
    	
    	//Ridge regression button listener
    	regSwitch.addActionListener(new ActionListener(){		
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(regressionActive) {
					regressionActive = false;
					regSwitch.setText("Off");
					regSwitch.setForeground(Color.red);
					alpha.setEnabled(false);
				} else {
					regressionActive = true;
					regSwitch.setText("On");
					regSwitch.setForeground(Color.green);
					alpha.setEnabled(true);
				}
			}
			
		});

    	
    }
    
    /**
     * Returns and instance of the apply button, so that other classes
     * @return
     */
    public JButton getApplyButton() { 
    	return applyButton;
    }
    
}
