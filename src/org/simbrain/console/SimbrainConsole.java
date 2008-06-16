package org.simbrain.console;

import java.io.*;
import java.lang.reflect.InvocationTargetException;

import org.simbrain.workspace.Workspace;

import bsh.commands.*;
import bsh.Capabilities;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.TargetError;

public class SimbrainConsole extends bsh.Console {

    public static void main( String args[] ) {

        if ( !Capabilities.classExists( "bsh.util.Util" ) )
            System.out.println("Can't find the BeanShell utilities...");

        if ( Capabilities.haveSwing() ) 
        {
            bsh.util.Util.startSplashScreen();
            try {
                Interpreter interpreter = new Interpreter();
                interpreter.getNameSpace().importCommands("org.simbrain.console.commands");
                interpreter.eval("simbrainConsoleDesktop()");
                
            } catch (EvalError e) {
                e.printStackTrace();
            }
        } else {
            System.err.println(
                "Can't find javax.swing package: "
            +" An AWT based Console is available but not built by default.");
            //AWTConsole.main( args );
        }
    }

}
