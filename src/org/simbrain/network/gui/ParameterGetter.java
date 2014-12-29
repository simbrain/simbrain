package org.simbrain.network.gui;

public interface ParameterGetter <T, V> {
	
	V getParameter(T source);

}
