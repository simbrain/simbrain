import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream
import java.time.Duration

/**
 * Main Simbrain build. Many of these functions are called by platform-specific workflow actions in ./github/workflows
 *
 * To build add relevant bracketed command to commit message (see tops of the .yaml files).  E.g "[push macos]"
 *
 * To build all just use all of them: "[push macos][push windows][push linux]"
 */

plugins {
    `java-library`
    idea
    application
    kotlin("jvm") version "1.9.22"
    id("ua.eshepelyuk.ManifestClasspath") version "1.0.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

val versionName = "4Beta"
val version = "4.0.0"
val docs = "docs"
val dist = "${buildDir}/dist"
val buildMain = "${buildDir}/main"

val includeAllPlatforms = project.findProperty("includeAllPlatforms")?.toString()?.toBoolean() ?: false

project.version = version

val simbrainJvmArgs = listOf(
    "--add-opens", "java.base/java.util=ALL-UNNAMED",
    "--add-opens", "java.desktop/java.awt=ALL-UNNAMED",
    "--add-opens", "java.desktop/java.awt.geom=ALL-UNNAMED",
    "--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED",
    "--add-opens", "java.base/java.util.concurrent.atomic=ALL-UNNAMED",
    "--add-opens", "java.base/java.lang=ALL-UNNAMED"
)

application {
    mainClass.set("org.simbrain.workspace.gui.Splasher")
    applicationDefaultJvmArgs = simbrainJvmArgs
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

val openBlasVersion = "0.3.26-1.5.10"
val javacppVersion = "1.5.10"
val arpackVersion = "3.9.1-1.5.10"

val excludeNatives: Action<ExternalModuleDependency> = Action {
    exclude(group = "org.bytedeco", module = "openblas")
    exclude(group = "org.bytedeco", module = "javacpp")
    exclude(group = "org.bytedeco", module = "arpack-ng")
}

dependencies {

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")

    // Smile
    implementation(
        group = "com.github.haifengl",
        name = "smile-core",
        version = "3.1.0",
        dependencyConfiguration = excludeNatives
    )
    implementation("com.github.haifengl:smile-kotlin:3.1.0", dependencyConfiguration = excludeNatives)
    implementation("com.github.haifengl:smile-plot:3.1.0", dependencyConfiguration = excludeNatives)
    implementation("com.github.haifengl:smile-nlp:3.1.0", dependencyConfiguration = excludeNatives)

    implementation("org.bytedeco:openblas:${openBlasVersion}")
    implementation("org.bytedeco:javacpp:${javacppVersion}")
    implementation("org.bytedeco:arpack-ng:${arpackVersion}")

    val platformSpecificDependencies = mapOf(
        "macosx" to listOf(
            "org.bytedeco:openblas:${openBlasVersion}:macosx-arm64",
            "org.bytedeco:openblas:${openBlasVersion}:macosx-x86_64",
            "org.bytedeco:javacpp:${javacppVersion}:macosx-arm64",
            "org.bytedeco:javacpp:${javacppVersion}:macosx-x86_64",
            "org.bytedeco:arpack-ng:${arpackVersion}:macosx-x86_64"
        ),
        "linux" to listOf(
            "org.bytedeco:openblas:${openBlasVersion}:linux-arm64",
            "org.bytedeco:openblas:${openBlasVersion}:linux-x86_64",
            "org.bytedeco:javacpp:${javacppVersion}:linux-arm64",
            "org.bytedeco:javacpp:${javacppVersion}:linux-x86_64",
            "org.bytedeco:arpack-ng:${arpackVersion}:linux-arm64",
            "org.bytedeco:arpack-ng:${arpackVersion}:linux-x86_64"
        ),
        "windows" to listOf(
            "org.bytedeco:openblas:${openBlasVersion}:windows-x86_64",
            "org.bytedeco:javacpp:${javacppVersion}:windows-x86_64",
            "org.bytedeco:arpack-ng:${arpackVersion}:windows-x86_64"
        )
    )

    if (includeAllPlatforms) {
        platformSpecificDependencies.values.flatten().forEach(::implementation)
    } else {
        when {
            OperatingSystem.current().isMacOsX -> "macosx"
            OperatingSystem.current().isLinux -> "linux"
            OperatingSystem.current().isWindows -> "windows"
            else -> throw GradleException("Unsupported platform: ${OperatingSystem.current().name}")
        }.let {
            platformSpecificDependencies[it]!!.forEach(::implementation)
        }
    }

    // JUnit
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation(platform("org.junit:junit-bom:5.7.1"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")

    // https://mvnrepository.com/artifact/org.apache.commons/commons-math3
    implementation(group = "org.apache.commons", name = "commons-math3", version = "3.6.1")

    // jsoup HTML parser library @ https://jsoup.org/
    implementation("org.jsoup:jsoup:1.15.4")

    // https://mvnrepository.com/artifact/org.ostermiller/utils
    implementation(group = "org.ostermiller", name = "utils", version = "1.07.00")

    // https://mvnrepository.com/artifact/org.tinylog/tinylog/1.3.6
    implementation(group = "org.tinylog", name = "tinylog", version = "1.3.6")
    // https://mvnrepository.com/artifact/org.tinylog/tinylog-impl
    runtimeOnly(group = "org.tinylog", name = "tinylog-impl", version = "2.6.1")
    // https://mvnrepository.com/artifact/org.tinylog/slf4j-tinylog
    runtimeOnly(group = "org.tinylog", name = "slf4j-tinylog", version = "2.6.1")

    // https://mvnrepository.com/artifact/com.thoughtworks.xstream/xstream
    implementation(group = "com.thoughtworks.xstream", name = "xstream", version = "1.4.20")

    // https://mvnrepository.com/artifact/org.piccolo2d/piccolo2d-extras
    implementation(group = "org.piccolo2d", name = "piccolo2d-extras", version = "3.0.1")
    implementation(group = "org.piccolo2d", name = "piccolo2d-core", version = "3.0.1")

    // https://mvnrepository.com/artifact/org.jfree/jfreechart
    implementation(group = "org.jfree", name = "jfreechart", version = "1.5.4")

    // https://mvnrepository.com/artifact/org.swinglabs/swingx-core
    implementation(group = "org.swinglabs", name = "swingx-core", version = "1.6.2-2")

    // https://mvnrepository.com/artifact/com.miglayout/miglayout-swing
    implementation(group = "com.miglayout", name = "miglayout-swing", version = "11.0")

    // https://mvnrepository.com/artifact/com.fifesoft/rsyntaxtextarea
    implementation(group = "com.fifesoft", name = "rsyntaxtextarea", version = "3.4.0")

    // https://mvnrepository.com/artifact/org.beanshell/bsh
    implementation(group = "org.beanshell", name = "bsh", version = "2.0b5")

    implementation(group = "org.jetbrains", name = "markdown", version = "0.7.3")

}

tasks.test {
    jvmArgs(simbrainJvmArgs)
    useJUnitPlatform()
}

// Sample invocation:
// gradle runSim -PsimName="Test Sim"
// gradle runSim -PsimName="Evolve Grazing Cows" -PoptionString="2:20:1000:100:0.5:true"
// Option string: numCows:maxGenerations:iterationsPerRun:populationSize:elimRatio:useAverage
tasks.register<JavaExec>("runSim") {
    jvmArgs(simbrainJvmArgs)
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.simbrain.custom_sims.RegisteredSimulationsKt")
    if (project.hasProperty("simName")) {
        if (project.hasProperty("optionString")) {
            args(project.property("simName") as String, project.property("optionString") as String)
        } else {
            args(project.property("simName") as String)
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf(
            "-Xuse-experimental=kotlin.experimental.ExperimentalTypeInference",
            "-Xjvm-default=all",
            "-Xcontext-receivers"
        )
    }
}

// Configure the shadow plugin
tasks.shadowJar {
    archiveClassifier.set("shadow")
    manifest {
        attributes(
            "Main-Class" to "org.simbrain.workspace.gui.Splasher"
        )
    }
    archiveFileName.set("Simbrain.jar")
}

tasks.register<Copy>("buildDistribution") {
    dependsOn("shadowJar")

    doFirst {
        from("${buildDir}/libs/Simbrain.jar")
    }

    // Copy simulations
    from("simulations") {
        exclude("**/archives/**")
        into("simulations")
    }

    from("scripts") {
        into("scripts")
    }

    from("etc/License.txt")

    // Set the base destination directory for all copy operations
    into(buildMain)
}

tasks.register("cleanDistribution") {
    doLast {
        delete(dist)
    }
}

if (OperatingSystem.current().isMacOsX) {

    tasks.register<Exec>("jpackageMacOS") {
        onlyIf { OperatingSystem.current().isMacOsX }

        if (!File(buildMain).exists()) {
            throw GradleException("Build directory does not exist. Run the 'buildDistribution' task first.")
        }

        val iconFile = "etc/simbrain.icns"

        val javaHome = System.getProperty("java.home")
        val jpackageBinary = if (OperatingSystem.current().isWindows) "jpackage.exe" else "jpackage"
        val jpackagePath = file("${javaHome}/bin/$jpackageBinary")

        doFirst {
            // Define JVM arguments
            val jvmArgs = listOf(
                "-Duser.dir=\$APPDIR",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
                "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED"
            ).joinToString(" ")

            // Set up the jpackage command and its arguments
            executable(jpackagePath)
            args(
                "--input", buildMain,
                "--main-jar", "Simbrain.jar",
                "--dest", dist,
                "--name", "Simbrain",
                "--app-version", project.version,
                "--mac-sign",
                "--mac-signing-key-user-name", "Regents of the University of CA, Merced (W8BB6W47ZR)",
                "--icon", iconFile,
                "--java-options", jvmArgs,
                "--type", "app-image",
            )
        }

        // Timeout after 10 minutes if hanging
        timeout.set(Duration.ofMinutes(10L))
        // Redirect output and error streams to help with debugging
        standardOutput = System.out
        errorOutput = System.err
    }

    tasks.named("jpackageMacOS").configure {
        logging.captureStandardOutput(LogLevel.INFO)
        logging.captureStandardError(LogLevel.ERROR)
    }

    open class NotarizeMacApp : DefaultTask() {

        @Input
        var distPath: String = ""

        @Input
        var versionString: String = ""

        @TaskAction
        fun notarize() {
            val notarizationProfileName = "AC_PASSWORD"
            val distDir = File(distPath)
            val dmgFile = File(distDir, "Simbrain${versionString}.dmg")

            // Create .dmg file
            project.exec {
                commandLine(
                    "hdiutil",
                    "create",
                    "-volname",
                    versionString,
                    "-srcfolder",
                    "${distDir.path}/Simbrain.app",
                    "-ov",
                    "-format",
                    "UDZO",
                    dmgFile.path
                )
            }

            // Delete Simbrain.app
            File("${distDir.path}/Simbrain.app").deleteRecursively()

            // Submit .dmg for notarization and wait
            val submitOutputStream = ByteArrayOutputStream()
            project.exec {
                commandLine(
                    "xcrun",
                    "notarytool",
                    "submit",
                    dmgFile.path,
                    "-p", notarizationProfileName,
                    "--wait",
                    "-v",
                    "--output-format", "json"
                )
                standardOutput = submitOutputStream
            }
            val notarizationOutput = submitOutputStream.toString()
            println("Notarization Output: $notarizationOutput")

            // Save JSON output to a temporary file for parsing with jq
            val tempFile = File.createTempFile("notarization", ".json")
            tempFile.writeText(notarizationOutput)

            // Parse JSON output with jq to get notarization status and UUID
            val statusOutputStream = ByteArrayOutputStream()
            project.exec {
                commandLine("jq", "-r", ".status", tempFile.path)
                standardOutput = statusOutputStream
            }
            val status = statusOutputStream.toString().trim()

            val uuidOutputStream = ByteArrayOutputStream()
            project.exec {
                commandLine("jq", "-r", ".id", tempFile.path)
                standardOutput = uuidOutputStream
            }
            val uuid = uuidOutputStream.toString().trim()

            // Delete the temporary file
            tempFile.delete()

            // Check notarization status and staple if accepted
            if (status == "Accepted") {
                println("Application has been accepted for notarization. Stapling ticket to .dmg and application is ready for distribution.")
                project.exec {
                    commandLine("xcrun", "stapler", "staple", dmgFile.path)
                }
            } else {
                println("Application has not been accepted for notarization. Fetching detailed logs...")
                val logOutputStream = ByteArrayOutputStream()
                project.exec {
                    commandLine("xcrun", "notarytool", "log", uuid, "-p", notarizationProfileName)
                    standardOutput = logOutputStream
                }
                val logOutput = logOutputStream.toString()
                println("Detailed Notarization Log:\n$logOutput")
            }
        }
    }

    tasks.register<NotarizeMacApp>("notarizeMacApp") {
        onlyIf { OperatingSystem.current().isMacOsX }
        distPath = dist
        versionString = versionName
    }
}

if (OperatingSystem.current().isWindows) {
    tasks.register<Exec>("jpackageWindows") {
        onlyIf { OperatingSystem.current().isWindows }

        dependsOn("cleanDistribution")
        dependsOn("shadowJar")
        dependsOn("buildDistribution")

        val iconFile = "etc/simbrain.ico"

        val javaHome = System.getProperty("java.home")
        val jpackageBinary = if (OperatingSystem.current().isWindows) "jpackage.exe" else "jpackage"
        val jpackagePath = file("${javaHome}/bin/$jpackageBinary")

        doFirst {
            // Define JVM arguments
            val jvmArgs = listOf(
                "-Duser.dir=\$APPDIR",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
                "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED"
            ).joinToString(" ")

            // Set up the jpackage command and its arguments
            executable(jpackagePath)
            args(
                "--input", buildMain,
                "--main-jar", "Simbrain.jar",
                "--dest", dist,
                "--name", "Simbrain",
                "--app-version", project.version,
                "--icon", iconFile,
                "--java-options", jvmArgs,
                "--win-menu",
                "--win-menu-group", "Simbrain",
                "--vendor", "Simbrain"
            )
        }
    }

    tasks.register<Exec>("signWindowsApp") {
        onlyIf { OperatingSystem.current().isWindows }

        dependsOn("jpackageWindows")

        val appPath = "${dist}/Simbrain-${project.version}.exe"

        val signtool = findWindowsSignTool()

        doFirst {

            executable(signtool)
            args(
                "sign",
                "/v",
                "/sm",
                "/s",
                "My",
                "/sha1",
                System.getenv("CERTIFICATE_SHA1"),
                "/fd",
                "SHA256",
                appPath
            )
        }

        doLast {
            val distDir = file(dist)
            val oldFile = File(distDir, "Simbrain-${project.version}.exe")
            val newFile = File(distDir, "Simbrain${versionName}.exe")

            if (oldFile.exists()) {
                val success = oldFile.renameTo(newFile)
                if (!success) {
                    throw GradleException("Failed to rename file from ${oldFile.name} to ${newFile.name}.")
                } else {
                    println("Signed executable is available at ${newFile.absolutePath}")
                }
            } else {
                throw GradleException("File ${oldFile.name} does not exist.")
            }
        }
    }
}

/**
 * Run script for Linux distribution. Avoids headaches of file copying and maintenance for a specific distribution.
 */
val runScriptFile = File.createTempFile("run", ".sh").apply {
    val dollar = "$"
    writeText(
        """
        #!/bin/bash

        # Check if Java is installed and if the version is 17 or higher
        java_version=${'$'}(java -version 2>&1 | head -n 1 | awk -F\" '{print ${'$'}2}' | awk -F\\. '{print ${'$'}1}')
        jdk_folder="jdk-17"
        if [[ -z "${dollar}java_version" ]] || [[ "${dollar}java_version" -lt 17 ]]; then
            if [[ ! -d "${dollar}jdk_folder" ]]; then
                echo "Java 17 or higher not found. Downloading Azul Zulu JDK 17..."
                os_name=${dollar}(uname -s)
                os_arch=${dollar}(uname -m)

                if [[ "${dollar}os_name" == "Linux" ]]; then
                    if [[ "${dollar}os_arch" == "x86_64" ]]; then
                        jdk_url="https://cdn.azul.com/zulu/bin/zulu17.30.15-ca-jdk17.0.1-linux_x64.tar.gz"
                    elif [[ "${dollar}os_arch" == "aarch64" ]]; then
                        jdk_url="https://cdn.azul.com/zulu/bin/zulu17.30.15-ca-jdk17.0.1-linux_aarch64.tar.gz"
                    fi
                elif [[ "${dollar}os_name" == "Darwin" ]]; then
                    if [[ "${dollar}os_arch" == "x86_64" ]]; then
                        jdk_url="https://cdn.azul.com/zulu/bin/zulu17.30.15-ca-jdk17.0.1-macosx_x64.tar.gz"
                    elif [[ "${dollar}os_arch" == "arm64" ]]; then
                        jdk_url="https://cdn.azul.com/zulu/bin/zulu17.30.15-ca-jdk17.0.1-macosx_aarch64.tar.gz"
                    fi
                fi

                if [[ -z "${dollar}jdk_url" ]]; then
                    echo "Unsupported platform: ${dollar}os_name ${dollar}os_arch"
                    exit 1
                fi

                mkdir -p "${dollar}jdk_folder"

                if command -v wget >/dev/null 2>&1; then
                    wget -q -O - "${dollar}jdk_url" | tar xz -C "${dollar}jdk_folder" --strip-components=1
                elif command -v curl >/dev/null 2>&1; then
                    curl -Ls "${dollar}jdk_url" | tar xz -C "${dollar}jdk_folder" --strip-components=1
                else
                    echo "Neither wget nor curl is available. Please install one of them and try again."
                    exit 1
                fi
            fi
            java_path="./${dollar}jdk_folder/bin/java"
        else
            java_path="java"
        fi

        # Run the jar using the appropriate Java version
        ${dollar}java_path -jar Simbrain.jar
    """.trimIndent()
    )
    setExecutable(true)
    deleteOnExit()
}

tasks.register<Zip>("createZip") {
    dependsOn("buildDistribution")
    archiveFileName.set("Simbrain${versionName}.zip")
    destinationDirectory.set(file(dist))
    // Include the run.sh file
    val dir = "Simbrain${versionName}"
    from(buildMain) {
        into(dir)
    }
    from(runScriptFile) {
        into(dir)
        rename { "run.sh" }
    }
}

fun findWindowsSignTool(): String {
    val windowsKitsFolder = File("C:/Program Files (x86)/Windows Kits/10/bin/")
    if (!windowsKitsFolder.exists()) {
        throw GradleException("Windows Kits folder not found")
    }

    // Find all signtool.exe files specifically in x64 directories under the SDK versions
    val signToolFiles = findSignToolRecursive(windowsKitsFolder) { it.path.contains("\\x64\\") }

    if (signToolFiles.isEmpty()) {
        println("Dumping directory structure for debugging:")
        dumpDirectoryTree(windowsKitsFolder)
        throw GradleException("SignTool.exe not found in the x64 SDK version folders")
    }

    return signToolFiles.first().absolutePath
}

fun findSignToolRecursive(directory: File, filter: (File) -> Boolean = { true }): List<File> {
    val foundFiles = mutableListOf<File>()
    directory.walk().forEach {
        if (it.isFile && it.name.equals("signtool.exe", ignoreCase = true) && filter(it)) {
            foundFiles.add(it)
        }
    }
    return foundFiles
}

fun dumpDirectoryTree(directory: File, prefix: String = "") {
    directory.listFiles()?.forEach {
        if (it.isDirectory) {
            println("$prefix${it.name}/")
            dumpDirectoryTree(it, "$prefix${it.name}/")
        } else {
            println("$prefix${it.name}")
        }
    }
}
