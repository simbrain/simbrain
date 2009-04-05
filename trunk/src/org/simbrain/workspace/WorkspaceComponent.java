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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.simbrain.workspace.updator.ComponentUpdatePart;

/**
 * Represents a component in a Simbrain {@link org.simbrain.workspace.Workspace}.
 * Extend this class to create your own component type.  Gui representations of
 * a workspace component should extend {@link org.simbrain.workspace.gui.GuiComponent}.
 * 
 * @param <E> The type of the workspace listener associated with this
 * component.
 */
public abstract class WorkspaceComponent<E extends WorkspaceComponentListener> {

    /** The workspace that 'owns' this component. */
    private Workspace workspace;
    
    /** The static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(WorkspaceComponent.class);
    
    /** Log4j logger. */
    private Logger logger = Logger.getLogger(WorkspaceComponent.class);

    /** Whether this component has changed since last save. */
    private boolean changedSinceLastSave = true;

    /** The name of this component.  Used in the title, in saving, etc. */
    private String name  = "";
    
    /** How to order a list of attributes. */
    public enum AttributeListingStyle { TOTAL, DEFAULT_EACH };
    
    /** Current attribute listing style. */
    private AttributeListingStyle attributeListingStyle = AttributeListingStyle.DEFAULT_EACH;

    /** Default current directory if it is not set elsewhere. */
    private static final String DEFAULT_CURRENT_DIRECTORY = "."
            + System.getProperty("file.separator");

    /**
     * Current directory. So when re-opening this type of component the app remembers
     * where to look.
     * <p>Subclasses can provide a default value using User Preferences.
     */
    private String currentDirectory = DEFAULT_CURRENT_DIRECTORY;

    /**
     * Current file.  Used when "saving" a component.
     * Subclasses can provide a default value using User Preferences.
     */
    private File currentFile;
    
    /** WorkspaceComponentListeners mapped to their proxies. */
    private final Map<WorkspaceComponentListener, E> componentListeners
        = new HashMap<WorkspaceComponentListener, E>();
    
    
    /** The set of all listeners on this component. */
    private final Collection<E> listeners = new HashSet<E>();
    
    /**
     * Construct a workspace component.
     * 
     * @param name The name of the component.
     */
    public WorkspaceComponent(final String name) {
        this.name = name;
        logger.trace(this.getClass().getCanonicalName() + ": " + name + " created");
    }

    /**
     * Used when saving a workspace.  All changed workspace components are saved using
     * this method.
     *
     * @param output the stream of data to write the data to.
     * @param format a key used to define the requested format.
     */
    public abstract void save(OutputStream output, String format);
    
    /**
     * Returns a list of the formats that this component supports.
     * 
     * <p>The default behavior is to return an empty list.  This means
     * that there is one format.
     * 
     * @return a list of the formats that this component supports.
     */
    public List<? extends String> getFormats() {
        return Collections.singletonList(getDefaultFormat());
    }

    /**
     * Closes the WorkspaceComponent.
     */
    public final void close() {
        closing();
        workspace.removeWorkspaceComponent(this);
    }
    
    /**
     * Perform cleanup after closing.
    */
    protected abstract void closing();

    /**
     * Called by Workspace to update the state of the component.
     */
    public void update() {
        /* no default implementation */
    }
    
    /**
     * Returns the collection of update parts for this component.
     * 
     * @return The collection of update parts for this component.
     */
    public Collection<ComponentUpdatePart> getUpdateParts() {
        Runnable callable = new Runnable() {
            public void run() {
                update();
            }
        };
        
        return Collections.singleton(new ComponentUpdatePart(this, callable, toString(), this));
    }
    
    /**
     * Returns the locks for the update parts.  There should be one lock
     * per part.  These locks need to be the same ones used to lock the
     * update of each part.
     * 
     * @return The locks for the update parts.
     */
    public Collection<? extends Object> getLocks() {
        return Collections.singleton(this);
    }
    
    /**
     * Called by Workspace to notify that updates have stopped.
     */
    protected void stopped() {
        /* no default implementation */
    }
    
    /**
     * Notify all listeners of a componentUpdated event.
     */
    public final void fireUpdateEvent() {
        for (WorkspaceComponentListener listener : listeners) {
            listener.componentUpdated();
        }
    }
    
    /**
     * Called after a global update ends.
     */
    final void doStopped() {
        stopped();
    }
    
    /**
     * Returns the listeners on this component.
     * 
     * @return The listeners on this component.
     */
    protected Collection<? extends E> getListeners() {
        return Collections.unmodifiableCollection(listeners);
    }
    
