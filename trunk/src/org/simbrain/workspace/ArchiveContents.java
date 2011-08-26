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
package org.simbrain.workspace;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Instances of this class are used for building and reading the TOC of an
 * archive.
 *
 * This is the class that XStream serializes.
 *
 * @author Matt Watson
 */
class ArchiveContents {

    /** A map of all the components to their uris. */
    private transient Map<WorkspaceComponent, String> componentUris = new HashMap<WorkspaceComponent, String>();

    /** All of the components in the archive. */
    private List<ArchivedComponent> archivedComponents = new ArrayList<ArchivedComponent>();

    /** All of the couplings in the archive. */
    private List<ArchivedCoupling> archivedCouplings = new ArrayList<ArchivedCoupling>();

    /** The serializer for this archive. */
    private final WorkspaceComponentSerializer serializer;

    /** Reference to workspace used to serialize parameters in workspace. */
    private final Workspace workspaceParameters;


    /**
     * The component serializer for this archive.
     *
     * @param workspace references to parent workspace
     * @param serializer The component serializer for this archive.
     */
    ArchiveContents(final Workspace workspace,
            final WorkspaceComponentSerializer serializer) {
        this.workspaceParameters = workspace;
        this.serializer = serializer;
    }

    /**
     * Adds a new workspace Component to the archive.
     *
     * @param workspaceComponent The workspace component to add.
     * @return The component created for this WorkspaceComponent.
     */
    ArchivedComponent addComponent(final WorkspaceComponent workspaceComponent) {
        ArchivedComponent component = new ArchivedComponent(serializer, workspaceComponent);
        archivedComponents.add(component);
        componentUris.put(workspaceComponent, component.uri);
        return component;
    }

