package org.simbrain.workspace;

/**
 * Helper class for persisting workspace components.  Objects of this class
 * are easily persisted, and can be used to recreate the workspace.
 */
public class WorkspaceComponentProxy {

    String path;
    String name;
    Class componentClass;
    int x;
    int y;
    int height;
    int width;

    public WorkspaceComponentProxy(String path, String name, Class componentClass, int x, int y, int height, int width) {
        this.path = path;
        this.name = name;
        this.componentClass = componentClass;
        this.x = x;
        this.y = y;
        this.height = height;
        this.width = width;
    }
    /**
     * @return the componentClass
     */
    public Class getComponentClass() {
        return componentClass;
    }
    /**
     * @param componentClass the componentClass to set
     */
    public void setComponentClass(Class componentClass) {
        this.componentClass = componentClass;
    }
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }
    /**
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }
    /**
     * @return the x
     */
    public int getX() {
        return x;
    }
    /**
     * @param x the x to set
     */
    public void setX(int x) {
        this.x = x;
    }
    /**
     * @return the y
     */
    public int getY() {
        return y;
    }
    /**
     * @param y the y to set
     */
    public void setY(int y) {
        this.y = y;
    }
    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }
    /**
     * @param height the height to set
     */
    public void setHeight(int height) {
        this.height = height;
    }
    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }
    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }
}