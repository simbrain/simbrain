OS=`uname -s`
if [ $OS -eq 'Windows_NT' ] ; then
	SEP=";"
else 
	SEP=":"
fi
cd ..
CLASSPATH=".$SEP./src$SEP./lib/utils.jar$SEP./lib/hisee.jar$SEP./lib/Jama-1.0.1.jar$SEP./lib/simnet.jar$SEP./lib/piccolo.jar$SEP./lib/piccolox.jar$SEP./lib.$SEP./lib/calpahtml.jar$SEP./lib/castor.jar$SEP./lib/snarli.jar$SEP./lib/jlinalg.jar$SEP./lib/xerxes.jar"
javac -d ./bin -classpath $CLASSPATH ./src/org/simnet/*.java
javac -d ./bin -classpath $CLASSPATH ./src/org/simnet/*/*.java
javac -d ./bin -classpath $CLASSPATH ./src/org/simnet/*/*/*.java
javac -d ./bin -classpath $CLASSPATH ./src/org/simbrain/*/*.java 
javac -d ./bin -classpath $CLASSPATH ./src/org/simbrain/*/*/*.java 
cp ./src/org/simbrain/resource/*.gif  ./bin/org/simbrain/resource
cd scripts
