package org.simbrain.network.gui.trainer.subnetworkTrainingPanels;
import java.awt.event.ActionEvent;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.subnetworks.CompetitiveNetwork;
import org.simbrain.network.trainers.CompetitiveTrainer;
import org.simbrain.network.trainers.Trainer.DataNotInitializedException;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.LabelledItemPanel;

/**
 * Training panel for Competitive Network
 */
public class CompetitiveTrainerControlsPanel extends JPanel {

	/** Reference to trainer. */
	private CompetitiveTrainer trainer;

	/** Current number of iterations. */
	private JLabel iterationsLabel = new JLabel("--- ");

	/** Reference to network panel. */
	private NetworkPanel panel;

	/** Reference to the Competitive Network. */
	private CompetitiveNetwork network;

	/**
	 * Construct the Competitive Training Controls Panel.
	 *
	 * @param trainer reference to the Competitive trainer
	 */
	public CompetitiveTrainerControlsPanel(final NetworkPanel panel,
			final CompetitiveTrainer trainer, final CompetitiveNetwork network) {
		this.panel = panel;
		this.trainer = trainer;
		this.network = network;
		init();
	}

	/**
	 * Initialize the panel.
	 */
	public void init() {
		// Set up properties tab
		Box propsBox = Box.createVerticalBox();
		propsBox.setOpaque(true);
		propsBox.add(Box.createVerticalGlue());

		// Run Tools
		JPanel runTools = new JPanel();
		runTools.add(new JLabel("Iterate: "));
		runTools.add(new JButton(runAction));
		JButton stepButton = new JButton(stepAction);
		stepButton.setHideActionText(true);
		runTools.add(stepButton);
		JButton resetButton = new JButton(resetAction);
		resetButton.setHideActionText(true);
		runTools.add(resetButton);
		JButton randomizeButton = new JButton(randomizeAction);
		randomizeButton.setHideActionText(true);
		runTools.add(randomizeButton);
		propsBox.add(runTools);

		// Separator
		JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
		propsBox.add(separator);

		// Labels
		LabelledItemPanel labelPanel = new LabelledItemPanel();
		labelPanel.addItem("Iterations:", iterationsLabel);
		propsBox.add(labelPanel);

		// Wrap it up
		add(propsBox);
		update();

	}

	/**
	 * Update internal labels.
	 */
	private void update() {
		//TODO
		//            lLearningRate.setText("" + network.getCompetitive().getAlpha());
		//            lNeighborhoodSize.setText(""
		//                    + network.getCompetitive().getNeighborhoodSize());
		iterationsLabel.setText("" + trainer.getIteration());
	}

	/**
	 * A "play" action, that can be used to repeatedly iterate iterable
	 * training algorithms.
	 *
	 */
	private Action runAction = new AbstractAction() {

		// Initialize
		{
			putValue(SMALL_ICON, ResourceManager.getImageIcon("Play.png"));
			// putValue(NAME, "Open (.csv)");
			putValue(SHORT_DESCRIPTION,
					"Iterate training until stopping condition met");
		}

		/**
		 * {@inheritDoc}
		 */
		public void actionPerformed(ActionEvent arg0) {
			if (trainer == null) {
				return;
			}
			if (trainer.isUpdateCompleted()) {
				// Start running
				trainer.setUpdateCompleted(false);
				putValue(SMALL_ICON,
						ResourceManager.getImageIcon("Stop.png"));
				Executors.newSingleThreadExecutor().submit(new Runnable() {
					public void run() {
						try {
							while (!trainer.isUpdateCompleted()) {
								trainer.apply();
								update();
								// if (showUpdates.isSelected()) {
									//                                    panel.getNetwork()
									//                                            .setUpdateCompleted(false);
								//                                    panel.getNetwork().fireNetworkChanged();
								//                                    while (!panel.getNetwork()
								//                                            .isUpdateCompleted()) {
								//                                        try {
								//                                            Thread.sleep(1);
								//                                        } catch (InterruptedException e) {
								//                                            e.printStackTrace();
								//                                        }
								//                                    }
								// }
							}
						} catch (DataNotInitializedException e) {
							JOptionPane.showOptionDialog(null,
									e.getMessage(), "Warning",
									JOptionPane.DEFAULT_OPTION,
									JOptionPane.WARNING_MESSAGE, null,
									null, null);
						}
					}
				});
			} else {
				// Stop running
				trainer.setUpdateCompleted(true);
				update();
				panel.getNetwork().fireNetworkChanged();
				putValue(SMALL_ICON,
						ResourceManager.getImageIcon("Play.png"));
			}

		}

	};

	/**
	 * /** Apply training algorithm.
	 */
	private Action stepAction = new AbstractAction() {

		// Initialize
		{
			putValue(SMALL_ICON, ResourceManager.getImageIcon("Step.png"));
			putValue(NAME, "Train network");
			// putValue(SHORT_DESCRIPTION, "Import table from .csv");
		}

		/**
		 * {@inheritDoc}
		 */
		public void actionPerformed(ActionEvent arg0) {
			if (trainer == null) {
				return;
			}
			try {
				trainer.apply();
				update();
				panel.getNetwork().fireNetworkChanged();
			} catch (DataNotInitializedException e) {
				JOptionPane.showOptionDialog(null, e.getMessage(),
						"Warning", JOptionPane.DEFAULT_OPTION,
						JOptionPane.WARNING_MESSAGE, null, null, null);
			}
		}

	};

	/**
	 * Action for reseting the underlying network.
	 */
	private Action resetAction = new AbstractAction() {

		// Initialize
		{
			putValue(SMALL_ICON, ResourceManager.getImageIcon("Reset.png"));
			putValue(NAME, "Reset");
			putValue(SHORT_DESCRIPTION, "Reset network");
		}

		/**
		 * {@inheritDoc}
		 */
		public void actionPerformed(ActionEvent arg0) {
			network.getCompetitive().randomize();
			trainer.setIteration(0);
			panel.getNetwork().fireNetworkChanged();
		}
	};
	/**
	 * Action for randomizing the underlying network.
	 */
	private Action randomizeAction = new AbstractAction() {

		// Initialize
		{
			putValue(SMALL_ICON, ResourceManager.getImageIcon("Rand.png"));
			putValue(NAME, "Randomize");
			putValue(SHORT_DESCRIPTION, "Randomize network");
		}

		/**
		 * {@inheritDoc}
		 */
		public void actionPerformed(ActionEvent arg0) {
			network.getCompetitive().randomize();
			update();
			panel.getNetwork().fireNetworkChanged();
		}
	};
}

