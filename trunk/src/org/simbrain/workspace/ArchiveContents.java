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
 * Object used for building and reading the TOC of an
 * archive.
 * 
 * @author Matt Watson
 */
class ArchiveContents {
    /** A map of all the components to their uris. */
    private transient Map<WorkspaceComponent<?>, String> componentUris
        = new HashMap<WorkspaceComponent<?>, String>();
    /** All of the components in the archive. */
    private List<Component> components = new ArrayList<Component>();
    /** All of the couplings in the archive. */
    private List<Coupling> couplings = new ArrayList<Coupling>();
    /** The serializer for this archive. */
    private final WorkspaceComponentSerializer serializer;
    
    /**
     * The component serializer for this archive.
     * 
     * @param serializer The component serializer for this archive.
     */
    ArchiveContents(final WorkspaceComponentSerializer serializer) {
        this.serializer = serializer;
    }
    
    /**
     * Adds a new workspace Component to the archive.
     * 
     * @param workspaceComponent The workspace component to add.
     * @return The component created for this WorkspaceComponent.
     */
    Component addComponent(final WorkspaceComponent<?> workspaceComponent) {
        Component component = new Component(serializer, workspaceComponent);
        components.add(component);
        
        componentUris.put(workspaceComponent, component.uri);
        
        return component;
    }
    
    /**
     * Returns an immutable list of the components in this archive.
     * 
     * @return An immutable list of the components in this archive.
     */
    List<? extends Component> getComponents() {
        if (components == null) components = Collections.emptyList();
        
        return Collections.unmodifiableList(components);
    }
    
    /**
     * Returns an immutable list of the couplings in this archive.
     * 
     * @return An immutable list of the couplings in this archive.
     */
    List<? extends Coupling> getCouplings() {
        if (couplings == null) couplings = Collections.emptyList();
        return Collections.unmodifiableList(couplings);
    }
    
    /**
     * Returns the component associated with the uri.
     * 
     * @param uri The uri for the component.
     * @return The component associated with the uri.
     */
    Component getComponent(final String uri) {
        for (Component component : components) {
            if (component.uri.equals(uri)) return component;
        }
        
        return null;
    }
    
    /**
     * Adds a coupling to the archive.
     * 
     * @param coupling The coupling to add.
     * @return The coupling entry in the archive.
     */
    Coupling addCoupling(final org.simbrain.workspace.Coupling<?> coupling) {
        Coupling c = new Coupling(this, coupling);
        
        couplings.add(c);
        
        return c;
    }
    
    /**
     * Represents the data used to store components in the archive.
     * 
     * @author Matt Watson
     */
    static final class Component {
        /** The name of the class for the component. */
        final String className;
        /** The name of the Component. */
        final String name;
        /** The uri for the serialized component. */
        final String uri;
        /** A unique id for the component in the archive. */
        final int id;
        /** A short String used to signify the format of the serialized component. */
        final String format;
        /** The desktop component associated with the component (if there is one). */
        DesktopComponent desktopComponent;
        
        /**
         * Creates a new Component entry.
         * 
         * @param serializer The component serializer for the archive.
         * @param component The workspace component this entry represents.
         */
        private Component(final WorkspaceComponentSerializer serializer,
                final WorkspaceComponent<?> component) {
            this.className = component.getClass().getCanonicalName();
            this.id = serializer.getId(component);
            this.name = component.getName();
            this.format = component.getDefaultFormatKey();
            this.uri = "components/" + id + '_' + name.replaceAll("\\s", "_") + '.' + format;
        }
        
        /**
         * Adds a desktop componet to this component entry.
         * 
         * @param dc The desktop component to add an entry for.
         * @return The entry for the desktop component.
         */
        DesktopComponent addDesktopComponent(
                final org.simbrain.workspace.gui.DesktopComponent<?> dc) {
            return desktopComponent = new DesktopComponent(this, dc);
        }
        
        /**
         * Class used to represent a desktop component in the archive.
         * 
         * @author Matt Watson
         */
        static final class DesktopComponent {
            /** The class for the desktop component. */
            final String className;
            /** The uri for the serialized data. */
            final String uri;
            /** The format for the serialized data. */
            final String format;
            
            /**
             * Creates a new instance.
             * 
             * @param parent The parent component entry.
             * @param dc The desktop component this instance represents.
             */
            private DesktopComponent(final Component parent,
                    final org.simbrain.workspace.gui.DesktopComponent<?> dc) {
                this.className = dc.getClass().getCanonicalName();
                this.format = dc.getDefaultFormatKey();
                this.uri = "guis/" + parent.id + '_' + parent.name.replaceAll("\\s", "_")
                    + '.' + format;
            }
        }
    }
    
    /**
     * Class used to represent a coupling in the archive.
     * 
     * @author Matt Watson
     */
    static final class Coupling {
        /** The source attribute for the coupling. */
        final Attribute source;
        /** The target attribute for the coupling. */
        final Attribute target;
        
        /**
         * Creates a new instance.
         * 
         * @param parent The parent archive.
         * @param coupling The coupling this instance represents.
         */
        Coupling(final ArchiveContents parent, final org.simbrain.workspace.Coupling<?> coupling) {
            ProducingAttribute<?> producing = coupling.getProducingAttribute();
            ConsumingAttribute<?> consuming = coupling.getConsumingAttribute();
            
            this.source = new Attribute(parent, producing);
            this.target = new Attribute(parent, consuming);
        }
        
        /**
         * The class used to represent an attribute in the archive.
         * 
         * @author Matt Watson
         */
        static final class Attribute {
            /** The uri for the parent component of this attribute. */
            final String uri;
            /** The key that the component uses to identify the attribute. */
            final String key;
            
            /**
             * Creates a new instance.
             * 
             * @param parent The parent archive.
             * @param attribute The attribute this instance represents.
             */
            Attribute(final ArchiveContents parent,
                    final org.simbrain.workspace.Attribute attribute) {
                WorkspaceComponent<?> comp = attribute.getParent().getParentComponent();
                
                this.uri = parent.componentUris.get(comp);
                this.key = comp.getKeyForAttribute(attribute);
            }
        }
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
        
        xstream.omitField(ArchiveContents.class, "serializer");
        xstream.omitField(Component.class, "serializer");
        xstream.omitField(Coupling.class, "serializer");
        xstream.omitField(Component.class, "data");
        xstream.omitField(Component.DesktopComponent.class, "data");
                    
        xstream.alias("Workspace", ArchiveContents.class);
        xstream.alias("Component", Component.class);
        xstream.alias("Coupling", Coupling.class);
        xstream.alias("DesktopComponent", Component.DesktopComponent.class);
        
        xstream.addImplicitCollection(ArchiveContents.class, "components", Component.class);
        xstream.addImplicitCollection(ArchiveContents.class, "couplings", Coupling.class);
        xstream.addImplicitCollection(Component.class, "desktopComponents");
        
        return xstream;
    }
}