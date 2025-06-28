package org.simbrain.console;

import org.simbrain.workspace.WorkspaceComponent;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Workspace component corresponding to a beanshell window.
 */
public class ConsoleComponent extends WorkspaceComponent {

    public ConsoleComponent(String name) {
        super(name);
    }

    /**
     * Opens a saved component. There isn't much to do here since currently
     * there is nothing to persist with a console. This just ensures that a
     * component is created and (in the gui) presented.
     *
     * @param input  stream
     * @param name   name of file
     * @param format format
     * @return component to be opened
     */
    public static ConsoleComponent open(InputStream input, final String name, final String format) {
        return new ConsoleComponent(name);
    }

    @Override
    public void save(OutputStream output, final String format) {
        // TODO implement
    }

}
