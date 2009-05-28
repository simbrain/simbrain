package org.simbrain.world.game;

/**
 * base class for a simple square board game.
 * 
 * @author Matt Watson
 */
public abstract class GameModel {
    /** the size (width and height). */
    private final int size;
    /** the name of the game. */
    private String name;
    
    /**
     * Creates a new instance with the given name and size.
     * 
     * @param name the name of the game.
     * @param size the size of the board.
     */
    protected GameModel(final String name, final int size) {
        this.name = name;
        this.size = size;
    }
    
    /**
     * Returns the size of the board.
     * 
     * @return the size of the board.
     */
    public int size() {
        return size;
    }
    
    /**
     * Returns the name of the game.
     * 
     * @return the name of the game.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the value of the cell at the given coordinates.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * 
     * @return the value of the given cell.
     */
    public abstract double get(int x, int y);
    
    /**
     * Sets the value of the cell at the given coordinates.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * @param value the value set
     * 
     * @return whether the move is allowed.
     */
    public abstract boolean set(int x, int y, double value);
}
