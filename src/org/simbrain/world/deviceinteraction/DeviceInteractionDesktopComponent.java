package org.simbrain.world.deviceinteraction;

import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.component_actions.CloseAction;
import org.simbrain.workspace.component_actions.OpenAction;
import org.simbrain.workspace.component_actions.SaveAction;
import org.simbrain.workspace.component_actions.SaveAsAction;
import org.simbrain.workspace.gui.GuiComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * @author Amanda Pandey <amanda.pandey@gmail.com>
 */
public class DeviceInteractionDesktopComponent extends
        GuiComponent<DeviceInteractionComponent> {

    /** Default height. */
    private static final int DEFAULT_HEIGHT = 250;

    /** Default width. */
    private static final int DEFAULT_WIDTH = 400;

    /** Menu Bar. */
    private JMenuBar menuBar = new JMenuBar();

    /** File menu for saving and opening world files. */
    private JMenu file = new JMenu("File");

    /** Edit menu Item. */
    private JMenu edit = new JMenu("Edit");

    /** Opens user preferences dialog. */
    private JMenuItem preferences = new JMenuItem("Preferences");

    /** Show dictionary. */
    private JMenuItem showDictionary = new JMenuItem("Show dictionary");

    /** Opens the help dialog for TextWorld. */
    private JMenu help = new JMenu("Help");

    /** Help menu item. */
    private JMenuItem helpItem = new JMenuItem("Display Help");

    /** The pane representing the text world. */
    private DeviceInteractionPanel panel;

    /** The text world. */
    private KeyboardWorld world;


    /**
     * Construct a workspace component.
     *
     * @param frame              the parent frame.
     * @param component the component to wrap.
     */
    public DeviceInteractionDesktopComponent(GenericFrame frame, DeviceInteractionComponent component) {
        super(frame, component);
        world = component.getWorld();
        JToolBar openSaveToolBar = new JToolBar();
        openSaveToolBar.add(new OpenAction(this));
        openSaveToolBar.add(new SaveAction(this));
        panel = new DeviceInteractionPanel(world, openSaveToolBar);
        this.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        addMenuBar();
        add(panel);
        frame.pack();

        // Force component to fill up parent panel
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Component component = e.getComponent();
                panel.setPreferredSize(new Dimension(component.getWidth(),
                        component.getHeight()));
                panel.revalidate();
            }
        });

    }

    /**
     * Adds menu bar to the top of TextWorldComponent.
     */
    private void addMenuBar() {

        // File Menu
        menuBar.add(file);
        file.add(new OpenAction(this));
        file.add(new SaveAction(this));
        file.add(new SaveAsAction(this));
        file.addSeparator();
        file.addSeparator();
        file.add(new CloseAction(this.getWorkspaceComponent()));

        showDictionary.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //TODO implement
            }
        });
        edit.add(showDictionary);
        edit.addSeparator();
        edit.add(preferences);
        menuBar.add(edit);

        // Help Menu
        menuBar.add(help);

        //TODO: create equivalent html for Device interaction
        ShowHelpAction helpAction = new ShowHelpAction(
                "Pages/Worlds/TextWorld/TextWorld.html");
        helpItem.setAction(helpAction);
        help.add(helpItem);

        getParentFrame().setJMenuBar(menuBar);
    }

    @Override
    protected void closing() {
        //no implementation
    }
}
