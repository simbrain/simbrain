package org.simbrain.util.geneticalgorithms

import org.simbrain.workspace.Consumer
import org.simbrain.workspace.Producer
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.couplings.CouplingManager


interface ProducerGene<T> {

    fun CouplingManager.defaultProducer(container: T): Producer?

}

interface ConsumerGene<T> {
    fun CouplingManager.defaultConsumer(container: T): Consumer?
}

class WorkspaceGeneticBuilder(override val productMap: ProductMap): GeneticBuilder<Workspace> {

    private val builders = ArrayList<(Workspace) -> Unit>()

    fun add(template: (Workspace) -> Unit) {
        builders.add(template)
    }

    fun build(): Workspace {
        return Workspace().also { workspace -> builders.forEach { it(workspace) } }
    }

}

class WorkspaceBuilderProvider: BuilderProvider<Workspace, WorkspaceGeneticBuilder, WorkspaceBuilderContext>,
        TopLevelBuilderContextInvokable<WorkspaceBuilderContext, Workspace> {

    fun createBuilder(productMap: ProductMap): WorkspaceGeneticBuilder {
        return WorkspaceGeneticBuilder(productMap)
    }

    fun createContext(builder: WorkspaceGeneticBuilder): WorkspaceBuilderContext {
        return WorkspaceBuilderContext(builder)
    }

    override fun createProduct(productMap: ProductMap, template: WorkspaceBuilderContext.() -> Unit): Workspace {
        return createBuilder(productMap).also { createContext(it).apply(template) }.build()
    }

}

interface WorkspaceBuilderContextInvokable<C: BuilderContext, T> {
    fun createProduct(productMap: ProductMap, template: C.() -> Unit): T
    fun createWorkspaceComponent(name: String): WorkspaceComponent
}

class WorkspaceBuilderContext(val builder: WorkspaceGeneticBuilder): BuilderContext {

    inline operator fun <P, B, C, reified T> P.invoke(name: String? = null, noinline template: C.() -> Unit)
            where
            B : GeneticBuilder<T>,
            C : BuilderContext,
            P : BuilderProvider<T, B, C>,
            P : WorkspaceBuilderContextInvokable<C, T> {
        builder.productMap[this] = createProduct(builder.productMap, template)
        val defaultName = T::class.java.simpleName
        builder.add { workspace -> workspace.addWorkspaceComponent(createWorkspaceComponent(name ?: defaultName)) }
    }

    fun couplingManager(template: CouplingManagerContext.() -> Unit) {
        builder.add { workspace -> CouplingManagerContext(builder, workspace.couplingManager).apply(template) }
    }

}

class CouplingManagerContext(val builder: WorkspaceGeneticBuilder, val couplingManager: CouplingManager) {

    fun <T1, G1, T2, G2> couple(source: Chromosome<T1, G1>, target: Chromosome<T2, G2>)
            where
            G1: Gene<T1>,
            G1: ProducerGene<T1>,
            G2: Gene<T2>,
            G2: ConsumerGene<T2> {

        with(couplingManager) {
            (source.genes.asSequence() zip target.genes.asSequence()).forEach { (producerGene, consumerGene) ->
                createCoupling(
                        with(producerGene) { defaultProducer(builder.productMap[this]) },
                        with(consumerGene) { defaultConsumer(builder.productMap[this]) }
                )
            }
        }

    }

    fun <T1, G1, T2, G2> couple(source: G1, target: G2)
            where
            G1: Gene<T1>,
            G1: ProducerGene<T1>,
            G2: Gene<T2>,
            G2: ConsumerGene<T2> {

        with(couplingManager) {
            createCoupling(
                    with(source) { defaultProducer(builder.productMap[this]) },
                    with(target) { defaultConsumer(builder.productMap[this]) }
            )
        }

    }


}

//interface WorkspaceComponentBuilderContext<T>: BuilderContext<T> {
//    fun getBuilderFunction(): (Workspace) -> Unit
//}
//
///**
// * Marker interface for builders that can be used in a workspace builder scope
// */
//interface WorkspaceComponentContext<T, B: WorkspaceComponentBuilderContext<T>>: BuilderProvider<T, B> {
//    fun addProductTo(workspace: Workspace)
//}

fun useWorkspace() = WorkspaceBuilderProvider()