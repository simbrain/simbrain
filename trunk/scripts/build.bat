SET CLASSPATH=src;lib\utils.jar;lib\piccolo.jar;lib\piccolox.jar;lib\simnet.jar;lib\Jama-1.0.1.jar;lib\castor.jar;lib\xerxes.jar;lib\jlinalg.jar;lib\snarli.jar;lib\bsh-2.0b4.jar;build\checkstyle\checkstyle-all-4.0-beta6.jar
cd ..
javac -d bin -classpath %CLASSPATH% src\org\simbrain\gauge\*.java
javac -d bin -classpath %CLASSPATH% src\org\simbrain\gauge\core\*.java
javac -d bin -classpath %CLASSPATH% src\org\simbrain\gauge\graphics\*.java 
javac -d bin -classpath %CLASSPATH% src\org\simbrain\network\*.java
javac -d bin -classpath %CLASSPATH% src\org\simbrain\network\actions\*.java
javac -d bin -classpath %CLASSPATH% src\org\simbrain\network\dialog\*.java
javac -d bin -classpath %CLASSPATH% src\org\simbrain\network\dialog\network\*.java
javac -d bin -classpath %CLASSPATH% src\org\simbrain\network\dialog\network\layout\*.java 
javac -d bin -classpath %CLASSPATH% src\org\simbrain\network\dialog\neuron\*.java
javac -d bin -classpath %CLASSPATH% src\org\simbrain\network\dialog\synapse\*.java
javac -d bin -classpath %CLASSPATH% src\org\simbrain\network\filters\*.java
javac -d bin -classpath %CLASSPATH% src\org\simbrain\network\nodes\*.java
javac -d bin -classpath %CLASSPATH% src\org\simbrain\network\nodes\subnetworks\*.java
javac -d bin -classpath %CLASSPATH% src\org\simbrain\resource\*.java
javac -d bin -classpath %CLASSPATH% src\org\simbrain\util\*.java
javac -d bin -classpath %CLASSPATH% src\org\simbrain\workspace\*.java
javac -d bin -classpath %CLASSPATH% src\org\simbrain\world\*.java
javac -d bin -classpath %CLASSPATH% src\org\simbrain\world\dataworld\*.java
javac -d bin -classpath %CLASSPATH% src\org\simbrain\world\odorworld\*.java

javac -d bin -classpath %CLASSPATH% src\org\simnet\*.java
javac -d bin -classpath %CLASSPATH% src\org\simnet\coupling\*.java
javac -d bin -classpath %CLASSPATH% src\org\simnet\interfaces\*.java
javac -d bin -classpath %CLASSPATH% src\org\simnet\layouts\*.java
javac -d bin -classpath %CLASSPATH% src\org\simnet\networks\*.java
javac -d bin -classpath %CLASSPATH% src\org\simnet\neurons\*.java
javac -d bin -classpath %CLASSPATH% src\org\simnet\synapses\*.java
javac -d bin -classpath %CLASSPATH% src\org\simnet\synapses\spikeresponders\*.java
javac -d bin -classpath %CLASSPATH% src\org\simnet\util\*.java
copy src\org\simbrain\resource\*.gif bin\org\simbrain\resource
cd scripts
