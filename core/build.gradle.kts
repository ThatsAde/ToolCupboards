dependencies {
    // `api` and not `implementation`: a consumer of :core cannot use it without the :api types,
    // and they have to end up on the runtime classpath so the jar below can bundle them.
    api(project(":api"))

    // Soft dependencies. paper-plugin.yml declares both with `required: false`, so they may be
    // absent at runtime — hence compileOnly, and hence every provider guarding on isAvailable().
    //
    // isTransitive = false on purpose: we only need their own classes to compile against, and
    // their full dependency graphs drag in artifacts that do not resolve.
    compileOnly("com.nexomc:nexo:1.26.0") { isTransitive = false }
    compileOnly("net.Indyuce:MMOItems-API:6.10.1-SNAPSHOT") { isTransitive = false }
    compileOnly("io.lumine:MythicLib-dist:1.7.1-SNAPSHOT") { isTransitive = false }

    // Nexo is written in Kotlin but ships no kotlin-stdlib and declares none in its POM, so it has
    // to be supplied here. javac survives without it because it only resolves what you actually
    // call, but IntelliJ resolves every member of a class it indexes: without this, NexoItems
    // resolves as a symbol yet none of its methods are usable.
    // Runtime is unaffected — the Nexo plugin brings its own copy.
    // (fastutil, also referenced by Nexo signatures, already arrives through paper-api.)
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.2.20")
}

// :core is also the plugin module: it holds the bootstrap, src/main/resources and the final jar.

// Lets paper-plugin.yml declare `version: '${version}'` and stay in sync with the Gradle version.
tasks.processResources {
    val properties = mapOf("version" to project.version)
    inputs.properties(properties)
    filesMatching("paper-plugin.yml") {
        expand(properties)
    }
}

tasks.jar {
    // Otherwise the artifact would be named after the module, and the server would load `core.jar`.
    archiveBaseName.set("ToolCupboards")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    // The server only ever sees one file, so :api has to travel inside it. paper-api is
    // compileOnly, so the runtime classpath is exactly what belongs in the jar.
    //
    // zipTree() over a mapped file list loses provenance, so :api:jar has to be requested
    // explicitly — otherwise this task runs before it has produced anything.
    dependsOn(configurations.runtimeClasspath)
    from(provider {
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
}
