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

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Base class for chart model classes. Maintains listeners.
 *
 * TODO: Some charts have update happening inside the consumer setVal function.
 * This makes it impossible to "turn the component off". It's also a bit
 * inefficient, since a graphics update fired every time. See projection for a
 * perhaps better model.
 */
public class ChartModel {

    /** Listeners on this chart. */
    private List<ChartListener> listenerList = new ArrayList<ChartListener>();

    /** Settings Listeners on this chart. */
    private List<ChartSettingsListener> settingsListenerList = new ArrayList<ChartSettingsListener>();

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
     * Fire  settings updated event.
     */
    public void fireSettingsChanged() {
        for (ChartSettingsListener listener : settingsListenerList) {
            listener.chartSettingsUpdated();
        }
    }

    /**
     * Fire data source added event.
     *
     * @param index index of added data source
     */
    public void fireDataSourceAdded(final int index) {
        if (listenerList == null) {
            listenerList = new ArrayList<ChartListener>();
        }
        for (ChartListener listener : listenerList) {
            listener.dataSourceAdded(index);
        }
    }

    /**
     * Fire data source removed event.
     *
     * @param index index of removed data source
     */
    public void fireDataSourceRemoved(final int index) {
        for (ChartListener listener : listenerList) {
            listener.dataSourceRemoved(index);
        }
    }

    /**
     * Creates an xtream object with relevant fields omitted.
     * @return
     */
    public static XStream getXStream() {
        final XStream xstream = new XStream(new DomDriver());
        xstream.omitField(ChartModel.class, "listenerList");
        xstream.omitField(ChartModel.class, "settingsListenerList");
        return xstream;
    }
}