    /**
     * Adds a listener to this component.
     * 
     * @param listener the Listener to add.
     */
    public void addComponentListener(final WorkspaceComponentListener listener) {
        E proxy = getProxy(listener);
        if (proxy == null) throw new IllegalArgumentException(
            "this workspace component class: " + getClass().getName()
            + " is not designed to allow standard WorkspaceComponentListeners. "
            + "Please review the developer documentation on this subject or seek assistance.");
            
        listeners.add(proxy);
    }

    /**
     * Returns a proxy for a plain WorkspaceComponentListener so that subclasses
     * may treat it as a specialized listener.
     * 
     * I (Yoshimi) was initially confused by this; in case it helps, here is a
     * (slightly edited) Q/A session about the issue.
     * 
     * QUESTION: I'm not clear why we needed the proxy stuff. Wouldn't just
     * adding attributeRemoved to WorkspaceComponentListener have been
     * sufficient?
     * 
     * ANSWER: Probably the easiest way to understand this is to comment out the
     * addComponentListener method and try to resolve the resulting compilation
     * errors on CouplingManager without uncommenting it.
     * 
     * This is one of those traps of generics that I've been talking about.The
     * issue came up when I tried to make CoupingManager be a
     * WorkspaceComponentListener. The problem is that WorkspaceComponent has a
     * generic parameter E that defines the type of the listener. This allows
     * sub-types of WorkspaceComponent to customize their listeners without a
     * lot of redundant code. This is really useful and seems like a great use a
     * of generics, and it is until you want to do something like write a
     * handler that's general for all WorkspaceComponenetListeners. You can't
     * add it to the WorkspaceComponent instance. The reason is that in
     * CouplingManager we refer to the WorkspaceComponentListener as
     * WorkspaceComponentListener<?> because we don't know (or care) what the
     * specific type of the workspace component is. When you try to add any old
     * WorkspaceComponentListener to the WorkspaceComponent<?>, it comes back
     * with an error that basically says: "hey buddy, you see that '?' You
     * haven't told me what kind of WorkspaceComponentListener this
     * WorkspaceComponent takes and I can't be sure what you are providing is
     * the right one."
     * 
     * If you look at the RootNetwork class, it becomes clear why that won't
     * work. RootNetwork calls all these specialized events on the listener
     * instances. If those methods weren't there, the code would fail. Really,
     * it would fail earlier because the Java ensures that you can't assign
     * WorkspaceComponentListener reference to a NetworkListener variable.
     * 
     * So what I did is use a fairly esoteric feature of Java that allows you to
     * implement an unknown interface at runtime dynamically. And basically all
     * I do is if the method is implemented by the actual
     * WorkspaceComponentListener instance, it forwards the event to the
     * instance, otherwise nothing with the event. That lets observers add
     * listeners for just the basic WorkspaceComponentListener events without
     * having every WorkspaceComponent implement special handling for both
     * standard listeners and the special listeners which would defeat the
     * purpose.
     * 
     * 
     * @param listener
     *            the listener to wrap
     * @return a proxy for the listener implementing E
     */
    @SuppressWarnings("unchecked")
    private E getProxy(final WorkspaceComponentListener listener) {
        Type superclass = getClass().getGenericSuperclass();
        
        Type[] typeArguments = ((ParameterizedType) superclass).getActualTypeArguments();
        
        if (typeArguments == null || typeArguments.length <= 0) return null;
            
        Type type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
        
        if (!(type instanceof Class<?>)) return null;
        
        Class<?>[] clazz = {(Class<?>) type};
        
        if (!WorkspaceComponentListener.class.isAssignableFrom(clazz[0])) return null;
        
        return (E) Proxy.newProxyInstance(getClass().getClassLoader(),
            clazz, new ProxyInvocationHandler(listener));
    }
    
    /**
     * A invocation handler for listener proxies.
     * 
     * @author Matt Watson
     */
    private class ProxyInvocationHandler implements InvocationHandler {
        /** The underlying listener. */
        final WorkspaceComponentListener listener;
        
        /**
         * Creates a new instance for the provided listener.
         * 
         * @param listener the listener to wrap.
         */
        ProxyInvocationHandler(final WorkspaceComponentListener listener) {
            this.listener = listener;
        }
        
        /**
         * If the method called is a WorkspaceComponentListener method, invokes
         * that method, otherwise, does nothing.
         * 
         * @param proxy the proxy object
         * @param method the method being called
         * @param args the arguments to that method
         * @return the result of the method call or null
         * @throws InvocationTargetException if something goes wrong
         * @throws IllegalAccessException if the security maanger prevents the call
         */
        public Object invoke(final Object proxy, final Method method, final Object[] args)
                throws InvocationTargetException, IllegalAccessException {
            try {
                Class<?> listenerClass = listener.getClass();
                Method implementation = listenerClass.getMethod(method.getName(),
                    method.getParameterTypes());
                
                return implementation.invoke(listener, args);
            } catch (NoSuchMethodException e) {
                return null;
            }
        }
    }
    
