/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
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
package org.simbrain.network.trainers;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.network.util.SimnetUtils;
import org.simbrain.util.math.Matrices;
import org.simbrain.util.propertyeditor.ComboBoxWrapper;

import Jama.Matrix;

/**
 * Offline/Batch Learning with least mean squares.
 * 
 * @author ztosi
 * @author jyoshimi
 */
public class LMSOffline extends Trainer {

	/** Current solution type. */
	private SolutionType solutionType = SolutionType.WIENER_HOPF;

	/** Whether or not ridge regression is to be performed. */
	private boolean ridgeRegression;

	/** The magnitude of the ridge regression. */
	private double alpha;

	/**
	 * Construct the LMSOOffline object, with a trainable network the Synapse
	 * group where the new synapses will be placed.
	 * 
	 * @param network
	 *            the network to train
	 */
	public LMSOffline(Trainable network) {
		super(network);
	}

	/**
	 * Solution methods for offline LMS.
	 */
	public enum SolutionType {
		/**
		 * Wiener-Hopf solution.
		 */
		WIENER_HOPF {
			@Override
			public String toString() {
				return "Wiener-Hopf";
			}
		},

		/**
		 * Moore-Penrose Solution.
		 */
		MOORE_PENROSE {
			@Override
			public String toString() {
				return "Moore-Penrose";
			}
		}

	};

	@Override
	public void apply() throws DataNotInitializedException {

		if (getTrainableNetwork().getTrainingSet().getInputData() == null) {
			throw new DataNotInitializedException(
					"Input data not initalized");
		}
		if (getTrainableNetwork().getTrainingSet().getTargetData() == null) {
			throw new DataNotInitializedException(
					"Target data not initalized");
		}

		fireTrainingBegin();

		int index = 0;
		for (Neuron n : network.getOutputNeurons()) {
			if (n.getUpdateRule() instanceof SigmoidalRule) {
				for (int i = 0; i < network.getTrainingSet()
						.getTargetData().length; i++) {
					network.getTrainingSet().getTargetData()[i][index] =
							((SigmoidalRule) n.getUpdateRule())
									.getInverse(network.getTrainingSet()
											.getTargetData()[i][index]);
				}
			}
			index++;
		}

		if (solutionType == SolutionType.WIENER_HOPF) {
			weinerHopfSolution(network);
		} else if (solutionType == SolutionType.MOORE_PENROSE) {
			moorePenroseSolution(network);
		} else {
			throw new IllegalArgumentException("Solution type must be "
					+ "'MoorePenrose' or 'WeinerHopf'.");
		}

		fireTrainingEnd();

	}

	/**
	 * Implements the Wiener-Hopf solution to LMS linear regression.
	 */
	public void weinerHopfSolution(Trainable network) {
		Matrix inputMatrix =
				new Matrix(network.getTrainingSet().getInputData());
		Matrix trainingMatrix =
				new Matrix(network.getTrainingSet().getTargetData());

		fireProgressUpdate("Correlating State Matrix (R = S'S)...", 0);
		trainingMatrix = inputMatrix.transpose().times(trainingMatrix);

		fireProgressUpdate(
				"Cross-Correlating States with Teacher data (P = S'D)...",
				15);
		inputMatrix = inputMatrix.transpose().times(inputMatrix);

		fireProgressUpdate("Computing Inverse Correlation Matrix...", 30);
		try {

			if (ridgeRegression) {
				Matrix scaledIdentity =
						Matrix.identity(inputMatrix.getRowDimension(),
								inputMatrix.getColumnDimension()).times(
								alpha * alpha);
				inputMatrix = inputMatrix.plus(scaledIdentity);
			}

			inputMatrix = inputMatrix.inverse();

			fireProgressUpdate("Computing Weights...", 80);
			double[][] wOut =
					inputMatrix.times(trainingMatrix).getArray();
			fireProgressUpdate("Setting Weights...", 95);
			SimnetUtils.setWeights(network.getInputNeurons(),
					network.getOutputNeurons(), wOut);
			fireProgressUpdate("Done!", 100);

			// TODO: What error does JAMA actually throw for singular Matrices?
		} catch (RuntimeException e) {
			JOptionPane.showMessageDialog(new JFrame(), ""
					+ "State Correlation Matrix is Singular",
					"Training Failed", JOptionPane.ERROR_MESSAGE);
			fireProgressUpdate("Training Failed", 0);
		}

		trainingMatrix = null;
		inputMatrix = null;
	}

	/**
	 * Moore penrose.
	 */
	public void moorePenroseSolution(Trainable network) {
		Matrix inputMatrix =
				new Matrix(network.getTrainingSet().getInputData());
		Matrix trainingMatrix =
				new Matrix(network.getTrainingSet().getTargetData());

		fireProgressUpdate("Computing Moore-Penrose Pseudoinverse...", 0);
		// Computes Moore-Penrose Pseudoinverse
		inputMatrix = Matrices.pinv(inputMatrix);

		fireProgressUpdate("Computing Weights...", 50);
		double[][] wOut = inputMatrix.times(trainingMatrix).getArray();

		fireProgressUpdate("Setting Weights...", 75);
		SimnetUtils.setWeights(network.getInputNeurons(),
				network.getOutputNeurons(), wOut);
		fireProgressUpdate("Done!", 100);

		inputMatrix = null;
		trainingMatrix = null;
	}

	/**
	 * Set solution type.
	 * 
	 * @param solutionType
	 *            the solutionType to set
	 */
	public void setSolutionType(SolutionType solutionType) {
		this.solutionType = solutionType;
	}

	/**
	 * Returns the current solution type inside a comboboxwrapper. Used by
	 * preference dialog.
	 * 
	 * @return the the comboBox
	 */
	public ComboBoxWrapper getSolutionType() {
		return new ComboBoxWrapper() {
			public Object getCurrentObject() {
				return solutionType;
			}

			public Object[] getObjects() {
				return SolutionType.values();
			}
		};
	}

	/**
	 * Set the current parse style. Used by preference dialog.
	 * 
	 * @param solutionType
	 *            the current solution.
	 */
	public void setSolutionType(ComboBoxWrapper solutionType) {
		setSolutionType((SolutionType) solutionType.getCurrentObject());
	}

	public boolean isRidgeRegression() {
		return ridgeRegression;
	}

	public void setRidgeRegression(boolean ridgeRegression) {
		this.ridgeRegression = ridgeRegression;
	}

	public double getAlpha() {
		return alpha;
	}

	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}

}
