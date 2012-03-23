package org.simbrain.network.gui.trainer;

import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.simbrain.network.trainers.LMSOffline;
import org.simbrain.network.trainers.TrainerListener;

/**
 * Panel for Using an LMSOffline trainer.
 *
 * @author jeffyoshimi
 */
public class LMSOfflinePanel extends JPanel {
	
	
    /** Reference to trainer object. */
    private LMSOffline trainer;

    /**
     * Build the panel.
     *
     * @param trainer the LMSOffline trainer to represent
     */
    public LMSOfflinePanel(final LMSOffline trainer) {
    	this.trainer = trainer;
        setLayout(new FlowLayout(FlowLayout.LEADING, 25, 5));


        JButton applyButton = new JButton("Apply");
		final SwingWorker<Void,Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				trainer.apply();
				return null;
			}
			
		};
        applyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				worker.execute();
			}
        });
		add(applyButton);
		
        final JProgressBar progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);       
        progressBar.setStringPainted(true);
		add(progressBar);
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
        
		JButton setPropertiesButton = new JButton();
		setPropertiesButton.setHideActionText(true);
		setPropertiesButton.setAction( TrainerGuiActions.getPropertiesDialogAction(trainer));
    	add(setPropertiesButton);

	}

}
