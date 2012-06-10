package org.simbrain.world.game.tictactoe;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Interface for a tic-tac-toe (naughts and crosses) game.
 *
 * @author Matt Watson
 */
public class TicTacToeGui {
    /** The default board size. */
    private static final int DEFAULT_BOARD_SIZE = 3;

    /** The icon for an empty square. */
    private final ImageIcon blank = new ImageIcon("docs/Images/blank.gif");
    /** The icon for an 'X' square. */
    private final ImageIcon ecks = new ImageIcon("docs/Images/ecks.gif");
    /** The icon for an 'O' square. */
    private final ImageIcon ouh = new ImageIcon("docs/Images/ouh.gif");
    /** The size of the game board. */
    private final int size = DEFAULT_BOARD_SIZE;

    /** The listeners on this gui. */
    private List<Listener> listeners = new ArrayList<Listener>();
    /** The array of labels. */
    private JLabel[][] labels = new JLabel[size][size];

    /**
     * Returns a game panel.
     *
     * @return A game panel.
     */
    public JPanel getPanel() {
        JPanel panel = new JPanel(new GridLayout(size, size));

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                JLabel label = new JLabel(blank);
                labels[i][j] = label;
                Selected selected = new Selected(i, j);
                JButton button = new JButton(selected);

                button.setBackground(Color.white);

                button.add(label);

                panel.add(button);
            }
        }

        return panel;
    }

    /**
     * Updates the gui with the current model state.
     *
     * @param model The game model.
     */
    public void update(final TicTacToeModel model) {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                final ImageIcon icon;

                switch (model.getState(i, j)) {
                case EMPTY:
                    icon = blank;
                    break;
                case ECKS:
                    icon = ecks;
                    break;
                case OUH:
                    icon = ouh;
                    break;
                default:
                    throw new IllegalStateException("unknown state");
                }

                labels[i][j].setIcon(icon);
            }
        }
    }

    /**
     * Action for when a square is selected.
     *
     * @author Matt Watson
     */
    private class Selected extends AbstractAction {
        /** The default serial version ID. */
        private static final long serialVersionUID = 1L;
        /** The x coordinate. */
        final int x;
        /** The y coordinate. */
        final int y;

        /**
         * Creates a new selected action for the provided coordinates.
         *
         * @param x The x coordinate.
         * @param y The y coordinate.
         */
        Selected(final int x, final int y) {
            this.x = x;
            this.y = y;
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(final ActionEvent e) {
            updated(x, y);
        }
    }

    /**
     * Adds a listener to the gui.
     *
     * @param listener The listener to add.
     */
    public void addListener(final Listener listener) {
        listeners.add(listener);
    }

    /**
     * Calls all the listeners.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    private void updated(final int x, final int y) {
        for (Listener listener : listeners) {
            listener.updated(x, y);
        }
    }

    /**
     * The listener interface for this gui.
     *
     * @author Matt Watson
     */
    public interface Listener {
        /**
         * Called when the given coordinate is selected.
         *
         * @param x The x coordinate.
         * @param y The y coordinate.
         */
        void updated(final int x, final int y);
    }
}
