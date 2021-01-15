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
package org.simbrain.workspace.serialization;

import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.gui.DesktopComponent;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Class used to assist with deserializing workspace components.
 *
 * @author Matt Watson
 */
public class WorkspaceComponentDeserializer {

    /**
     * A map used to retrieve workspace components given their uris.
     */
    private final Map<String, WorkspaceComponent> componentKeys = new HashMap<String, WorkspaceComponent>();

    /**
     * Returns the workspace component associated with the given uri.
     *
     * @param uri The uri for the component to retrieve.
     * @return The component for the uri.
     */
    WorkspaceComponent getComponent(final String uri) {
        return componentKeys.get(uri);
    }

    /**
     * Deserializes a workspace component using the information from the
     * provided component and input stream.
     *
     * @param archivedComponent The component entry from the archive contents.
     * @param input             The input stream to read data from.
     * @return The deserialized WorkspaceComponent.
     */
    WorkspaceComponent deserializeWorkspaceComponent(ArchivedWorkspaceComponent archivedComponent, InputStream input) throws ReflectiveOperationException {
        Class<?> componentClass = Class.forName(archivedComponent.getClassName());
        WorkspaceComponent wc = deserializeWorkspaceComponent(componentClass, archivedComponent.getName(), input, null);
        componentKeys.put(archivedComponent.getUri(), wc);
        wc.setChangedSinceLastSave(false);
        return wc;
    }

    /**
     * Deserialized a component for the given class, input and input format.
     *
     * @param componentClass the class of the component
     * @param name           the name of the component
     * @param input          the input stream
     * @param format         the format of the data
     * @return a new component
     */
    public static WorkspaceComponent deserializeWorkspaceComponent(Class<?> componentClass, String name, InputStream input, String format) throws ReflectiveOperationException {
        Method method = componentClass.getMethod("open", InputStream.class, String.class, String.class);
        Object obj = method.invoke(null, input, name, format);
        if (obj instanceof WorkspaceComponent) {
            WorkspaceComponent component = (WorkspaceComponent) obj;
            component.setChangedSinceLastSave(false);
            return component;
        } else {
            throw new ReflectiveOperationException("Incompatible open method return type in class " + componentClass);
        }
    }

    /**
     * Deserializes a desktop component given a class, input stream and name.
     *
     * @param className The class name for the DesktopComponent
     * @param component The desktop component entry for the desktop component.
     * @param input     The input stream.
     * @param name      The name of the desktop component.
     * @return The deserialized desktop component.
     */
    DesktopComponent<?> deserializeDesktopComponent(String className, WorkspaceComponent component, InputStream input, String name) throws ReflectiveOperationException {
        Class<?> clazz = Class.forName(className);
        Method method = clazz.getMethod("open", WorkspaceComponent.class, InputStream.class, String.class);
        return (DesktopComponent<?>) method.invoke(null, component, input, name);
    }
}
