package org.simbrain.plot;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.simbrain.util.Utils;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.gui.CouplingMenuItem;
import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;

public class PlotDesktopComponent extends DesktopComponent<PlotComponent> implements ActionListener, MenuListener  {

    /** Time series. */
    XYSeries series = new XYSeries("Time series");

    /** Consumer list. */
    private ArrayList<Consumer> consumers= new ArrayList<Consumer>();

    /** Coupling list. */
    private ArrayList<Coupling> couplings = new ArrayList<Coupling>();

    /** Coupling menu item. Must be reset every time.  */
    JMenuItem couplingMenuItem;

    private final PlotComponent component;
    
    /**
     * Construct a new world panel.  Set up the toolbars.  Create an  instance of a world object.
     * @param ws the workspace associated with this frame
     */
    public PlotDesktopComponent(PlotComponent component) {
        super(component);
        this.component = component;
        init();
    }


    /**
     * Initializes frame.
     */
    public void init() {

        consumers.add(new Variable(component));

        getContentPane().setLayout(new BorderLayout());
        setCouplingMenuItem();
        JMenu couplingMenu = new JMenu("Couplings");
        couplingMenu.addMenuListener(this);
        couplingMenu.add(couplingMenuItem);
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(couplingMenu);
        setJMenuBar(menuBar);
        //         Add the series to your data set
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        //         Generate the graph
        JFreeChart chart = ChartFactory.createXYLineChart("Time series", // Title
                "iterations", // x-axis Label
                "value", // y-axis Label
                dataset, // Dataset
                PlotOrientation.VERTICAL, // Plot Orientation
                true, // Show Legend
                true, // Use tooltips
                false // Configure chart to generate URLs?
            );
        getContentPane().add("Center", new ChartPanel(chart));
    }
    
    /**
     * Responds to actions performed.
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {

        // Handle Coupling wireup
        if (e.getSource() instanceof CouplingMenuItem) {
            CouplingMenuItem m = (CouplingMenuItem) e.getSource();
            Coupling coupling = new Coupling(m.getProducingAttribute(), this.getConsumers().get(0).getDefaultConsumingAttribute());
            getCouplings().clear();
            getCouplings().add(coupling);
        }
    }


    /**
     * Set up the coupling menu.
     */
    private void setCouplingMenuItem() {
        couplingMenuItem = SimbrainDesktop.getInstance().getProducerMenu(this);
        couplingMenuItem.setText("Set plotter source");
    }

    int time = 0;
    
    public void setValue(double value) {
        series.add(time++, value);
    }

    @Override
    public String getFileExtension() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isChangedSinceLastSave() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void save(File saveFile) {
        // TODO Auto-generated method stub
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }

    
    /**
     * {@inheritDoc}
     */
    public List<Consumer> getConsumers() {
        return consumers;
    }

    /**
     * {@inheritDoc}
     */
    public List<Coupling> getCouplings() {
        return couplings;
    }

    /**
     * No producers.
     */
    public List<Producer> getProducers() {
        return null;
    }

    @Override
    public void open(File openFile) {
        // TODO Auto-generated method stub
    }


    public void menuCanceled(MenuEvent arg0) {
        // TODO Auto-generated method stub
    }


    public void menuDeselected(MenuEvent arg0) {
        // TODO Auto-generated method stub
    }

    public void menuSelected(MenuEvent arg0) {
        setCouplingMenuItem();
    }

}
