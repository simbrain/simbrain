package org.simbrain.plot.pixelplot;

import org.simbrain.plot.actions.PlotActionManager;
import org.simbrain.util.SwingUtilsKt;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class PixelPlotDesktopComponent extends DesktopComponent<PixelPlotComponent> {

    /**
     * If true show grid lines.
     */
    private boolean showGridLines = true;

    /**
     * Displays the {@link PixelPlot}
     */
    private PixelPlotPanel pixelPlotPanel = new PixelPlotPanel();

    private PlotActionManager actionManager;

    /**
     * Construct a new PixelDisplayDesktopComponent GUI.
     *
     * @param frame     The frame in which to place GUI elements.
     * @param component The PixelDisplayComponent to interact with.
     */
    public PixelPlotDesktopComponent(GenericFrame frame, PixelPlotComponent component) {
        super(frame, component);
        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, pixelPlotPanel);
        getWorkspaceComponent().getPixelPlot().getEvents().getImageUpdate().on(this::repaint);

        actionManager = new PlotActionManager(this);

        createAttachMenuBar();
    }

    private void createAttachMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        for (Action action : actionManager.getOpenSavePlotActions()) {
            fileMenu.add(action);
        }
        fileMenu.addSeparator();
        fileMenu.add(SimbrainDesktop.INSTANCE.getActionManager().createRenameAction(this));
        fileMenu.addSeparator();
        fileMenu.add(SimbrainDesktop.INSTANCE.getActionManager().createCloseAction(this));

        JMenu editMenu = new JMenu("Edit");
        JMenuItem preferences = new JMenuItem("Preferences...");
        preferences.addActionListener(e -> {
            SwingUtilsKt.display(SwingUtilsKt.createEditorDialog(getWorkspaceComponent().getPixelPlot()));
        });
        editMenu.add(preferences);
        JMenuItem resizePixelMatrix = new JMenuItem("Resize pixel matrix...");
        resizePixelMatrix.addActionListener(e -> {
            ResizePixelPlotDialog dialog = new ResizePixelPlotDialog(getWorkspaceComponent().getPixelPlot());
            dialog.setVisible(true);
        });
        editMenu.add(resizePixelMatrix);


        JMenu helpMenu = new JMenu("Help");
        ShowHelpAction helpAction = new ShowHelpAction("Pages/Plot/bar_chart.html");
        JMenuItem helpItem = new JMenuItem(helpAction);
        helpMenu.add(helpItem);

        bar.add(fileMenu);
        bar.add(editMenu);
        bar.add(helpMenu);

        getParentFrame().setJMenuBar(bar);
    }

    private class PixelPlotPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            BufferedImage currentImage = getWorkspaceComponent().getPixelPlot().getImage();
            if (currentImage == null) {
                return;
            }
            graphics.drawImage(currentImage, 0, 0, getWidth(), getHeight(), this);

            // Draw grid lines
            if (showGridLines) {
                // Don't draw gridlines in cases where they would obscure the image itself
                // That is, only draw if image is zoomed in more than 10 times
                if (getWidth() > currentImage.getWidth() * 10 && getHeight() > currentImage.getHeight() * 10) {
                    graphics.setColor(Color.GRAY);
                    for (int i = 0; i < currentImage.getWidth(); i++) {
                        graphics.drawLine(
                                (int) ((double) i / currentImage.getWidth() * getWidth()),
                                0,
                                (int) ((double) i / currentImage.getWidth() * getWidth()),
                                getHeight()
                        );
                    }
                    for (int i = 0; i < currentImage.getHeight(); i++) {
                        graphics.drawLine(
                                0,
                                (int) ((double) i / currentImage.getHeight() * getHeight()),
                                getWidth(),
                                (int) ((double) i / currentImage.getHeight() * getHeight())
                        );
                    }
                }
            }
        }
    }

}
