#!/usr/bin/env zsh

#
# Script for building Simbrain.app
#
# Assumes "ant build" has been called and that the proper certificate is in the keychain on the computer from which this is run

# See https://github.com/x-stream/xstream/issues/262
JVM_ARGS="-Duser.dir=\$APPDIR "
JVM_ARGS+="--add-opens=java.base/java.util=ALL-UNNAMED "
JVM_ARGS+="--add-opens=java.desktop/java.awt=ALL-UNNAMED "
JVM_ARGS+="--add-opens=java.base/java.util.concurrent=ALL-UNNAMED "

jpackage --input build/main/ --main-jar Simbrain.jar --dest dist --name "Simbrain" \
	 --app-version 3.07 --icon etc/simbrain.icns \
  	 --mac-sign --mac-signing-key-user-name "Regents of the University of CA, Merced (W8BB6W47ZR)" \
	 --java-options $JVM_ARGS --type app-image \
	 --verbose
