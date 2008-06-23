package org.simbrain.plot;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeriesCollection;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.gui.CouplingMenuItem;
import org.simbrain.workspace.gui.CouplingMenus;
import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.workspace.gui.GenericFrame;

public class PlotDesktopComponent extends GuiComponent<PlotComponent> {

    private static final long serialVersionUID = 1L;

    /** Coupling menu item. Must be reset every time.  */
    JMenuItem couplingMenuItem;
    
    private final PlotComponent component;
    
    /**
     * Construct a new world panel.  Set up the toolbars.  Create an  instance of a world object.
     * @param ws the workspace associated with this frame
     */
    public PlotDesktopComponent(GenericFrame frame, PlotComponent component) {
        super(frame, component);
        this.component = component;
        setPreferredSize(new Dimension(500, 400));
    }

    /**
     * Initializes frame.
     */
    @Override
    public void postAddInit() {
        setLayout(new BorderLayout());
        setCouplingMenuItem();
        JMenu couplingMenu = new JMenu("Couplings");
        couplingMenu.addMenuListener(menuListener);
        couplingMenu.add(couplingMenuItem);
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(couplingMenu);
        getParentFrame().setJMenuBar(menuBar);
        // Add the series to your data set
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(component.getSeries());
        //         Generate the graph
        JFreeChart chart = ChartFactory.createXYLineChart(
            "Time series", // Title
            "iterations", // x-axis Label
            "value", // y-axis Label
            dataset, // Dataset
            PlotOrientation.VERTICAL, // Plot Orientation
            true, // Show Legend
            true, // Use tooltips
            false // Configure chart to generate URLs?
        );
        
        ChartPanel panel = new ChartPanel(chart);
        add("Center", panel);       
    }
    
//    private final ActionListener actionListener = new ActionListener() {
//        /**
//         * {@inheritDoc}
//         */
//        @SuppressWarnings("unchecked")
//        public void actionPerformed(final ActionEvent e) {
//
//            /* Handle Coupling wire-up */
//            CouplingMenuItem m = (CouplingMenuItem) e.getSource();
//            component.couple((ProducingAttribute<Double>) m.getProducingAttribute());
//        }
//    };
    
    /**
     * Set up the coupling menu.
     */
    private void setCouplingMenuItem() {
        couplingMenuItem = CouplingMenus.getProducerMenu(this.getWorkspaceComponent().getWorkspace(), component.getVariable());
        couplingMenuItem.setText("Set plotter source");
    }

    @Override
    public void closing() {
        // TODO Auto-generated method stub
    }

    @Override
    public void update() {
        
    }
    
    private final MenuListener menuListener = new MenuListener() {
        public void menuCanceled(MenuEvent arg0) {
            /* no implementation */
        }
    
        public void menuDeselected(MenuEvent arg0) {
            /* no implementation */
        }
    
        public void menuSelected(MenuEvent arg0) {
            setCouplingMenuItem();
        }
    };
}
