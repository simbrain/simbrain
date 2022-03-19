// /*
//  * Part of Simbrain--a java-based neural network kit
//  * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
//  *
//  * This program is free software; you can redistribute it and/or modify
//  * it under the terms of the GNU General Public License as published by
//  * the Free Software Foundation; either version 2 of the License, or
//  * (at your option) any later version.
//  *
//  * This program is distributed in the hope that it will be useful,
//  * but WITHOUT ANY WARRANTY; without even the implied warranty of
//  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  * GNU General Public License for more details.
//  *
//  * You should have received a copy of the GNU General Public License
//  * along with this program; if not, write to the Free Software
//  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//  */
package org.simbrain.workspace.updater
//
// import org.simbrain.workspace.updater.WorkspaceUpdater.workspace
// import org.simbrain.workspace.updater.UpdateAction.invoke
// import org.simbrain.workspace.updater.UpdateAction.description
// import org.simbrain.workspace.updater.UpdateAction.longDescription
// import org.simbrain.workspace.updater.WorkspaceUpdater
// import org.simbrain.workspace.Workspace
// import java.lang.StringBuilder
// import java.io.FileInputStream
// import java.io.FileNotFoundException
// import bsh.EvalError
// import bsh.Interpreter
// import java.io.File
// import java.util.*
//
// /**
//  * Update using a custom action saved as a beanshell script.
//  *
//  * @author jyoshimi
//  */
// class UpdateActionCustom(description: String, longDescription: String, updater: WorkspaceUpdater, action: suspend () -> Unit) : UpdateAction(description, longDescription) {
//
//     /**
//      * @return the scriptString
//      */
//     /**
//      * @param scriptString the scriptString to set
//      */
//     /**
//      * The custom update script in persistable string form.
//      */
//     var scriptString: String
//
//     /**
//      * The interpreter for converting the the script into an executable update
//      * action.
//      */
//     @Transient
//     private val interpreter: Interpreter = Interpreter()
//
//     /**
//      * Custom update action.
//      */
//     @Transient
//     private lateinit var theAction: UpdateAction
//
//     /**
//      * Create a new custom update action from a file containing the custom
//      * script.
//      *
//      * @param updater reference to workspace updater
//      * @param script  the custom script as a string
//      */
//     constructor(updater: WorkspaceUpdater, script: String) : super() {
//         this.updater = updater
//         scriptString = script
//         init()
//     }
//
//     /**
//      * Create a new custom update action from a file containing the custom
//      * script.
//      *
//      * @param workspace reference to parent workspace
//      * @param file      file containing custom code
//      */
//     constructor(workspace: Workspace, file: File?) : super() {
//         updater = workspace.updater
//         val scriptText = StringBuilder()
//         val newLine = System.getProperty("line.separator")
//         var scanner: Scanner? = null
//         try {
//             scanner = Scanner(FileInputStream(file))
//             while (scanner.hasNextLine()) {
//                 scriptText.append(scanner.nextLine() + newLine)
//             }
//         } catch (e: FileNotFoundException) {
//             e.printStackTrace()
//         } finally {
//             scanner!!.close()
//         }
//         scriptString = scriptText.toString()
//         init()
//     }
//
//     /**
//      * Initialize the interpreter.
//      */
//     fun init() {
//         try {
//             interpreter["updater"] = updater
//             interpreter["workspace"] = updater.workspace
//             interpreter.eval(scriptString)
//             theAction = interpreter["action"] as UpdateAction
//         } catch (e: EvalError) {
//             e.printStackTrace()
//         }
//     }
//
//     /**
//      * {@inheritDoc}
//      */
//     override suspend operator fun invoke() {
//         theAction.invoke()
//     }
//
//     override val description: String?
//         get() = theAction.description
//
//     override val longDescription: String?
//         get() = theAction.longDescription
// }
//
// fun createUpdateActionCustom(updater: WorkspaceUpdater, description: String, longDescription: String, action: suspend () -> Unit) {
//     return UpdateActionCustom()
// }