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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.updater.*;

import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Instances of this class are used for building and reading the TOC of an
 * archive.
 * <p>
 * This is the class that XStream serializes.
 *
 * @author Matt Watson
 * @author Jeff Yoshimi
 */
@XStreamAlias("ArchivedWorkspace")
class ArchivedWorkspace {

    /**
     * A map of all the components to their uris.
     */
    private transient Map<WorkspaceComponent, String> componentUris = new HashMap<WorkspaceComponent, String>();

    /**
     * The serializer for this archive.
     */
    private final transient WorkspaceComponentSerializer serializer;

    /**
     * All of the components in the archive.
     */
    private List<ArchivedWorkspaceComponent> archivedComponents = new ArrayList<ArchivedWorkspaceComponent>();

    /**
     * All of the couplings in the archive.
     */
    private List<ArchivedCoupling> archivedCouplings = new ArrayList<ArchivedCoupling>();

    /**
     * All of the updateactions in the archive.
     */
    private List<ArchivedUpdateAction> archivedActions = new ArrayList<ArchivedUpdateAction>();

    /**
     * Reference to workspace used to serialize parameters in workspace.
     */
    private final Workspace workspaceParameters;

    /**
     * The component serializer for this archive.
     *
     * @param workspace  references to parent workspace
     * @param serializer The component serializer for this archive.
     */
    ArchivedWorkspace(Workspace workspace, WorkspaceComponentSerializer serializer) {
        this.workspaceParameters = workspace;
        this.serializer = serializer;
    }

    /**
     * Adds a new workspace Component to the archive.
     *
     * @param workspaceComponent The workspace component to add.
     * @return The component created for this WorkspaceComponent.
     */
    ArchivedWorkspaceComponent addComponent(WorkspaceComponent workspaceComponent) {
        ArchivedWorkspaceComponent component = new ArchivedWorkspaceComponent(serializer, workspaceComponent);
        archivedComponents.add(component);
        componentUris.put(workspaceComponent, component.uri);
        return component;
    }

    /**
     * Adds an update action the archive.
     *
     * @param action the action to archive.
     */
    void addUpdateAction(final UpdateAction action) {
        archivedActions.add(getArchivedAction(action));
    }

    /**
     * Creates the archived action given the "real" update action.
     *
     * @param action the real update action.
     * @return the archived action.
     */
    private ArchivedUpdateAction getArchivedAction(UpdateAction action) {
        String component_id = null;
        String coupling_id = null;
        // Get a component id if this is an update component action
        if (action instanceof UpdateComponent) {
            component_id = componentUris.get(((UpdateComponent) action).getComponent());
        }
        // Get a coupling id, if this is coupling action
        if (action instanceof UpdateCoupling) {
            Coupling<?> coupling = ((UpdateCoupling) action).getCoupling();
            if (coupling != null) {
                coupling_id = coupling.getId();
            } else {
                System.err.println("Invalid coupling action found while saving:" + action.getDescription());
            }
        }
        // Create and return the archived action
        return new ArchivedUpdateAction(action, component_id, coupling_id);
    }

    /**
     * Returns an immutable list of the components in this archive.
     *
     * @return An immutable list of the components in this archive.
     */
    List<? extends ArchivedWorkspaceComponent> getArchivedComponents() {
        if (archivedComponents == null) {
            archivedComponents = Collections.emptyList();
        }

        return Collections.unmodifiableList(archivedComponents);
    }

    /**
     * Returns an immutable list of the couplings in this archive.
     *
     * @return An immutable list of the couplings in this archive.
     */
    List<? extends ArchivedCoupling> getArchivedCouplings() {
        if (archivedCouplings == null) {
            archivedCouplings = Collections.emptyList();
        }
        return Collections.unmodifiableList(archivedCouplings);
    }

    /**
     * Returns the component associated with the uri.
     *
     * @param uri The uri for the component.
     * @return The component associated with the uri.
     */
    ArchivedWorkspaceComponent getArchivedComponent(final String uri) {
        for (ArchivedWorkspaceComponent component : archivedComponents) {
            if (component.uri.equals(uri)) {
                return component;
            }
        }
        return null;
    }

    /**
     * Create a "real" update action from an archived update action.
     *
     * @param workspace             parent workspace in which to place the new action
     * @param componentDeserializer used to get the workspace component
     *                              corresponding to a workspace component id
     * @param archivedAction        the archived action to convert into a real action
     * @return the "real" update action
     */
    UpdateAction createUpdateAction(Workspace workspace, WorkspaceComponentDeserializer componentDeserializer, ArchivedUpdateAction archivedAction) {
        // Use reflection to create the update action, based on what type of action was archived.
        // For actions whose constructors require components or couplings, the archived ids are
        // used to find the component or coupling.
        UpdateAction serializedAction = archivedAction.getUpdateAction();
        UpdateAction action = null;
        if (serializedAction instanceof UpdateComponent) {
            try {
                WorkspaceComponent component = componentDeserializer.getComponent(archivedAction.getComponentId());
                Class<? extends UpdateAction> type = serializedAction.getClass();
                Constructor<? extends UpdateAction> constructor = type.getConstructor(WorkspaceUpdater.class, WorkspaceComponent.class);
                action = constructor.newInstance(workspace.getUpdater(), component);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (serializedAction instanceof UpdateAllAction) {
            try {
                Class<? extends UpdateAction> type = serializedAction.getClass();
                action = type.getConstructor(WorkspaceUpdater.class).newInstance(workspace.getUpdater());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (serializedAction instanceof UpdateActionCustom) {
            try {
                String script = ((UpdateActionCustom) archivedAction.getUpdateAction()).getScriptString();
                Class<? extends UpdateAction> type = serializedAction.getClass();
                action = type.getConstructor(WorkspaceUpdater.class, String.class).newInstance(workspace.getUpdater(), script);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (serializedAction instanceof UpdateCoupling) {
            try {
                String id = archivedAction.getCouplingId();
                Coupling<?> coupling = workspace.getCouplingManager().getCoupling(id);
                Class<? extends UpdateAction> type = serializedAction.getClass();
                action = type.getConstructor(Coupling.class).newInstance(coupling);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (serializedAction instanceof SynchronizedTaskUpdateAction) {
            return workspace.getUpdater().getSyncUpdateAction();
        }
        return action;
    }

    /**
     * Adds a coupling to the archive.
     *
     * @param coupling The coupling to add.
     * @return The coupling entry in the archive.
     */
    void addCoupling(ArchivedCoupling coupling) {
        archivedCouplings.add(coupling);
    }

    /**
     * @return the archivedActions
     */
    public List<ArchivedUpdateAction> getArchivedActions() {
        return archivedActions;
    }

    /**
     * @return the workspaceParameters
     */
    public Workspace getWorkspaceParameters() {
        return workspaceParameters;
    }

    /**
     * Writes this instance to XML.
     *
     * @param stream The stream to write to.
     */
    void toXml(final OutputStream stream) {
        xstream().toXML(this, stream);
    }

    /**
     * Returns the XStream instance used to serialize and deserialize instances
     * of this class.
     *
     * @return An XStream instance.
     */
    static XStream xstream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.ignoreUnknownElements();
        xstream.processAnnotations(ArchivedWorkspace.class);
        xstream.processAnnotations(ArchivedWorkspaceComponent.class);
        xstream.processAnnotations(ArchivedCoupling.class);
        xstream.processAnnotations(ArchivedAttribute.class);
        xstream.processAnnotations(ArchivedUpdateAction.class);
        return xstream;
    }

}