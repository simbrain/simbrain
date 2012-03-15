package org.simbrain.world.game;

import java.io.OutputStream;

import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.game.tictactoe.TicTacToeModel;

/**
 * Component type for board games.
 * 
 * @author Matt Watson
 */
public class GameComponent extends WorkspaceComponent {
    /** The game model. */
    private final TicTacToeModel model = new TicTacToeModel();

    /**
     * The attributes that wrap the model to make it act as a producer
     * and a consumer.
     */
    private final GameAttributes attributes = new GameAttributes(this, model);

    /**
     * Creates a new game component.
     */
    public GameComponent() {
        super("Tic Tac Toe");
        //addConsumer(attributes);
    }

    /**
     * Returns the game model.
     * 
     * @return The game model.
     */
    public TicTacToeModel getModel() {
        return model;
    }
    
    /**
     * Called when the component is closed.
     */
    @Override
    protected void closing() {
        /* no implementation */
    }

    /**
     * Saves the component to the given stream.
     * 
     * @param output the output stream.
     * @param format the format type.
     */
    @Override
    public void save(final OutputStream output, final String format) {
        throw new UnsupportedOperationException();
    }
    
        
}