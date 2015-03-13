package org.simbrain.world.game.tictactoe;

import java.util.ArrayList;
import java.util.List;

import org.simbrain.world.game.GameModel;

/**
 * Holds the state of the game.
 *
 * @author Matt Watson
 */
public class TicTacToeModel extends GameModel {
    /** the size of the board (3). */
    private static final int SIZE = 3;
    /** The state of the board. */
    State[][] board = { { State.EMPTY, State.EMPTY, State.EMPTY },
            { State.EMPTY, State.EMPTY, State.EMPTY },
            { State.EMPTY, State.EMPTY, State.EMPTY } };

    /**
     * Creates a new tic-tac-toe game.
     */
    public TicTacToeModel() {
        super("tic tac toe", SIZE);
    }

    /**
     * Type for the state of a square.
     *
     * @author Matt Watson
     */
    public enum State {
        /** State for an empty square. */
        EMPTY,
        /** 'O' state for a square. */
        OUH,
        /** 'X' state for a square. */
        ECKS
    }

    /**
     * Returns the state of a given square.
     *
     * @param x the horizontal position.
     * @param y the vertical position.
     * @return the state for the given square.
     */
    public State getState(final int x, final int y) {
        return board[x][y];
    }

    /**
     * Sets the state of the given square.
     *
     * @param x the horizontal position.
     * @param y the vertical position.
     * @param state the state to set the square.
     */
    public void setState(final int x, final int y, final State state) {
        if (board[x][y] == State.EMPTY) {
            board[x][y] = state;
            updated();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double get(final int x, final int y) {
        switch (getState(x, y)) {
        case EMPTY:
            return 0;
        case OUH:
            return -1;
        case ECKS:
            return 1;
        default:
            throw new IllegalStateException("No vaild value set at " + x + ", "
                    + y);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean set(final int x, final int y, final double value) {
        if (value == 0) {
            return false;
        } else if (Math.floor(value) <= -1) {
            return false;
        } else if (Math.ceil(value) >= 1) {
            setState(x, y, State.OUH);
            return true;
        } else {
            throw new RuntimeException("Illegal value " + value);
        }
    }

    List<Listener> listeners = new ArrayList<Listener>();

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    private void updated() {
        for (Listener listener : listeners) {
            listener.updated();
        }
    }

    public interface Listener {
        void updated();
    }
}
