SET CLASSPATH=src;lib\utils.jar;lib\piccolo.jar;lib\piccolox.jar;lib\hisee.jar;lib\simnet.jar;lib\Jama-1.0.1.jar;lib\calpahtml.jar;lib\castor.jar;lib\xerxes.jar;lib\jlinalg.jar;lib\snarli.jar
cd ..
javac -d bin -classpath %CLASSPATH% src\org\simbrain\network\*.java 
javac -d bin -classpath %CLASSPATH% src\org\simbrain\network\old\*.java 
javac -d bin -classpath %CLASSPATH% src\org\simbrain\network\dialog\*.java 
javac -d bin -classpath %CLASSPATH% src\org\simbrain\network\pnodes\*.java 
javac -d bin -classpath %CLASSPATH% src\org\simbrain\util\*.java 
javac -d bin -classpath %CLASSPATH% src\org\simbrain\simulation\*.java 
javac -d bin -classpath %CLASSPATH% src\org\simbrain\world\*.java 
javac -d bin -classpath %CLASSPATH% src\org\simbrain\resource\*.java 
javac -d bin -classpath %CLASSPATH% src\org\simnet\*.java 
javac -d bin -classpath %CLASSPATH% src\org\simnet\interfaces\*.java 
javac -d bin -classpath %CLASSPATH% src\org\simnet\networks\*.java 
javac -d bin -classpath %CLASSPATH% src\org\simnet\util\*.java 
javac -d bin -classpath %CLASSPATH% src\org\simnet\synapses\*.java 
javac -d bin -classpath %CLASSPATH% src\org\simnet\synapses\rules\*.java 
javac -d bin -classpath %CLASSPATH% src\org\simnet\neurons\*.java 
javac -d bin -classpath %CLASSPATH% src\org\simnet\neurons\rules\*.java 
cd scripts