package org.simbrain.world.game;

import java.awt.GridLayout;

import javax.swing.SwingUtilities;

import org.simbrain.workspace.gui.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.world.game.tictactoe.TicTacToeGui;
import org.simbrain.world.game.tictactoe.TicTacToeModel;
import org.simbrain.world.game.tictactoe.TicTacToeGui.Listener;
import org.simbrain.world.game.tictactoe.TicTacToeModel.State;

/**
 * Desktop Component for board games.
 * 
 * @author Matt Watson
 */
public class GameDesktopComponent extends GuiComponent<GameComponent> {
    /** The game gui. */
    private final TicTacToeGui gui = new TicTacToeGui();
    
    /**
     * Creates a new desktop component.
     * 
     * @param frame parent frame
     * @param component the game component
     */
    public GameDesktopComponent(final GenericFrame frame, final GameComponent component) {
        super(frame, component);
        
        final TicTacToeModel model = component.getModel();
        
        gui.addListener(new Listener() {
            public void updated(final int x, final int y) {
                model.setState(x, y, State.ECKS);
//                gui.update(component.getModel());
//                frame.pack();
            }
        });
        
        model.addListener(new TicTacToeModel.Listener() {
            public void updated() {
                gui.update(model);
                frame.pack();
            }
        });
        
        setLayout(new GridLayout(1,1));
        add(gui.getPanel());
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                frame.pack();
            }
        });
    }

    /** Default serial id. */
    private static final long serialVersionUID = 1L;

    /**
     * Called when the desktop component is closing.
     */
    @Override
    protected void closing() {
        /* no implementation */
    }
}
