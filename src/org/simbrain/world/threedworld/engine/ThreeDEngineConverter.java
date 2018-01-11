package org.simbrain.world.threedworld.engine;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * XStream XML Converter for the ThreeDEngine class. Used only to manage
 * references without preserving any engine state during serialization.
 */
public class ThreeDEngineConverter implements Converter {
    private ThreeDEngine engine;

    /**
     * Construct a new converter.
     */
    public ThreeDEngineConverter() { }

    /**
     * Construct a new converter which will use an existing engine to deserialize
     * references, rather than constructing an engine.
     * @param engine The engine to assign to serialized references.
     */
    public ThreeDEngineConverter(ThreeDEngine engine) {
        this.engine = engine;
    }

    @SuppressWarnings("rawtypes") @Override
    public boolean canConvert(Class type) {
        return type.equals(ThreeDEngine.class);
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) { }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        if (engine == null) {
            engine = new ThreeDEngine();
            engine.queueState(ThreeDEngine.State.SystemPause, true);
        }
        return engine;
    }
}
