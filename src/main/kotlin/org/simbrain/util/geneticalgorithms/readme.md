val workspace = useWorkspace()

val network1 = useNetwork()

val network2 = useNetwork()

on eval {
    workspace.product // workspace in eval has product functionalities
}

on build { // GenericContext
    workspace { // workspace in build function has scoping functionalities
        // WorkspaceContext
        network1 { // network1 scope
            // NetworkContext
            ...
        } // after invoke, put into product map
    }
    network2 { ... } // network2 is a standalone network, not network component
}

GenericContext < WorkspaceContext (network, odorworld)
               < NetworkContext (+nodeGene, +connectionGene)
               
workspace // provider
workspace { context.() -> }

network // provider
network { context.() -> } // returns unit, but internally produce a builder and added to super

fun network(template: BuilderContext.() -> Unit) {
  val thing = thing() // actual builder
  thing.use(template)
  addThing(thing) // provided by super scope
}

## Provider
provides different functionalities based on the scope
e.g.
provides `#product` in eval scope
```
onEval {
    use(workspace) {
        iterate(100)
    }
}
```

So this probably means 

```
fun <T> EvalContext.use(provider: Provider<T>, block: T.() -> Unit) {
    productMap[provider].block()
}
```

I couldn't do `EvalContext.Provider.product` so have to settle with this syntax for now.

there should be a provider to product mapping so that the T context can be provided

provides `#invoke(context.() -> Unit)` in builder
```
onBuild { // top-level builder context
    workspace { // workspace builder context
        ...
    }
}
```
workspace can only be invoke in the top-level builder context

P is provider, like useWorkspace {
  should effectively have EvalContext.product +
  should effectively have BuilderContext.invoke +
  
  should be able to make a T (createProduct), example:
  
  fun createProduct(template: WorkspaceContext.() -> Unit) {
    return WorkspaceContext().apply(template).workspace
  }
}
C is context, like workspace builder context {

  should have all the necessary helper functions (e.g. +nodeGene) +
  
}
C.() -> Unit is template, or everything within the builder block
T is product, like the workspace itself

createProduct(C.() -> Unit) returns T

B is builder {
   should have a productMap +
   should be able to make a T for P from a template +
   
   has the actual product
   
   this object is created everytime the builder is run +
}

```
class TopLevelBuilderContext {
    fun <P, C, T> P.invoke(template: C.() -> Unit) 
        where
            C : Context<T>
            P : Provider<C, T>,
            P: TopLevelBuilderContextInvokable {
        productMap[this] = this.createProduct(template) // result is T
    }
}
```
               
workspace { // provides a function that allows other builders
    // I want this to do 2 things: 1) make a product for product map, 2) add the product to workspace
    <C, T: WorkspaceBuilderContext<C>> T.invoke(template: C.() -> Unit) {
        buildWith(template) // T should always have this function
        addProductTo(this@outer.product) // only WorsapceChildren should have this function
    }
   

// WorkspaceSpecificBuilder looks like TopLevelBuilder, but should not inherit from it.

NetworkBuilder<NetworkBuilderContext>: WorkspaceChildren {
    fun buildWith(template: NetworkBuilderContext.() -> Unit)
}

fun TopLevelContext.network(template: NetworkContext.() -> Unit) {
    addBuilder {
        NetworkContext.apply(template).product
    }
}

NetworkBuilderContext {
    addNeuron
    addSynapse
    etc.
}

why isn't chromosome a provider
- it is being used in both the mutation block and the build block
- builder is static and runs through every line, but chromosome is mutating and memoized somewhere else
 