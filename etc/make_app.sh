# Assumes "ant build" has been called
jpackage --input ../build/main/	 --main-jar Simbrain.jar --dest ../dist --name "Simbrain" \
	 --app-version 3.05 --icon simbrain.icns --java-options "-Duser.dir=\$APPDIR --illegal-access=permit"