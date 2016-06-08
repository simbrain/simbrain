#!/bin/bash
cd ../../../

if [[ ! -f "Simbrain.jar" ]];
then
	if [[ -d "./build/main/" ]];
	then
		cd ./build/main/
		if [[ ! -f "Simbrain.jar" ]];
		then
			echo "Creating simbrain jar..."
			cd ../../
			ant jar
			cd ./scripts/scriptmenu/SORN/
			javac -classpath "../../../build/main/Simbrain.jar" *.java
		fi
	else
		echo "Cannot find Simbrain.jar to perform custom compilation."
		exit 1
	fi
else
	cd ./scripts/scriptmenu/SORN/
	javac -classpath "../../../Simbrain.jar" *.java
fi




