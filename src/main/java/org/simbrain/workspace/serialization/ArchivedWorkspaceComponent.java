package org.simbrain.workspace.serialization;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.gui.DesktopComponent;

/**
 * Represents the data used to store components in the archive.
 *
 * @author Matt Watson
 */
@XStreamAlias("ArchivedWorkspaceComponent")
public final class ArchivedWorkspaceComponent {

    /**
     * The name of the class for the component.
     */
    private final String className;

    /**
     * The name of the Component.
     */
    private final String name;

    /**
     * The uri for the serialized component.
     */
    final String uri;

    /**
     * A unique id for the component in the archive.
     */
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
    private ArchivedWorkspaceComponent.ArchivedDesktopComponent desktopComponent;

    /**
     * Creates a new Component entry.
     *
     * @param serializer The component serializer for the archive.
     * @param component  The workspace component this entry represents.
     */
    ArchivedWorkspaceComponent(final WorkspaceComponentSerializer serializer, final WorkspaceComponent component) {
        this.className = component.getClass().getCanonicalName();
        this.id = serializer.getId(component);
        this.name = component.name;
        this.format = component.getDefaultFormat();
        this.uri = "components/" + id + '_' + name.replaceAll("\\s", "_") + '.' + format;
    }

    /**
     * Adds a desktop component to this component entry.
     *
     * @param dc The desktop component to add an entry for.
     * @return The entry for the desktop component.
     */
    ArchivedWorkspaceComponent.ArchivedDesktopComponent addDesktopComponent(final DesktopComponent<?> dc) {
        return desktopComponent = new ArchivedDesktopComponent(this, dc);
    }

    /**
     * Class used to represent a desktop component in the archive.
     *
     * @author Matt Watson
     */
    @XStreamAlias("DesktopComponent")
    static final class ArchivedDesktopComponent {

        /**
         * The class for the desktop component.
         */
        private final String className;

        /**
         * The uri for the serialized data.
         */
        private final String uri;

        /**
         * The format for the serialized data.
         */
        private final String format;

        /**
         * Creates a new instance.
         *
         * @param parent The parent component entry.
         * @param dc     The desktop component this instance represents.
         */
        private ArchivedDesktopComponent(final ArchivedWorkspaceComponent parent, final DesktopComponent<?> dc) {
            this.className = dc.getClass().getCanonicalName();
            this.format = dc.getWorkspaceComponent().getDefaultFormat();
            this.uri = "guis/" + parent.id + '_' + parent.name.replaceAll("\\s", "_") + '.' + format;
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
    public ArchivedWorkspaceComponent.ArchivedDesktopComponent getDesktopComponent() {
        return desktopComponent;
    }
}