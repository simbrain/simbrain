SET CLASSPATH=bin;lib\utils.jar;lib\piccolo.jar;lib\piccolox.jar;lib\hisee.jar;lib\simnet.jar;lib\Jama-1.0.1.jar;lib\calpahtml.jar;lib\castor.jar;lib\xerxes.jar;lib\jlinalg.jar
cd ..
javac -classpath %CLASSPATH% src\org\simbrain\network\*.java 
javac -classpath %CLASSPATH% src\org\simbrain\network\old\*.java 
javac -classpath %CLASSPATH% src\org\simbrain\network\dialogs\*.java 
javac -classpath %CLASSPATH% src\org\simbrain\network\pnodes\*.java 
javac -classpath %CLASSPATH% src\org\simbrain\util\*.java 
javac -classpath %CLASSPATH% src\org\simbrain\simulation\*.java 
javac -classpath %CLASSPATH% src\org\simbrain\world\*.java 
javac -classpath %CLASSPATH% src\org\simbrain\resource\*.java 
javac -classpath %CLASSPATH% src\org\simnet\*.java 
javac -classpath %CLASSPATH% src\org\simnet\interfaces\*.java 
javac -classpath %CLASSPATH% src\org\simnet\networks\*.java 
javac -classpath %CLASSPATH% src\org\simnet\util\*.java 
javac -classpath %CLASSPATH% src\org\simnet\synapses\*.java 
javac -classpath %CLASSPATH% src\org\simnet\synapses\rules\*.java 
javac -classpath %CLASSPATH% src\org\simnet\neurons\*.java 
javac -classpath %CLASSPATH% src\org\simnet\neurons\rules\*.java 
cd scripts