    /**
     * Adds a listener to this component.
     * 
     * @param listener the Listener to add.
     */
    public void addListener(final E listener) {
        listeners.add(listener);
    }
    
    /**
     * Adds a listener to this component.
     * 
     * @param listener the Listener to add.
     */
    public void removeComponentListener(final WorkspaceComponentListener listener) {
        listeners.remove(componentListeners.get(listener));
        componentListeners.remove(listener);
    }
    
    /**
     * Removes a listener to this component.
     * 
     * @param listener the Listener to add.
     */
    public void removeListener(final E listener) {
        listeners.add(listener);
    }

    /**
     * Returns the name of this component.
     * 
     * @return The name of this component.
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return name;
//        return this.getClass().getSimpleName() + ": " + name;
    }

    /**
     * @param name the name to set
     */
    public void setName(final String name) {
        this.name = name;
        for (WorkspaceComponentListener listener : this.getListeners()) {
            listener.setTitle(name);
        }
    }

    /**
     * Retrieves a simple version of a component name from its class,
     * e.g. "Network" from "org.simbrain.network.NetworkComponent"/
     *
     * @return the simple name.
     */
    public String getSimpleName() {
        String simpleName = getClass().getSimpleName();
        if (simpleName.endsWith("Component")) {
            simpleName = simpleName.replaceFirst("Component", "");
        }
        return simpleName;
    }

    /**
     * Returns the consumers associated with this component.
     * 
     * @return The consumers associated with this component.
     */
    public Collection<? extends Consumer> getConsumers() {
        return Collections.emptySet();
    }

    /**
     * Return producing attributes in a particular order, for use in creating couplings.
     * 
     * @return custom list of producing attributes.
     */
    public ArrayList<ProducingAttribute<?>> getProducingAttributes() {
        
        ArrayList<ProducingAttribute<?>> list = new ArrayList<ProducingAttribute<?>>();
    
        if (attributeListingStyle.equals(AttributeListingStyle.DEFAULT_EACH)) {
            for (Producer producer : this.getProducers()) {
                list.add(producer.getDefaultProducingAttribute());
            }
        } else if (attributeListingStyle.equals(AttributeListingStyle.TOTAL)) {
            for (Producer producer : this.getProducers()) {
                for (ProducingAttribute<?> attribute : producer.getProducingAttributes()) {
                    list.add(attribute);
                }
            }
        }
        return list;
    }

    /**
     * Return consuming attributes in a specified order.
     * 
     * @return custom list of producing attributes.
     */
    public ArrayList<ConsumingAttribute<?>> getConsumingAttributes() {
        
        ArrayList<ConsumingAttribute<?>> list = new ArrayList<ConsumingAttribute<?>>();
        if (attributeListingStyle.equals(AttributeListingStyle.DEFAULT_EACH)) {
            for (Consumer consumer : this.getConsumers()) {
                list.add(consumer.getDefaultConsumingAttribute());
            }
        } else if (attributeListingStyle.equals(AttributeListingStyle.TOTAL)) {
            for (Consumer consumer : this.getConsumers()) {
                for (ConsumingAttribute<?> attribute : consumer.getConsumingAttributes()) {
                    list.add(attribute);
                }

            }
        }
        return list;
    }
        
