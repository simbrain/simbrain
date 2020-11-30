package org.simbrain.util.geneticalgorithms

import org.simbrain.workspace.Workspace
import org.simbrain.workspace.WorkspaceComponent

class WorkspaceGeneticBuilder(override val productMap: ProductMap): GeneticBuilder<Workspace> {

    private val builders = ArrayList<(Workspace) -> Unit>()

    fun add(template: (Workspace) -> Unit) {
        builders.add(template)
    }

    override fun build(): Workspace {
        return Workspace().also { workspace -> builders.forEach { it(workspace) } }
    }

}

class WorkspaceBuilderProvider: BuilderProvider<Workspace, WorkspaceGeneticBuilder, WorkspaceBuilderContext>,
        TopLevelBuilderContextInvokable {

    override fun createBuilder(productMap: ProductMap): WorkspaceGeneticBuilder {
        return WorkspaceGeneticBuilder(productMap)
    }

    override fun createContext(builder: WorkspaceGeneticBuilder): WorkspaceBuilderContext {
        return WorkspaceBuilderContext(builder)
    }

}

interface WorkspaceBuilderContextInvokable {
    fun createWorkspaceComponent(name: String): WorkspaceComponent
}

class WorkspaceBuilderContext(val builder: WorkspaceGeneticBuilder): BuilderContext {

    inline operator fun <P, B, C, reified T> P.invoke(name: String? = null, noinline template: C.() -> Unit)
            where
            B : GeneticBuilder<T>,
            C : BuilderContext,
            P : BuilderProvider<T, B, C>,
            P : WorkspaceBuilderContextInvokable {
        builder.productMap[this] = createProduct(builder.productMap, template)
        val defaultName = T::class.java.simpleName
        builder.add { workspace -> workspace.addWorkspaceComponent(createWorkspaceComponent(name ?: defaultName)) }
    }

    fun couplingManager(template: CouplingManagerContext.() -> Unit) {
        builder.add { workspace -> workspace.couplingManager }
    }

}

class CouplingManagerContext {
    operator fun <T1, T2, G1 : Gene<T1>, G2 : Gene<T2>> Chromosome<T1, G1>.plus(
            other: Chromosome<T2, G2>
    ) = listOf(this, other)
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