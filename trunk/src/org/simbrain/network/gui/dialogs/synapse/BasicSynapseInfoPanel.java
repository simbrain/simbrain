package org.simbrain.network.gui.dialogs.synapse;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.util.DropDownTriangle;

public class BasicSynapseInfoPanel extends JPanel {

	/** Null string. */
	public static final String NULL_STRING = "...";

	/** Id Label. */
	private JLabel idLabel = new JLabel();

	private JLabel detailLabel = new JLabel();

	/** Strength field. */
	private JTextField tfStrength = new JTextField();

	/**
	 * A triangle that switches between an up (left) and a down state Used for
	 * showing/hiding extra synapse data.
	 */
	private DropDownTriangle detailTriangle = new DropDownTriangle(
			DropDownTriangle.LEFT, false);

	/**
	 * The extra data panel. Includes: increment, upper bound, lower bound, and
	 * priority.
	 */
	private ExtendedSynapseInfoPanel extraDataPanel;

	/** The synapses being modified. */
	private ArrayList<Synapse> synapseList = new ArrayList<Synapse>();

	/**
	 * @param selectedSynapses
	 *            the pnode_synapses being adjusted
	 */
	public BasicSynapseInfoPanel(final Collection<Synapse> synapseList) {
		this.synapseList = (ArrayList<Synapse>) synapseList;
		initializeLayout();
		fillFieldValues();
		addListeners();
	}

	/**
	 * Initialize the basic info panel (generic synapse parameters)
	 * 
	 * @return the basic info panel
	 */
	private void initializeLayout() {

		setLayout(new BorderLayout());

		JPanel basicsPanel = new JPanel(new GridBagLayout());
		basicsPanel
				.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0.8;
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(5, 0, 0, 0);
		basicsPanel.add(new JLabel("Synapse Id:"), gbc);

		gbc.gridwidth = 2;
		gbc.gridx = 1;
		basicsPanel.add(idLabel, gbc);

		gbc.weightx = 0.8;
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 1;
		basicsPanel.add(new JLabel("Strength:"), gbc);

		gbc.anchor = GridBagConstraints.EAST;
		gbc.insets = new Insets(5, 3, 0, 0);
		gbc.gridwidth = 2;
		gbc.weightx = 0.2;
		gbc.gridx = 1;
		basicsPanel.add(tfStrength, gbc);

		gbc.gridwidth = 1;
		int lgap = detailTriangle.isDown() ? 5 : 0;
		gbc.insets = new Insets(10, 5, lgap, 5);
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.weightx = 0.2;
		String details = detailTriangle.isDown() ? "Less" : "More";
		detailLabel.setText(details);
		basicsPanel.add(detailLabel, gbc);
		gbc.weightx = 0.0;
		gbc.gridx = 2;
		basicsPanel.add(detailTriangle, gbc);

		this.add(basicsPanel, BorderLayout.NORTH);

		extraDataPanel = new ExtendedSynapseInfoPanel(synapseList);

		extraDataPanel.setVisible(detailTriangle.isDown());

		this.add(extraDataPanel, BorderLayout.SOUTH);

		TitledBorder tb = BorderFactory.createTitledBorder("Basic Data");
		this.setBorder(tb);

	}

	/**
	 * Called Externally to repaint this panel based on whether or not extra
	 * data is displayed.
	 */
	public void repaintPanel() {
		extraDataPanel.setVisible(detailTriangle.isDown());
		String details = detailTriangle.isDown() ? "Less" : "More";
		detailLabel.setText(details);
		repaint();
	}

	/**
	 * A method for adding all internal listeners.
	 */
	private void addListeners() {

		// Add a listener to display/hide extra editable synapse data
		detailTriangle.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				// Repaint to show/hide extra data
				extraDataPanel.setVisible(detailTriangle.isDown());
				String details =
						detailTriangle.isDown() ? "Less" : "More";
				detailLabel.setText(details);
				// Alert the panel/dialog/frame this is embedded in to
				// resize itself accordingly
				firePropertyChange("Extra Data",
						!detailTriangle.isDown(), detailTriangle.isDown());
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}

		});
	}

	/**
	 * Set the initial values of dialog components.
	 */
	public void fillFieldValues() {

		Synapse synapseRef = synapseList.get(0);
		if (synapseList.size() == 1) {
			idLabel.setText(synapseRef.getId());
		} else {
			idLabel.setText(NULL_STRING);
		}

		// (Below) Handle consistency of multiple selections

		// Handle Strength
		if (!NetworkUtils.isConsistent(synapseList, Synapse.class,
				"getStrength"))
			tfStrength.setText(NULL_STRING);
		else
			tfStrength.setText(Double.toString(synapseRef.getStrength()));

	}

	/**
     * 
     */
	public void commitChanges() {
		for (int i = 0; i < synapseList.size(); i++) {

			Synapse synapseRef = synapseList.get(i);

			// Strength
			if (!tfStrength.getText().equals(NULL_STRING))
				synapseRef.setStrength(Double.parseDouble(tfStrength
						.getText()));

		}

		extraDataPanel.commitChanges();

	}

	/**
	 * 
	 * @return
	 */
	public DropDownTriangle getDetailTriangle() {
		return detailTriangle;
	}

}
