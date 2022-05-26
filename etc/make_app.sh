# Script for building Simbrain.app
#
# Assumes "ant build" has been called and that the proper certificate is in the keychain on the computer from which this is run
#
# To code sign, unpackage the dmg after it's made, go to the folder it is in (dist) and run
# `codesign -fs "University of California, Merced" Simbrain.app`
# Then repackage it into a dmg by placing it in a folder and using Disk Utility, File > New Image
# Title is Simbrain-3.05.dmg

# Long jvm arg, mostly to deal with xstream issues
# See https://github.com/x-stream/xstream/issues/262
JVM_ARGS="-Duser.dir=\$APPDIR "
JVM_ARGS+="--add-opens=java.base/java.util=ALL-UNNAMED "
JVM_ARGS+="--add-opens=java.desktop/java.awt=ALL-UNNAMED "
JVM_ARGS+="--add-opens=java.base/java.util.concurrent=ALL-UNNAMED "

jpackage --input ../build/main/	 --main-jar Simbrain.jar --dest ../dist --name "Simbrain" \
	 --app-version 3.05 --icon simbrain.icns \
	 --java-options "-Duser.dir=\$APPDIR --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.desktop/java.awt=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED"