    /**
     * Returns an immutable list of the components in this archive.
     *
     * @return An immutable list of the components in this archive.
     */
    List<? extends ArchivedComponent> getArchivedComponents() {
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
    ArchivedComponent getArchivedComponent(final String uri) {
        for (ArchivedComponent component : archivedComponents) {
            if (component.uri.equals(uri)) {
                return component;
            }
        }

        return null;
    }

    /**
     * Adds a coupling to the archive.
     *
     * @param coupling The coupling to add.
     * @return The coupling entry in the archive.
     */
    ArchivedCoupling addCoupling(final Coupling<?> coupling) {
        ArchivedCoupling c = new ArchivedCoupling(this, coupling);
        archivedCouplings.add(c);
        return c;
    }

    /**
     * Represents the data used to store components in the archive.
     *
     * @author Matt Watson
     */
    static final class ArchivedComponent {

        /** The name of the class for the component. */
        private final String className;

        /** The name of the Component. */
        private final String name;

        /** The uri for the serialized component. */
        private final String uri;

        /** A unique id for the component in the archive. */
        private final int id;

        /**
         * A short String used to signify the format of the serialized
         * component.
         */
        private final String format;

        /**
         * The desktop component associated with the component (if there is
         * one).
         */
        private ArchivedDesktopComponent desktopComponent;

        /**
         * Creates a new Component entry.
         *
         * @param serializer The component serializer for the archive.
         * @param component The workspace component this entry represents.
         */
        private ArchivedComponent(final WorkspaceComponentSerializer serializer,
                final WorkspaceComponent component) {
            this.className = component.getClass().getCanonicalName();
            this.id = serializer.getId(component);
            this.name = component.getName();
            this.format = component.getDefaultFormat();
            this.uri = "components/" + id + '_' + name.replaceAll("\\s", "_")
                    + '.' + format;
        }

        /**
         * Adds a desktop component to this component entry.
         *
         * @param dc The desktop component to add an entry for.
         * @return The entry for the desktop component.
         */
        ArchivedDesktopComponent addDesktopComponent(
                final org.simbrain.workspace.gui.GuiComponent<?> dc) {
            return desktopComponent = new ArchivedDesktopComponent(this, dc);
        }

        /**
         * Class used to represent a desktop component in the archive.
         *
         * @author Matt Watson
         */
        static final class ArchivedDesktopComponent {

            /** The class for the desktop component. */
            private final String className;

            /** The uri for the serialized data. */
            private final String uri;

            /** The format for the serialized data. */
            private final String format;

            /**
             * Creates a new instance.
             *
             * @param parent The parent component entry.
             * @param dc The desktop component this instance represents.
             */
            private ArchivedDesktopComponent(final ArchivedComponent parent, final org.simbrain.workspace.gui.GuiComponent<?> dc) {
                this.className = dc.getClass().getCanonicalName();
                this.format = dc.getWorkspaceComponent().getDefaultFormat();
                this.uri = "guis/" + parent.id + '_' + parent.name.replaceAll("\\s", "_")
                    + '.' + format;
            }

            /**
             * @return the uri
             */
            public String getUri() {
                return uri;
            }
        }

        /**
         * @return the className
         */
        public String getClassName() {
            return className;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the uri
         */
        public String getUri() {
            return uri;
        }

        /**
         * @return the id
         */
        public int getId() {
            return id;
        }

        /**
         * @return the format
         */
        public String getFormat() {
            return format;
        }

        /**
         * @return the desktopComponent
         */
        public ArchivedDesktopComponent getDesktopComponent() {
            return desktopComponent;
        }
    }

    /**
     * Class used to represent a coupling in the archive.
     *
     * @author Matt Watson
     */
    static final class ArchivedCoupling {

        /** The source attribute for the coupling. */
        private final ArchivedAttribute archivedProducer;

        /** The target attribute for the coupling. */
        private final ArchivedAttribute archivedConsumer;

        /**
         * Creates a new instance.
         *
         * @param parent The parent archive.
         * @param coupling The coupling this instance represents.
         */
        ArchivedCoupling(final ArchiveContents parent,
                final org.simbrain.workspace.Coupling<?> coupling) {

            this.archivedProducer = new ArchivedAttribute(parent, coupling
                    .getProducer());
            this.archivedConsumer = new ArchivedAttribute(parent, coupling
                    .getConsumer());
        }

        /**
         * @return the archivedProducer
         */
        public ArchivedAttribute getArchivedProducer() {
            return archivedProducer;
        }

        /**
         * @return the archivedConsumer
         */
        public ArchivedAttribute getArchivedConsumer() {
            return archivedConsumer;
        }

    }

    /**
     * The class used to represent an attribute in the archive.
     *
     * @author Matt Watson
     * @author Jeff Yoshimi
     */
    public static final class ArchivedAttribute {

        /** The uri for the parent component of this attribute. */
        private final String parentComponentRef;

        /** The key that the component uses to identify the base object. */
        private final String baseObjectKey;

        /** The key that the component uses to identify the method name. */
        private final String methodBaseName;

        /** Key for data type. */
        private final Class<?> dataType;

        /** Argument data types. */
        private Class<?>[] argumentDataTypes;

        /** Argument values. */
        private Object[] argumentValues;

        /** Description. */
        private final String description;

        /**
         * Creates a new instance.
         *
         * @param parent The parent archive.
         * @param attribute The attribute this instance represents.
         */
        ArchivedAttribute(final ArchiveContents parent,
                final Attribute attribute) {

            WorkspaceComponent comp = attribute.getParentComponent();
            this.parentComponentRef = parent.componentUris.get(comp);
            this.baseObjectKey = comp.getKeyFromObject(attribute
                    .getBaseObject());
            this.methodBaseName = attribute.getMethodName();
            this.argumentDataTypes = attribute.getArgumentDataTypes();
            this.argumentValues = attribute.getArgumentValues();
            this.dataType = attribute.getDataType();
            this.description = attribute.getDescription();

        }

        /**
         * @return the parentComponentRef
         */
        public String getParentRef() {
            return parentComponentRef;
        }

        /**
         * @return the parentComponentRef
         */
        public String getParentComponentRef() {
            return parentComponentRef;
        }

        /**
         * @return the baseObjectKey
         */
        public String getBaseObjectKey() {
            return baseObjectKey;
        }

        /**
         * @return the methodBaseName
         */
        public String getMethodBaseName() {
            return methodBaseName;
        }

        /**
         * @return the dataType
         */
        public Class<?> getDataType() {
            return dataType;
        }

        /**
         * @return the description
         */
        public String getDescription() {
            return description;
        }

        /**
         * @return the argumentDataTypes
         */
        public Class<?>[] getArgumentDataTypes() {
            return argumentDataTypes;
        }

        /**
         * @return the argumentValues
         */
        public Object[] getArgumentValues() {
            return argumentValues;
        }
    }

    /**
     * Writes this instance to XML.
     *
     * @param stream
     *            The stream to write to.
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

        xstream.omitField(ArchiveContents.class, "serializer");
        xstream.omitField(ArchivedComponent.class, "serializer");
        xstream.omitField(ArchivedCoupling.class, "serializer");
        xstream.omitField(ArchivedComponent.class, "data");
        xstream.omitField(ArchivedComponent.ArchivedDesktopComponent.class, "data");
        xstream.omitField(Workspace.class, "LOGGER");
        xstream.omitField(Workspace.class, "manager");
        xstream.omitField(Workspace.class, "componentList");
        xstream.omitField(Workspace.class, "workspaceChanged");
        xstream.omitField(Workspace.class, "currentDirectory");
        xstream.omitField(Workspace.class, "currentFile");
        xstream.omitField(Workspace.class, "updator");
        xstream.omitField(Workspace.class, "listeners");
        xstream.omitField(Workspace.class, "componentNameIndices");
        xstream.omitField(Workspace.class, "updatorLock");
        xstream.omitField(Workspace.class, "componentLock");

        xstream.alias("Workspace", ArchiveContents.class);
        xstream.alias("Component", ArchivedComponent.class);
        xstream.alias("Coupling", ArchivedCoupling.class);
        xstream.alias("DesktopComponent", ArchivedComponent.ArchivedDesktopComponent.class);

        xstream.addImplicitCollection(ArchiveContents.class, "components", ArchivedComponent.class);
        xstream.addImplicitCollection(ArchiveContents.class, "couplings", ArchivedCoupling.class);
        xstream.addImplicitCollection(ArchivedComponent.class, "desktopComponents");

        return xstream;
    }

    /**
     * @return the workspaceParameters
     */
    public Workspace getWorkspaceParameters() {
        return workspaceParameters;
    }
}