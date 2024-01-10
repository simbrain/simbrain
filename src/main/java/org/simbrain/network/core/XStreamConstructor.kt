package org.simbrain.network.core;

import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.mapper.Mapper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Use this annotation to mark the constructor that should be used by {@link org.simbrain.util.XStreamUtils#createConstructorCallingConverter(Class, Mapper, ReflectionProvider, List)}}
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface XStreamConstructor {}
