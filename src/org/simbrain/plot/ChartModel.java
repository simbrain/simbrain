/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.plot;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.simbrain.util.Utils;
import org.simbrain.workspace.AttributeContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base class for chart model classes. Maintains listeners.
 * <p>
 * TODO: Some charts have update happening inside the consumer setVal function.
 * This makes it impossible to "turn the component off". It's also a bit
 * inefficient, since a graphics update fired every time. See projection for a
 * perhaps better model.
 */
public class ChartModel implements AttributeContainer {

    //TODO: Consider getting rid of this.  This type of thing may only end up
    // being done for histogram

    /**
     * Listeners on this chart.
     */
    private transient List<ChartListener> listenerList = new ArrayList<ChartListener>();

    /**
     * Settings Listeners on this chart.
     */
    private transient List<ChartSettingsListener> settingsListenerList = new ArrayList<ChartSettingsListener>();

    /**
     * Creates an xtream object for serializing the model.
     */
    public static XStream getXStream() {
        XStream xstream = Utils.getSimbrainXStream();
        return xstream;
    }

    /**
     * Add a data source to the chart model.
     */
    public ChartDataSource addDataSource(String description) {
        return null;
    }

    /**
     * Remove the data source from the chart model.
     */
    public void removeDataSource(ChartDataSource source) {
    }

    /**
     * Get the data source with the specified description, if it exists.
     */
    public Optional<? extends ChartDataSource> getDataSource(String description) {
        return Optional.empty();
    }

    /**
     * Add a chart listener.
     *
     * @param listener listener to add.
     */
    public void addListener(ChartListener listener) {
        if (listenerList == null) {
            listenerList = new ArrayList<ChartListener>();
        }
        listenerList.add(listener);
    }

    /**
     * Add a ChartSettings listener.
     *
     * @param listener listener to add.
     */
    public void addChartSettingsListener(ChartSettingsListener listener) {
        if (settingsListenerList == null) {
            settingsListenerList = new ArrayList<ChartSettingsListener>();
        }

        settingsListenerList.add(listener);
    }

    /**
     * Fire settings updated event.
     */
    public void fireSettingsChanged() {
        for (ChartSettingsListener listener : settingsListenerList) {
            listener.chartSettingsUpdated(this);
        }
    }

    /**
     * Fire data source added event.
     *
     * @param source The added data source.
     */
    public void fireDataSourceAdded(ChartDataSource source) {
        for (ChartListener listener : listenerList) {
            listener.dataSourceAdded(source);
        }
    }

    /**
     * Fire data source removed event.
     *
     * @param source The removed data source.
     */
    public void fireDataSourceRemoved(ChartDataSource source) {
        for (ChartListener listener : listenerList) {
            listener.dataSourceRemoved(source);
        }
    }

}