    /**
     * Open a workspace component from a file.  Currently assumes xml.
     *
     * @param openFile file representing saved component.
     */
    public final void open(final File openFile) {
        setCurrentFile(openFile);
        FileReader reader;
        try {
            reader = new FileReader(openFile);
            deserializeFromReader(reader);
            setChangedSinceLastSave(false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Override for use with open service.
     *
     * @return xml string representing stored file.
     */
    public String getXML() {
        return null;
    }

    /**
     * Override for use with save service.
     * 
     * @param reader the reader to deserialize from.
     */
    public final void deserializeFromReader(final FileReader reader) {
        // no implementation
    }

    
    /**
     * Returns the producers associated with this component.
     * 
     * @return The producers associated with this component.
     */
    public Collection<? extends Producer> getProducers() {
        return Collections.emptySet();
    }
    
    /**
     * Get a SingleConsumingAttribute by name.
     *
     * @param consumerId id of single consuming attribute
     * @return the attribute
     */
    public ConsumingAttribute getSingleConsumingAttribute(final String consumerId) {
        for (Consumer consumer : getConsumers()) {
            if (consumer instanceof SingleAttributeConsumer) {
                if (consumer.getDescription().equalsIgnoreCase(consumerId)) {
                    return consumer.getDefaultConsumingAttribute();
                }
            }
        }
        return null;
    }


    /**
     * Get a SingleProducingAttribute by id.
     *
     * @param producerId id of single producing attribute
     * @return the attribute
     */
    public ProducingAttribute getSingleProducingAttribute(final String producerId) {
        for (Producer producer : getProducers()) {
            if (producer instanceof SingleAttributeProducer) {
                if (producer.getDescription().equalsIgnoreCase(producerId)) {
                    return producer.getDefaultProducingAttribute();
                }
            }
        }
        return null;
    }

    /**
     * Get a consuming attribute, using the id of the attribute holder and the attribute itself.
     *
     * @param consumerId the name of the consumer (attribute holder)
     * @param attributeId the name of the attribute
     * @return the consuming attribute
     */
    public ConsumingAttribute getConsumingAttribute(String consumerId, String attributeId) {
        for (Consumer consumer : getConsumers()) {
            if (consumer.getDescription().equalsIgnoreCase(consumerId)) {
                   for(ConsumingAttribute attribute : consumer.getConsumingAttributes()) {
                       //System.out.println(attribute.getAttributeDescription());
                          if (attribute.getKey().equals(attributeId)) {
                              return attribute;
                          }
                   }
            }
        }
        return null;
    }

    /**
     * Get a producing attribute, using the id of the attribute holder and the attribute itself.
     *
     * @param producerId the name of the producing (attribute holder)
     * @param attributeId the name of the attribute
     * @return the producing attribute
     */
    public ProducingAttribute getProducingAttribute(String producerId, String attributeId) {
        for (Producer producer : getProducers()) {
            //System.out.println(producer.getDescription());            
            if (producer.getDescription().equalsIgnoreCase(producerId)) {
                   for(ProducingAttribute attribute : producer.getProducingAttributes()) {
                       //System.out.println(attribute.getAttributeDescription());
                          if (attribute.getKey().equals(attributeId)) {
                              return attribute;
                          }
                   }
            }
        }
        return null;
    }

    
    /**
     * Sets the workspace for this component.  Called by the
     * workspace right after this component is created.
     * 
     * @param workspace The workspace for this component.
     */
    public void setWorkspace(final Workspace workspace) {
        this.workspace = workspace;
    }

    /**
     * Returns the workspace associated with this component.
     * 
     * @return The workspace associated with this component.
     */
    public Workspace getWorkspace() {
        return workspace;
    }
    
    /**
     * Called when a coupling that this workspace owns the target
     * or source of is removed from the workspace.  This method
     * will only be called once if the workspace owns both the
     * source and the target.
     * 
     * @param coupling The coupling that has been removed.
     */
    protected void couplingRemoved(final Coupling<?> coupling) {
        /* no default implementation */
    }

    /**
     * Returns the attribute listing style.
     * 
     * @return The attribute listing style.
     */
    public AttributeListingStyle getAttributeListingStyle() {
        return attributeListingStyle;
    }

    /**
     * Sets the attribute listing style.
     * 
     * @param style the listing style.
     */
    public void setAttributeListingStyle(final AttributeListingStyle style) {
        this.attributeListingStyle = style;
    }
    
    /**
     * The overall name for the set of supported formats.
     *
     * @return the description
     */
    public String getDescription() {
        return null;
    }
    
    /**
     * The file extension for a component type, e.g. By default, "xml".
     *
     * @return the file extension
     */
    public String getDefaultFormat() {
        return "xml";
    }
    
    /**
     * Set to true when a component changes, set to false after a component is saved.
     *
     * @param changedSinceLastSave whether this component has changed since the last save.
     */
    public void setChangedSinceLastSave(final boolean changedSinceLastSave) {
        LOGGER.debug("component changed");
        this.changedSinceLastSave = changedSinceLastSave;
    }

    /**
     * Returns true if it's changed since the last save.
     *
     * @return the changedSinceLastSave
     */
    public boolean hasChangedSinceLastSave() {
        return changedSinceLastSave;
    }

    /**
     * This should be overridden if there are user preferences to get.
     *
     * @return the currentDirectory
     */
    public String getCurrentDirectory() {
        return currentDirectory;
    }

    /**
     *
     * This should be overridden if there are user preferences to set.
     *
     * @param currentDirectory the currentDirectory to set
     */
    public void setCurrentDirectory(final String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    /**
     * @return the currentFile
     */
    public File getCurrentFile() {
        return currentFile;
    }

    /**
     * @param currentFile the currentFile to set
     */
    public void setCurrentFile(final File currentFile) {
        this.currentFile = currentFile;
    }
}
