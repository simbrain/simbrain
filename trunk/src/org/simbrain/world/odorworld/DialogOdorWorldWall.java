package org.simbrain.world.odorworld;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>DialogOdorWorldWall</b> is a dialog box for setting the properties of a wall.
 */
public class DialogOdorWorldWall extends StandardDialog implements ActionListener, ChangeListener {

	private OdorWorld world = null;
	private Wall wall = null;
	private LabelledItemPanel panel = new LabelledItemPanel();
	private JButton colorButton = new JButton("Set");
	private JSlider width = new JSlider();
	private JSlider height = new JSlider();

	/**
	 * This method is the default constructor.
	 */
	public DialogOdorWorldWall(OdorWorld dworld, Wall selectedWall) {
		wall = selectedWall;
		world = dworld;
		init();
	}

	/**
	 * This method initialises the components on the panel.
	 */
	private void init() {
		//Initialize Dialog
		setTitle("Wall Dialog");
		fillFieldValues();
		this.setLocation(500, 0); //Sets location of network dialog

		//Set up sliders
		width.setMajorTickSpacing(25);
		width.setPaintTicks(true);
		width.setPaintLabels(true);
		height.setMajorTickSpacing(25);
		height.setPaintTicks(true);
		height.setPaintLabels(true);

		//Add Action Listeners
		colorButton.addActionListener(this);
		width.addChangeListener(this);
		height.addChangeListener(this);

		//Set up panel
		panel.addItem("Set wall color (all Walls)", colorButton);
		panel.addItem("Width", width);
		panel.addItem("Height", height);

		setContentPane(panel);
	}

	/**
	 * Respond to button pressing events
	 */
	public void actionPerformed(ActionEvent e) {

		Object o = e.getSource();

		if (o == colorButton) {
			Color theColor = getColor();
			if (theColor != null) {
				world.setWallColor(theColor);
			}
		}

	}

	/**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		width.setValue(wall.getWidth());
		height.setValue(wall.getHeight());
	}

	/** (non-Javadoc)
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	public void stateChanged(ChangeEvent e) {
		JSlider j = (JSlider) e.getSource();
		if (j == width) {
			wall.setWidth(j.getValue());
			world.repaint();
		} else if (j == height) {
			wall.setHeight(j.getValue());
			world.repaint();
		}
	}

	/**
	 * Show the color pallette and get a color
	 * 
	 * @return selected color
	 */
	public Color getColor() {
		JColorChooser colorChooser = new JColorChooser();
		Color theColor = JColorChooser.showDialog(this, "Choose Color",
				Color.BLACK);
		colorChooser.setLocation(200, 200); //Set location of color chooser
		return theColor;
	}


}