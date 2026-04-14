package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Neuron
import org.simbrain.util.*
import org.simbrain.util.decayfunctions.StepDecayFunction
import org.simbrain.util.geneticalgorithm.*
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.WorkspacePreferences
import org.simbrain.workspace.serialization.WorkspaceSerializer
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.getRandomLocation
import org.simbrain.world.odorworld.sensors.ObjectSensor
import java.awt.Dimension
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

/**
 * To run from command line, use:
 * `gradle runSim -PsimName="Evolve Grazing Cows" -PoptionString="2:20:1000:100:0.5:true"`
 * The resulting zip file must be loaded using the `load file` button in this sim
 */
val grazingCows = newSim { optionString ->

    var numCows = 2
    var maxGenerations = 50
    var iterationsPerRun = 2000
    var populationSize = 100
    var eliminationRatio = .5
    var numFlowers = 10
    // If not, use min to compute group level fitness across cows
    var useAverage = false

    class CowGenotype(seed: Long = Random.nextLong()) : SlotGenotype(seed) {

        // 6 sensor neurons (3 dandelion + 3 cow) + 1 drive neuron
        val inputs by nodeChromosome(7) { clamped = true }
        val hidden by nodeChromosome(2)
        val outputs by nodeChromosome(3) { upperBound = 10.0; lowerBound = -10.0 }
        val connections by connectionChromosome()

        init {
            // Set drive neuron activation
            inputs.genes.last().template.activation = 1.0
            // Random initial connections
            repeat(3) {
                connections.chromosome.add(connectionGene(inputs.genes.random(random), hidden.genes.random(random)))
                connections.chromosome.add(connectionGene(hidden.genes.random(random), outputs.genes.random(random)))
            }
            // Extra drive connection
            connections.chromosome.add(connectionGene(inputs.genes[3], hidden.genes.random(random)))
        }

        override fun createNew(seed: Long) = CowGenotype(seed)

        override fun mutate() {
            hidden.genes.forEach { it.mutate { bias += random.nextDouble(-1.0, 1.0) } }
            connections.genes.forEach { it.mutate { strength += random.nextDouble(-1.0, 1.0) } }

            val existingPairs = connections.genes.map { it.source to it.target }.toSet()
            val availableConnections =
                ((inputs.chromosome + hidden.chromosome + outputs.chromosome) cartesianProduct (hidden.chromosome + outputs.chromosome)) - existingPairs
            if (random.nextDouble() < 0.25 && availableConnections.isNotEmpty()) {
                val (source, target) = availableConnections.sampleOne(random)
                connections.chromosome.add(connectionGene(source, target) { strength = random.nextDouble(-1.0, 1.0) })
            }

            if (random.nextDouble() < 0.1) {
                hidden.addGene(nodeGene())
            }
        }
    }

    // What to do when a cow finds flower
    fun addFindFlowerAction(workspace: Workspace, entity: OdorWorldEntity, fitnessLambda: (Double) -> Unit = {}) {
        val world = entity.world
        workspace.addUpdateAction("${entity.name} found a flower") {
            (entity.getSensor("centralFlowerSensor") as ObjectSensor).let { sensor ->
                // Flowers found
                sensor.getSensedObjects(entity, .5).forEach {
                    it.location = world.getRandomLocation()
                    // Update fitness
                    fitnessLambda(1.0)
                }
            }

        }
    }


    class CowSim(
        val cowGenotypes: List<CowGenotype> = List(numCows) { CowGenotype() },
        val workspace: Workspace = Workspace()
    ) : EvoSim {

        val random = Random(cowGenotypes.first().random.nextInt())

        val cowFitnesses = mutableMapOf<Int, Double>()
        private var built = false

        val odorWorld = OdorWorldComponent("Odor World 1").also {
            workspace.addWorkspaceComponent(it)
        }.world.apply {
            launch {
                isObjectsBlockMovement = false
                with(tileMap) {
                    updateMapSize(25, 25)
                    fill("Grass1")
                }
            }
        }

        val networks = List(cowGenotypes.size) { index ->
            NetworkComponent("Network ${index + 1}").also { workspace.addWorkspaceComponent(it) }.network
        }
        val entities = runBlocking {
            List(cowGenotypes.size) { i ->
                OdorWorldEntity(odorWorld, EntityType.Cow).also {
                    odorWorld.addEntity(it)
                    it.location = point((i + 1) * 100, (i + 1) * 100)
                }
        }
        }

        val dandelionSensors = entities.map { entity ->
            List(3) { index ->
                ObjectSensor(EntityType.Dandelions, radius = 60.0, theta = (index * 120.0)).apply {
                    decayFunction.dispersion = 250.0
                }.also { entity.addSensor(it) }
            }
        }

        val cowSensors = entities.map { entity ->
            List(3) { index ->
                ObjectSensor(EntityType.Cow, radius = 50.0, theta = (index * 120.0)).apply {
                    decayFunction.dispersion = 200.0
                }.also { entity.addSensor(it) }
            }
        }


        val effectors = entities.map { entity ->
            entity.addDefaultEffectors()
            entity.effectors
        }

        init {
            // Central flower sensor to determine when the flower is actually found.
            entities.forEach {
                it.addSensor(
                    ObjectSensor(EntityType.Dandelions, radius = 0.0).apply {
                        label = "centralFlowerSensor"
                        decayFunction = StepDecayFunction()
                        decayFunction.dispersion = 30.0
                    }
                )
            }
            runBlocking {
                repeat(numFlowers) {
                    val loc = odorWorld.getRandomLocation()
                    odorWorld.addEntity(loc.x.toInt(), loc.y.toInt(),
                        EntityType.Dandelions, doubleArrayOf(1.0))
                }
            }
        }

        override suspend fun build() {
            if (!built) {
                // Express genotypes into networks
                cowGenotypes.zip(networks).forEach { (genotype, network) ->
                    genotype.expressAll(network)
                }
                // Couplings
                with(workspace.couplingManager) {
                    cowGenotypes.indices.forEach { i ->
                        (dandelionSensors[i] + cowSensors[i]) couple cowGenotypes[i].inputs.neurons.neuronList
                        cowGenotypes[i].outputs.neurons.neuronList couple effectors[i]
                    }
                }
                // Fitness tracking
                cowGenotypes.indices.forEach { i ->
                    cowFitnesses[i] = 0.0
                    addFindFlowerAction(workspace, entities[i]) { delta ->
                        cowFitnesses[i] = (cowFitnesses[i] ?: 0.0) + delta
                    }
                }
                built = true
            }
        }

        override fun mutate() {
            cowGenotypes.forEach { it.mutate() }
        }

        override fun visualize(workspace: Workspace) = CowSim(cowGenotypes.map { it.copyGenotype() as CowGenotype }, workspace)

        override fun copy() = CowSim(cowGenotypes.map { it.copyGenotype() as CowGenotype })

        override suspend fun eval(): Double {
            build()
            workspace.iterateSuspend(iterationsPerRun)
            // Determine a fitness for the sim based on the fitness of each cow
            return if (useAverage) {
                cowFitnesses.values.average()
            } else {
                cowFitnesses.values.minOrNull() ?: 0.0
            }
        }
    }

    fun displayBlock(genotype: CowGenotype): GeneDisplayBuilder.() -> Unit {
        val allNodes = genotype.inputs.genes + genotype.hidden.genes + genotype.outputs.genes
        fun nodeIndex(gene: NodeGene) = allNodes.indexOf(gene).let { if (it >= 0) it + 1 else "?" }
        return {
            display(genotype.inputs, noDefaults = true) {
                header(formatted("node") { nodeIndex(it) })
                +template(Neuron::clamped)
            }
            display(genotype.hidden) {
                header(formatted("node") { nodeIndex(it) })
            }
            display(genotype.outputs) {
                header(formatted("node") { nodeIndex(it) })
            }
            display(genotype.connections) {
                +formatted("in") { nodeIndex(it.source) }
                +formatted("out") { nodeIndex(it.target) }
            }
        }
    }

    suspend fun runSim() {
        withContext(workspace.coroutineContext) {
            val progressWindow = withGui {
                ProgressWindow(maxGenerations, "10th Percentile Fitness:").apply {
                    minimumSize = Dimension(300, 100)
                    setLocationRelativeTo(null)
                }
            }
            val genomeDisplays = List(numCows) { geneDisplayPanel(displayBlock = ::displayBlock) }
            withGui {
                genomeDisplays.forEachIndexed { i, panel ->
                    showGeneDisplay(panel, y = 400 + i * 320, title = "Cow ${i + 1} Genome")
                }
            }
            val runner = EvolutionRunner(
                populatingFunction = { CowSim() },
                populationSize = populationSize,
                eliminationRatio = eliminationRatio,
                stoppingFunction = { nthPercentileFitness(10) > 400 || generation > maxGenerations },
            )
            genomeDisplays.forEachIndexed { i, panel ->
                panel.bind(runner) { state ->
                    val g = (state.best as CowSim).cowGenotypes[i]
                    g to displayBlock(g)
                }
            }
            runner.onGeneration { state ->
                listOf(0, 10, 25, 50, 75, 90, 100).joinToString(" ") {
                    "$it: ${state.nthPercentileFitness(it).format(3)}"
                }.also {
                    println("[${state.generation}] $it")
                    progressWindow?.apply {
                        text = "10th Percentile Fitness: ${state.nthPercentileFitness(10).format(3)}"
                        value = state.generation
                    }
                }
            }
            val result = runner.run()
            with(result.best.visualize(workspace) as CowSim) {
                build()
                withGui {
                    workspace.componentList.filterIsInstance<OdorWorldComponent>().first().apply {
                        place(this, 280, 10, 476, 432)
                    }
                    workspace.componentList.filterIsInstance<NetworkComponent>().forEachIndexed { i, net ->
                        place(net, 768, 10 + i * 282, 326, 282)
                    }
                }
                cowGenotypes.forEach { g ->
                    g.inputs.neurons.location = point(0, 150)
                    g.hidden.neurons.location = point(0, 60)
                    g.outputs.neurons.location = point(0, -25)
                }
                cowGenotypes.zip(genomeDisplays).forEach { (genotype, panel) ->
                    panel.refreshFrom(genotype)
                }
                if (desktop == null) {
                    workspace.save(File("evolved_${SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())}.zip"), headless = true)
                }
            }
            progressWindow?.close()
        }
    }

    withGui {
        workspace.clearWorkspace()
        createControlPanel("Control Panel", 5, 10) {

            val numCowsTf = addTextField("Number of cows", "" + numCows)
            val maxGenTf = addTextField("Max Generations", "" + maxGenerations)
            val iterationsPerRunTf = addTextField("Num iterations per generation", "" + iterationsPerRun)
            val populationSizeTf = addTextField("Population size", "" + populationSize)
            val eliminationRatioTf = addTextField("Elimination ratio", "" + eliminationRatio)
            val useAverageCB = addCheckBox("Use mean group fitness (else min)", useAverage)

            addButton("Evolve") {
                workspace.removeAllComponents()
                numCows = numCowsTf.text.toInt();
                maxGenerations = maxGenTf.text.toInt()
                iterationsPerRun = iterationsPerRunTf.text.toInt()
                populationSize = populationSizeTf.text.toInt()
                eliminationRatio = eliminationRatioTf.text.toDouble()
                useAverage = useAverageCB.isSelected()
                runSim()
            }

            addButton("Load file") {
                val simulationChooser = SFileChooser(WorkspacePreferences.simulationDirectory, "Zip Archive", "zip")
                val simFile = simulationChooser.showOpenDialog()
                val serializer = WorkspaceSerializer(workspace)
                if (simFile != null) {
                    workspace.removeAllComponents()
                    workspace.updater.updateManager.reset()
                    withContext(Dispatchers.IO) {
                        serializer.deserialize(FileInputStream(simFile))
                    }
                }

                val world = workspace.componentList.filterIsInstance<OdorWorldComponent>().first().world
                world.entityList
                    .filter { e -> e.entityType == EntityType.Cow }
                    .forEach { addFindFlowerAction(workspace, it) }
            }
        }
    }

    addSidebarInfo(
        """
        # Evolving A Grazing Cow
        
        This is a simulation of the evolution of a neural network that is coupled to an agent in an odor world that contains food resources with the possibility of multiple agents. The neural network(s) will evolve to 
        optimize the simulation's performance using an evolutionary framework in Simbrain.
        
        # Simulation Details
        
        This simulation simulates the evolution of a neural network, or multiple neural networks until the target fitness is met, exceeded, or when it has reached the maximum generation. The goal of this simulation is to 
        evolve until it is as close as possible to the target fitness. By default, the target fitness is the 10th percentile, and if it is the target mean group fitness, the target fitness would be the 50th percentile.
        
        For a more in-depth look into how the fitness is calculated, use this [page](https://docs.simbrain.net/docs/simulations/) as a guide to see the simulation code. For a comprehensive look into how evolutionary 
        simulations are developed in Simbrain, look [here](https://docs.simbrain.net/docs/evolution/).
        
        ## Evolutionary Process
        
        The evolutionary process begins with a starting population size of simulations. In generation `0`, each simulation starts with a three-layer network (`7` input neurons, `2` hidden neurons, and `3` output 
        neurons) and a preset amount of connections (`4` in the input-hidden layer, `3` in the hidden-output layer). If the number of cows is more than `1`, there will be multiple networks that correspond to different
        agents in a simulation. Within each generation, a simulation will iterate until the specified value while the fitness of each simulation is calculated and recorded. When food is obtained, a value is added to
        the current fitness of the simulation and new food is repopulated at a random location in the odor world. All the agents will accumulate towards a single fitness value (the fitness of the simulation).
        
        Then after each generation, a percentage of the population is eliminated (e.g., `elimination ratio`) and repopulated with new simulations. During this process of reproduction, some of the new 
        simulations will have mutations, where the simulation develops new neurons in the hidden layer (`10%` chance), new connections between layers (`25%` chance), changes in neuron biases and weight strengths. 

        Then, a percentage of the top performers is evaluated (e.g, `Evaluation percentile`) to determine if the `target fitness` has been achieved. This process continues until the simulation has reached the target
        fitness or better, or when the evolutionary process ends.

        # What to Do
        
        In this simulation, similar to the other evolutionary simulations, the control panel controls how the evolutionary process works. Below are the steps to evolving the simulation:
        
        1) Specify the parameters of the simulation.
        
            - Number of agents (e.g., cows)
            
            - Use of mean group target fitness (`50th` percentile) vs default fitness (`10th` percentile)
        
        2) After confirming the parameters are what you want, click on the `Evolve` button to start the simulation.
        
        3) Now, wait for the evolution process to finish, note that it can take a while depending on your configurations.

        # Credits
 
        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        
        Kanly Thao
        
        """.trimIndent()
    )

    if (optionString?.isNotEmpty() == true) {
        val options = optionString.split(":")
        numCows = options[0].toInt()
        maxGenerations = options[1].toInt()
        iterationsPerRun = options[2].toInt()
        populationSize = options[3].toInt()
        eliminationRatio = options[4].toDouble()
        if (options.size > 5) {
            useAverage = options[5].toBoolean()
        }
        runSim()
    }

}
