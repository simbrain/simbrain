package org.simbrain.network.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import org.simbrain.network.connections.ConnectionUtilities;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.randomizer.PolarizedRandomizer;

public class PerformanceTests {

    public static final Network network = new Network();

    public static final int cycles = 1;

    public static final int numStrings = 1000000;

    public static final Synapse[] strings = new Synapse[numStrings];

    public static void init() {
        for (int i = 0; i < numStrings; i++) {
            strings[i] = randomString();
        }
    }

    public static Synapse randomString() {
        // StringBuilder sb = new StringBuilder();
        // Random randChar = new Random();
        // for (int i = 0; i < stringSize; i++) {
        // sb.append((char) randChar.nextInt(Short.MAX_VALUE));
        // }
        Neuron source = new Neuron(network);
        Neuron target = new Neuron(network);
        return new Synapse(source, target);
    }

    public static void HashSetAdd(HashSet<Synapse> hs) {
        Timer t = new Timer();
        t.start();
        for (int i = 0; i < numStrings; i++) {
            hs.add(strings[i]);
        }
        t.end();
        System.out.println("HashSet Add: \t\t" + t.getTime());
    }

    public static void ArrayListAdd(ArrayList<Synapse> al) {
        Timer t = new Timer();
        t.start();

        for (int i = 0; i < numStrings; i++) {
            al.add(strings[i]);
        }
        t.end();
        System.out.println("ArrayList Add: \t\t" + t.getTime());
    }

    public static void HashSetRemove(HashSet<Synapse> hs, int[] removeOrder) {
        Timer t = new Timer();
        t.start();
        for (int i = 0; i < numStrings; i++) {
            hs.remove(strings[removeOrder[i]]);
        }
        t.end();
        System.out.println("HashSet Remove: \t" + t.getTime());
    }

    public static void
        ArrayListRemove(ArrayList<Synapse> al, int[] removeOrder) {
        Timer t = new Timer();
        t.start();
        for (int i = 0; i < numStrings; i++) {
            al.remove(strings[removeOrder[i]]);
        }
        t.end();
        System.out.println("ArrayList Remove: \t" + t.getTime());
    }

    public static void ArrayListAddAll(ArrayList<Synapse> al) {
        Timer t = new Timer();
        t.start();
        al.addAll(Arrays.asList(strings));
        t.end();
        System.out.println("ArrayList AddAll: \t" + t.getTime());
    }

    public static void HashSetAddAll(HashSet<Synapse> hs) {
        Timer t = new Timer();
        t.start();
        hs.addAll(Arrays.asList(strings));
        t.end();
        System.out.println("HashSet AddAll: \t" + t.getTime());
    }

    public static void HashSetContains(HashSet<Synapse> hs, int[] removeOrder) {
        Timer t = new Timer();
        t.start();
        Random rand = new Random();
        for (int j = 0; j < cycles; j++) {
            for (int i = 0; i < numStrings; i++) {
                if (rand.nextBoolean()) {
                    hs.contains(strings[removeOrder[i]]);
                } else {
                    hs.contains(randomString());
                }
            }
        }
        t.end();
        System.out.println("HashSet Contains: \t" + t.getTime());
    }

    public static void ArrayListContains(ArrayList<Synapse> al,
        int[] removeOrder) {
        Timer t = new Timer();
        t.start();
        Random rand = new Random();
        for (int j = 0; j < cycles; j++) {
            for (int i = 0; i < numStrings; i++) {
                if (rand.nextBoolean()) {
                    al.contains(strings[removeOrder[i]]);
                } else {
                    al.contains(randomString());
                }
            }
        }
        t.end();
        System.out.println("ArrayList Contains: \t" + t.getTime());
    }

    public static void ArrayListRemoveSeq(ArrayList<Synapse> al) {
        Timer t = new Timer();
        t.start();
        al.clear();
        t.end();
        System.out.println("ArrayList Remove Seq.: \t" + t.getTime());
    }

    public static void HashSetRemoveSeq(HashSet<Synapse> hs) {
        Timer t = new Timer();
        t.start();
        hs.clear();
        t.end();
        System.out.println("HashSet Remove Seq.: \t" + t.getTime());
    }

    public static void main(String[] args) {

        init();

        HashSet<Synapse> hs = new HashSet<Synapse>((int) (numStrings / 0.75));
        ArrayList<Synapse> al = new ArrayList<Synapse>(
            (int) (numStrings / 0.75));
        int[] permute = SimbrainMath.randPermute(0, numStrings);

        ConnectionUtilities.randomizeAndPolarizeSynapses(
            Arrays.asList(strings), new PolarizedRandomizer(
                Polarity.EXCITATORY), new PolarizedRandomizer(
                Polarity.INHIBITORY), 0.5);

        HashSetAdd(hs);
        ArrayListAdd(al);
        HashSetContains(hs, permute);
        ArrayListContains(al, permute);
        HashSetRemove(hs, permute);
        ArrayListRemove(al, permute);
        // hs = new HashSet<String>((int) (numStrings / 0.75));
        // al = new ArrayList<String>((int) (numStrings / 0.75));
        permute = null;
        Runtime.getRuntime().gc();
        HashSetAddAll(hs);
        ArrayListAddAll(al);
        HashSetRemoveSeq(hs);
        ArrayListRemoveSeq(al);

    }

    public static class Timer {
        private long start;
        private long end;

        public Timer() {
        }

        public void start() {
            start = System.nanoTime();
        }

        public void end() {
            end = System.nanoTime();
        }

        public double getTime() {
            return (end - start) / Math.pow(10, 9);
        }
    }

}
