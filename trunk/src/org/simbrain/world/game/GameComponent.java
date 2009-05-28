package org.simbrain.world.game;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;
import org.simbrain.world.game.tictactoe.TicTacToeModel;

/**
 * Component type for board games.
 * 
 * @author Matt Watson
 */
public class GameComponent extends WorkspaceComponent<WorkspaceComponentListener> {
    /** The game model. */
    private final TicTacToeModel model = new TicTacToeModel();
    
    /**
     * the attributes that wraps the model to make it act as a producer
     * and a consumer.
     */
    private final GameAttributes attributes = new GameAttributes(this, model);
    
    /**
     * Creates a new game component.
     */
    public GameComponent() {
        super("Tic Tac Toe");
        
        setAttributeListingStyle(AttributeListingStyle.TOTAL);
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
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends Consumer> getConsumers() {
        return Collections.singleton(attributes);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends Producer> getProducers() {
        return Collections.singleton(attributes);
    }
}