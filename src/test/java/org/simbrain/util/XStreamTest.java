package org.simbrain.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class XStreamTest {

    // More of a sandbox for now but XStreamUtils does need tests

    @Test
    public void testArraySerialization() {
        var list = new ArrayList<String>();
        list.add("A");
        list.add("B");
        list.add("C");
        var xml = XStreamUtils.getSimbrainXStream().toXML(list);
        var newList = (ArrayList<String>)XStreamUtils.getSimbrainXStream().fromXML(xml);
        System.out.println(newList);
        assertArrayEquals(list.toArray(), newList.toArray());
    }

}