package org.simbrain.util;

/**
 * Wraps a getter and setter object on an object, of a specified type.  A string
 * description is provided to serve as a key when editors are stored in maps.
 *
 * @param <O> the type of the object whose property is being edited, e.g. NeuronUpdateRule.
 * @param <V> the value that is being edited, e.g. Double.
 */
public class ParameterEditor<O,V> {
    
    /** The type being edited. */
    private final Class<V> type;
    
    /** A string key for this editor. */
    private final String key;
    
    /** The getter. */
    private final ParameterGetter<O,V> getter;
    
    /** The setter. */
    private final ParameterSetter<O,V> setter;
    
    /**
     * Construct the editor.
     */
    public ParameterEditor(Class<V> type, String description, ParameterGetter<O,V> parameterGetter,
            ParameterSetter<O,V> parameterSetter) {
        super();
        this.type = type;
        this.key = description;
        this.getter = parameterGetter;
        this.setter = parameterSetter;
    }

    /**
     * @return the type
     */
    public Class<V> getType() {
        return type;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the getter
     */
    public ParameterGetter<O, V> getGetter() {
        return getter;
    }

    /**
     * @return the setter
     */
    public ParameterSetter<O, V> getSetter() {
        return setter;
    }

}
