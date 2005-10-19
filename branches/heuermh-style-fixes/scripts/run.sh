OS=`uname -s`
if [ $OS -eq 'Windows_NT' ] ; then
	SEP=";"
else 
	SEP=":"
fi
cd ..
java -classpath .$SEP./bin$SEP./lib/utils.jar$SEP./lib/Jama-1.0.1.jar$SEP./lib/simnet.jar$SEP./lib/piccolo.jar$SEP./lib/piccolox.jar$SEP./lib.$SEP./lib/castor.jar$SEP./lib/snarli.jar$SEP./lib/jlinalg.jar$SEP./lib/xerxes.jar org.simbrain.workspace.Splasher
cd scripts
