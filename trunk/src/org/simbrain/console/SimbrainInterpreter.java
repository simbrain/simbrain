package org.simbrain.console;

import java.io.*;
import java.lang.reflect.InvocationTargetException;

import org.simbrain.workspace.Workspace;

import bsh.commands.*;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.TargetError;

public class SimbrainInterpreter extends bsh.Interpreter {

    public static void main(final String[] args) {
        CommandLineReader in = new CommandLineReader(new InputStreamReader(System.in));
        Interpreter interpreter = new Interpreter(in, System.out, System.err, true);
        interpreter.getNameSpace().importPackage("org.simbrain.network.neurons");
        interpreter.getNameSpace().importPackage("org.simbrain.network.connections");
        interpreter.getNameSpace().importPackage("org.simbrain.network.layouts");
        interpreter.getNameSpace().importPackage("org.simbrain.network.networks");
        interpreter.getNameSpace().importPackage("org.simbrain.network.interfaces");
        interpreter.getNameSpace().importPackage("org.simbrain.network.groups");
        interpreter.getNameSpace().importPackage("org.simbrain.network.synapses");
        interpreter.getNameSpace().importPackage("org.simbrain.workspace");
        interpreter.getNameSpace().importCommands(".");
        interpreter.getNameSpace().importCommands("org.simbrain.console.commands");
        try {
            interpreter.set("workspace", new Workspace());
            interpreter.set("bsh.prompt", ">");
        } catch (EvalError e) {
            e.printStackTrace();
        }
        interpreter.run();
    }

}
