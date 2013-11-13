package org.simbrain.network.gui.dialogs.synapse;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.synapse.plasticity_panels.AbstractSynapsePanel;
import org.simbrain.network.gui.dialogs.synapse.plasticity_panels.ClampedSynapseRulePanel;
import org.simbrain.util.widgets.DropDownTriangle;
import org.simbrain.util.widgets.DropDownTriangle.UpDirection;

public class SynapseUpdateSettingsPanel extends JPanel {

	/** Null string. */
	public static final String NULL_STRING = "...";

	/**
	 * The default display state of the synapse panel. Currently, True, that is,
	 * by default, the synapse panel corresponding to the rule in the combo box
	 * is visible.
	 */
	private static final boolean DEFAULT_SP_DISPLAY_STATE = true;

	/** Synapse type combo box. */
	private final JComboBox<String> cbSynapseType =
			new JComboBox<String>(Synapse.getRuleList());

	/** The synapses being modified. */
	private final List<Synapse> synapseList;

	/** Synapse panel. */
	private AbstractSynapsePanel synapsePanel;

	/** For showing/hiding the synapse panel. */
	private final DropDownTriangle displaySPTriangle;

	/**
	 * A reference to the parent window, for resizing after panel content
	 * changes.
	 */
	private final Window parent;

	/**
	 * 
	 * @param synapseList
	 */
	public SynapseUpdateSettingsPanel(List<Synapse> synapseList,
			final Window parent) {
		this(synapseList, DEFAULT_SP_DISPLAY_STATE, parent);
	}

	/**
	 * 
	 * @param synapseList
	 * @param startingState
	 */
	public SynapseUpdateSettingsPanel(List<Synapse> synapseList,
			boolean startingState, final Window parent) {
		this.synapseList = synapseList;
		this.parent = parent;
		displaySPTriangle =
				new DropDownTriangle(UpDirection.LEFT, startingState,
						parent);
		initSynapseType();
		initializeLayout();
		addListeners();
	}

	/**
	 * Lays out this panel.
	 * 
	 * @return
	 */
	private void initializeLayout() {

		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		Border padding = BorderFactory.createEmptyBorder(5, 5, 5, 5);

		JPanel tPanel = new JPanel();
		tPanel.setLayout(new BoxLayout(tPanel, BoxLayout.X_AXIS));
		tPanel.add(cbSynapseType);
		tPanel.add(Box.createHorizontalStrut(20));

		JPanel supP = new JPanel(new FlowLayout());
		supP.add(new JLabel("Settings  "));
		supP.add(displaySPTriangle);

		tPanel.add(supP);
		tPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		tPanel.setBorder(padding);
		this.add(tPanel);

		this.add(Box.createRigidArea(new Dimension(0, 5)));

		synapsePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		synapsePanel.setBorder(padding);
		synapsePanel.setVisible(displaySPTriangle.isDown());
		this.add(synapsePanel);

		TitledBorder tb2 =
				BorderFactory.createTitledBorder("Update Rule");
		this.setBorder(tb2);

	}

	/**
	 * Adds the listeners to this dialog.
	 */
	private void addListeners() {
		displaySPTriangle.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {

				synapsePanel.setVisible(displaySPTriangle.isDown());
				repaint();
				parent.pack();

			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
			}

		});

		cbSynapseType.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				synapsePanel =
						Synapse.RULE_MAP.get(cbSynapseType
								.getSelectedItem());

				try {
					synapsePanel.fillFieldValues(Synapse
							.getRuleList(synapseList));
				} catch (Exception e) {
					synapsePanel.fillDefaultValues();
				}
				repaintPanel();
				parent.pack();
			}

		});

	}

	/**
	 * Called to repaint the panel based on changes in the to the selected
	 * synapse type.
	 */
	public void repaintPanel() {
		removeAll();
		initializeLayout();
		repaint();
	}

	/**
	 * Initialize the main synapse panel based on the type of the selected
	 * synapses.
	 */
	private void initSynapseType() {

		if (!NetworkUtils.isConsistent(synapseList, Synapse.class,
				"getType")) {
			cbSynapseType.addItem(AbstractSynapsePanel.NULL_STRING);
			cbSynapseType
					.setSelectedIndex(cbSynapseType.getItemCount() - 1);
			// Simply to serve as an empty panel
			synapsePanel = new ClampedSynapseRulePanel();
		} else {
			String synapseName =
					synapseList.get(0).getLearningRule().getDescription();
			synapsePanel = Synapse.RULE_MAP.get(synapseName);
			synapsePanel
					.fillFieldValues(Synapse.getRuleList(synapseList));
			cbSynapseType.setSelectedItem(synapseName);
		}

	}

	public JComboBox<String> getCbSynapseType() {
		return cbSynapseType;
	}

	public AbstractSynapsePanel getSynapsePanel() {
		return synapsePanel;
	}

	public void setSynapsePanel(AbstractSynapsePanel synapsePanel) {
		this.synapsePanel = synapsePanel;
	}
}
