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
		else
			cd ../../scripts/scriptmenu/SORN/
		fi
		echo "Compiling SORN and AddSTDP."
		javac -classpath "../../../build/main/Simbrain.jar" *.java
	else
		echo "Cannot find Simbrain.jar to perform custom compilation."
		exit 1
	fi
else
	echo "Compiling SORN and AddSTDP."
	cd ./scripts/scriptmenu/SORN/
	javac -classpath "../../../Simbrain.jar" *.java
fi




