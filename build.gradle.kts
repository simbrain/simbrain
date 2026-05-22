import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.time.Duration

/**
 * Main Simbrain build. Many of these functions are called by platform-specific workflow actions in ./github/workflows
 *
 * To build add relevant bracketed command to commit message (see tops of the .yaml files).  E.g "[push macos]"
 *
 * To build all just use all of them: "[push macos][push windows][push linux]"
 *
 * By default "[push macos]" builds for both silicon and intel. You can also use "[push macos arm64]" and "[push macos x64]"
 *
 */

plugins {
    `java-library`
    idea
    application
    kotlin("jvm") version "2.1.0"
    id("ua.eshepelyuk.ManifestClasspath") version "1.0.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

// Dynamic version from CI property or git tag, with fallback for local dev
val version: String = project.findProperty("releaseVersion")?.toString()
    ?: run {
        try {
            val stdout = ByteArrayOutputStream()
            exec {
                commandLine("git", "describe", "--tags", "--exact-match", "HEAD")
                standardOutput = stdout
                errorOutput = ByteArrayOutputStream()
                isIgnoreExitValue = true
            }
            stdout.toString().trim().takeIf { it.startsWith("v") }?.removePrefix("v")
        } catch (e: Exception) { null }
    } ?: "4.0.0"

// For consistency, versionName = version
val versionName = version
val isBeta = version.contains("-beta")

val docs = "docs"
val dist = "${buildDir}/dist"
val buildMain = "${buildDir}/main"

val includeAllPlatforms = project.findProperty("includeAllPlatforms")?.toString()?.toBoolean() ?: false
val versionSuffixString = project.findProperty("versionSuffix")?.toString() ?: ""
val linuxArch: String? = project.findProperty("linuxArch")?.toString()  // "x86_64" or "aarch64" for AppImage builds

// Build information from CI/CD
val buildNumber = project.findProperty("buildNumber")?.toString() ?: "dev"
val commitSha = project.findProperty("commitSha")?.toString() ?: "unknown"
val buildTimestamp = project.findProperty("buildTimestamp")?.toString() ?: "unknown"

project.version = version

val simbrainJvmArgs = listOf(
    "--add-opens", "java.base/java.util=ALL-UNNAMED",
    "--add-opens", "java.desktop/java.awt=ALL-UNNAMED",
    "--add-opens", "java.desktop/java.awt.geom=ALL-UNNAMED",
    "--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED",
    "--add-opens", "java.base/java.util.concurrent.atomic=ALL-UNNAMED",
    "--add-opens", "java.base/java.lang=ALL-UNNAMED",
    "-Dapple.laf.useScreenMenuBar=true",
    "-Dcom.apple.mrj.application.apple.menu.about.name=Simbrain",
    "-Dapple.awt.application.name=Simbrain"
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
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.1")

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
        "linux-x86_64" to listOf(
            "org.bytedeco:openblas:${openBlasVersion}:linux-x86_64",
            "org.bytedeco:javacpp:${javacppVersion}:linux-x86_64",
            "org.bytedeco:arpack-ng:${arpackVersion}:linux-x86_64"
        ),
        "linux-aarch64" to listOf(
            "org.bytedeco:openblas:${openBlasVersion}:linux-arm64",
            "org.bytedeco:javacpp:${javacppVersion}:linux-arm64",
            "org.bytedeco:arpack-ng:${arpackVersion}:linux-arm64"
        ),
        "windows" to listOf(
            "org.bytedeco:openblas:${openBlasVersion}:windows-x86_64",
            "org.bytedeco:javacpp:${javacppVersion}:windows-x86_64",
            "org.bytedeco:arpack-ng:${arpackVersion}:windows-x86_64"
        )
    )

    if (includeAllPlatforms) {
        platformSpecificDependencies.values.flatten().distinct().forEach(::implementation)
    } else {
        val platformKey = when {
            OperatingSystem.current().isMacOsX -> "macosx"
            OperatingSystem.current().isLinux -> linuxArch?.let { "linux-$it" } ?: "linux"
            OperatingSystem.current().isWindows -> "windows"
            else -> throw GradleException("Unsupported platform: ${OperatingSystem.current().name}")
        }
        platformSpecificDependencies[platformKey]!!.forEach(::implementation)
    }

    // JUnit
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation(platform("org.junit:junit-bom:5.7.1"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    // https://mvnrepository.com/artifact/org.apache.commons/commons-math3
    implementation(group = "org.apache.commons", name = "commons-math3", version = "3.6.1")

    // https://mvnrepository.com/artifact/org.apache.commons/commons-compress
    implementation(group = "org.apache.commons", name = "commons-compress", version = "1.28.0")

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

    // https://mvnrepository.com/artifact/org.json/json
    implementation("org.json:json:20240303")

    // JNA — calls into bundled libespeak-ng from PhonemeSynthesizer.
    implementation("net.java.dev.jna:jna:5.14.0")

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
        languageVersion = "2.1"
        freeCompilerArgs = listOf(
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
    
    doLast {
        println("=== SHADOW JAR VERIFICATION ===")
        val jarFile = archiveFile.get().asFile
        println("Shadow JAR created: ${jarFile.absolutePath}")
        println("JAR size: ${jarFile.length()} bytes")
        
        // Check if build-info.properties is in the JAR
        try {
            val process = ProcessBuilder("jar", "tf", jarFile.absolutePath)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val hasBuildInfo = output.contains("build-info.properties")
            
            if (hasBuildInfo) {
                println("✓ build-info.properties found in JAR")
            } else {
                println("✗ ERROR: build-info.properties NOT found in JAR")
                println("JAR contents (first 20 entries):")
                output.lines().take(20).forEach { println("  $it") }
            }
        } catch (e: Exception) {
            println("Could not verify JAR contents: ${e.message}")
        }
        println("=== END SHADOW JAR VERIFICATION ===")
    }
}

// Generate build info properties file
tasks.register("generateBuildInfo") {
    val outputDir = File("${buildDir}/generated-resources/main")
    val outputFile = File(outputDir, "build-info.properties")
    
    outputs.file(outputFile)
    
    doLast {
        println("=== BUILD INFO GENERATION ===")
        println("Version: ${version}")
        println("Is Beta: ${isBeta}")
        println("Build Number: ${buildNumber}")
        println("Commit SHA: ${commitSha}")
        println("Build Timestamp: ${buildTimestamp}")
        
        println("Creating build info directory: ${outputDir.absolutePath}")
        val dirCreated = outputDir.mkdirs()
        println("Directory created: ${dirCreated} (or already exists)")
        
        val content = """
            version=${version}
            versionName=${versionName}
            isBeta=${isBeta}
            buildNumber=${buildNumber}
            commitSha=${commitSha}
            buildTimestamp=${buildTimestamp}
        """.trimIndent()
        
        println("Writing build info to: ${outputFile.absolutePath}")
        outputFile.writeText(content)
        
        if (outputFile.exists()) {
            println("✓ Build info file created successfully")
            println("File size: ${outputFile.length()} bytes")
            println("File content:")
            println(outputFile.readText())
        } else {
            println("✗ ERROR: Build info file was not created!")
        }
        println("=== END BUILD INFO GENERATION ===")
    }
}

// Register generated build info directory as additional output
sourceSets {
    main {
        output.dir(tasks.named("generateBuildInfo"))
    }
}

tasks.register<Copy>("buildDistribution") {
    dependsOn("shadowJar")
    dependsOn("generateBuildInfo")
    dependsOn("buildEspeakNg")

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

    // Bundle eSpeak-ng for the host platform alongside Simbrain.jar.
    // jpackage on macOS picks this up via --input; the shadow zip ships it next to the jar.
    from(espeakNgInstallDir) {
        into("espeak-ng")
    }

    from("LICENSE")

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
                "--add-opens=java.desktop/java.awt.geom=ALL-UNNAMED",
                "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
                "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
                "--add-opens=java.base/java.lang=ALL-UNNAMED"
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
                "--mac-package-name", "Simbrain",
                "--mac-package-identifier", "org.simbrain"
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

        @Input
        var versionSuffix: String = ""

        @TaskAction
        fun notarize() {
            val notarizationProfileName = "AC_PASSWORD"
            val distDir = File(distPath)
            val dmgFile = File(distDir, "Simbrain${versionString}${versionSuffix}.dmg")

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
        versionSuffix = versionSuffixString
    }
}

if (OperatingSystem.current().isWindows) {

    val winJvmArgs = listOf(
        "-Duser.dir=\$APPDIR",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt.geom=ALL-UNNAMED",
        "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
        "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED"
    ).joinToString(" ")

    val jpackagePath = file("${System.getProperty("java.home")}/bin/jpackage.exe")

    // Directory of the jpackage app-image: holds the Simbrain.exe launcher and the bundled runtime.
    val winAppImageDir = "${dist}/Simbrain"

    // Phase 1 of 2: build only the app-image. Its Simbrain.exe launcher can then be code-signed
    // before being wrapped into the installer, so the installed program itself is signed and not
    // just the outer installer.
    tasks.register<Exec>("jpackageWindowsAppImage") {
        onlyIf { OperatingSystem.current().isWindows }

        dependsOn("cleanDistribution")
        dependsOn("shadowJar")
        dependsOn("buildDistribution")

        doFirst {
            // Windows Installer requires a numeric-only version (X.Y.Z); strip any pre-release suffix
            val winVersion = project.version.toString().replace(Regex("-.*"), "")
            executable(jpackagePath)
            args(
                "--type", "app-image",
                "--input", buildMain,
                "--main-jar", "Simbrain.jar",
                "--dest", dist,
                "--name", "Simbrain",
                "--app-version", winVersion,
                "--icon", "etc/simbrain.ico",
                "--java-options", winJvmArgs
            )
        }
    }

    // Phase 2 of 2: package the (by now signed) app-image into the installer .msi, then rename it.
    // An MSI is run directly by msiexec (no self-extracting wrapper), so signing the .msi itself
    // gives the install a verified publisher. Intentionally does NOT depend on jpackageWindowsAppImage
    // so CI can sign the app-image between the two phases without this task rebuilding and clobbering
    // the signed launcher.
    tasks.register<Exec>("jpackageWindowsInstaller") {
        onlyIf { OperatingSystem.current().isWindows }

        doFirst {
            if (!file(winAppImageDir).exists()) {
                throw GradleException("App-image not found at $winAppImageDir. Run jpackageWindowsAppImage first.")
            }
            val winVersion = project.version.toString().replace(Regex("-.*"), "")
            executable(jpackagePath)
            args(
                "--type", "msi",
                "--app-image", winAppImageDir,
                "--dest", dist,
                "--name", "Simbrain",
                "--app-version", winVersion,
                "--win-menu",
                "--win-menu-group", "Simbrain",
                "--vendor", "Simbrain"
            )
        }

        doLast {
            val distDir = file(dist)
            val winVersion = project.version.toString().replace(Regex("-.*"), "")
            val oldFile = File(distDir, "Simbrain-${winVersion}.msi")
            val newFile = File(distDir, "Simbrain${versionName}-installer.msi")
            if (!oldFile.exists()) {
                throw GradleException("File ${oldFile.name} does not exist.")
            }
            if (!oldFile.renameTo(newFile)) {
                throw GradleException("Failed to rename file from ${oldFile.name} to ${newFile.name}.")
            }
            println("Installer is available at ${newFile.absolutePath}")
        }
    }

    // Convenience aggregator for local, unsigned, end-to-end installer builds.
    tasks.register("jpackageWindows") {
        onlyIf { OperatingSystem.current().isWindows }
        dependsOn("jpackageWindowsAppImage", "jpackageWindowsInstaller")
    }
    tasks.named("jpackageWindowsInstaller").configure { mustRunAfter("jpackageWindowsAppImage") }
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
        ${dollar}java_path \
            --add-opens java.base/java.util=ALL-UNNAMED \
            --add-opens java.desktop/java.awt=ALL-UNNAMED \
            --add-opens java.desktop/java.awt.geom=ALL-UNNAMED \
            --add-opens java.base/java.util.concurrent=ALL-UNNAMED \
            --add-opens java.base/java.util.concurrent.atomic=ALL-UNNAMED \
            --add-opens java.base/java.lang=ALL-UNNAMED \
            -jar Simbrain.jar
    """.trimIndent()
    )
    setExecutable(true)
    deleteOnExit()
}

tasks.register<Zip>("createZip") {
    dependsOn("buildDistribution")
    archiveFileName.set("Simbrain${versionName}${versionSuffixString}.zip")
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

// eSpeak-ng bundling.
//
// Builds the eSpeak-ng phoneme synthesizer from source for the host platform so it can be
// shipped inside the Simbrain distribution. PhonemeSynthesizer dlopens libespeak-ng via JNA
// and drives it through espeak_Synth, so the trimmed flags drop unused features (audio
// output backend, async pipeline, MBROLA) we don't invoke.
//
// Tasks:
//   fetchEspeakNgSource — downloads + verifies the source tarball
//   buildEspeakNg       — runs cmake/make for the host platform; outputs to
//                         build/espeak-ng/<platform>/{bin,lib,share}
//
// `buildEspeakNg` is opt-in for plain `./gradlew run` — Simbrain dev startup doesn't pay the
// build cost. If the lib isn't present at runtime, audio is disabled with a one-line warning.
// Distribution tasks (buildDistribution, jpackageMacOS, prepareAppDir) depend on this so
// release artifacts always include the bundled lib.

val espeakNgVersion = "1.52.0"
val espeakNgSha256 = "bb4338102ff3b49a81423da8a1a158b420124b055b60fa76cfb4b18677130a23"
val espeakNgSourceUrl =
    "https://github.com/espeak-ng/espeak-ng/archive/refs/tags/${espeakNgVersion}.tar.gz"

val espeakNgPlatform: String = run {
    val osArch = System.getProperty("os.arch").lowercase()
    val arch = when {
        osArch == "aarch64" || osArch == "arm64" -> "arm64"
        osArch == "amd64" || osArch == "x86_64" -> "x64"
        else -> osArch
    }
    when {
        OperatingSystem.current().isMacOsX -> "macos-$arch"
        OperatingSystem.current().isLinux -> "linux-$arch"
        OperatingSystem.current().isWindows -> "windows-$arch"
        else -> "unknown-$arch"
    }
}

val espeakNgSourceArchive = file("${buildDir}/espeak-ng-src/espeak-ng-${espeakNgVersion}.tar.gz")
val espeakNgSourceDir = file("${buildDir}/espeak-ng-src/espeak-ng-${espeakNgVersion}")
val espeakNgInstallDir = file("${buildDir}/espeak-ng/${espeakNgPlatform}")

tasks.register("fetchEspeakNgSource") {
    outputs.dir(espeakNgSourceDir)

    doLast {
        if (espeakNgSourceDir.exists() && File(espeakNgSourceDir, "CMakeLists.txt").exists()) {
            return@doLast
        }
        espeakNgSourceArchive.parentFile.mkdirs()
        if (!espeakNgSourceArchive.exists()) {
            println("Downloading eSpeak-ng ${espeakNgVersion} source...")
            ant.withGroovyBuilder {
                "get"("src" to espeakNgSourceUrl, "dest" to espeakNgSourceArchive, "verbose" to true)
            }
        }
        val actualSha = MessageDigest.getInstance("SHA-256")
            .digest(espeakNgSourceArchive.readBytes())
            .joinToString("") { byte -> "%02x".format(byte) }
        if (actualSha != espeakNgSha256) {
            espeakNgSourceArchive.delete()
            throw GradleException(
                "eSpeak-ng tarball SHA-256 mismatch.\n" +
                "  expected: $espeakNgSha256\n" +
                "  actual:   $actualSha"
            )
        }
        espeakNgSourceDir.deleteRecursively()
        exec {
            workingDir = espeakNgSourceArchive.parentFile
            commandLine("tar", "-xzf", espeakNgSourceArchive.absolutePath)
        }
    }
}

tasks.register("buildEspeakNg") {
    dependsOn("fetchEspeakNgSource")

    val installedBinary = if (OperatingSystem.current().isWindows) {
        File(espeakNgInstallDir, "bin/espeak-ng.exe")
    } else {
        File(espeakNgInstallDir, "bin/espeak-ng")
    }
    outputs.dir(espeakNgInstallDir)

    doLast {
        if (installedBinary.exists()) {
            println("eSpeak-ng already built at ${espeakNgInstallDir.absolutePath}, skipping.")
            return@doLast
        }

        val cmakeAvailable = try {
            exec {
                commandLine("cmake", "--version")
                standardOutput = ByteArrayOutputStream()
                errorOutput = ByteArrayOutputStream()
            }
            true
        } catch (_: Exception) {
            false
        }
        if (!cmakeAvailable) {
            throw GradleException(
                "cmake is required to build eSpeak-ng but was not found on PATH.\n" +
                "  macOS:   brew install cmake\n" +
                "  Linux:   apt install cmake  (or your distro's equivalent)\n" +
                "  Windows: install CMake from https://cmake.org/download/"
            )
        }

        espeakNgInstallDir.deleteRecursively()
        espeakNgInstallDir.mkdirs()

        val buildDir = File(espeakNgSourceDir, "build-${espeakNgPlatform}")
        buildDir.deleteRecursively()
        buildDir.mkdirs()

        val cmakeArgs = mutableListOf(
            "cmake",
            "-S", espeakNgSourceDir.absolutePath,
            "-B", buildDir.absolutePath,
            "-DCMAKE_BUILD_TYPE=Release",
            "-DCMAKE_INSTALL_PREFIX=${espeakNgInstallDir.absolutePath}",
            // Shared so we can dlopen libespeak-ng via JNA from PhonemeSynthesizer.
            "-DBUILD_SHARED_LIBS=ON",
            "-DUSE_ASYNC=OFF",
            "-DUSE_MBROLA=OFF",
            "-DUSE_LIBPCAUDIO=OFF",
            "-DUSE_SPEECHPLAYER=ON",
            "-DUSE_KLATT=ON",
            "-DEXTRA_cmn=OFF",
            "-DEXTRA_ru=OFF"
        )
        if (OperatingSystem.current().isMacOsX) {
            val target = if (System.getProperty("os.arch").lowercase().let { it == "aarch64" || it == "arm64" }) {
                "arm64"
            } else {
                "x86_64"
            }
            cmakeArgs += "-DCMAKE_OSX_ARCHITECTURES=$target"
        }

        println("Configuring eSpeak-ng (cmake)...")
        exec { commandLine(cmakeArgs) }

        val cpuCount = Runtime.getRuntime().availableProcessors()
        println("Building eSpeak-ng (cmake --build, $cpuCount jobs)...")
        exec {
            commandLine(
                "cmake", "--build", buildDir.absolutePath,
                "--config", "Release",
                "--parallel", cpuCount.toString()
            )
        }

        println("Installing eSpeak-ng to ${espeakNgInstallDir.absolutePath}...")
        exec {
            commandLine(
                "cmake", "--install", buildDir.absolutePath,
                "--config", "Release"
            )
        }

        // Drop pieces we don't ship (headers, pkgconfig, .a archives).
        File(espeakNgInstallDir, "include").deleteRecursively()
        File(espeakNgInstallDir, "lib/pkgconfig").deleteRecursively()
        File(espeakNgInstallDir, "lib").listFiles()
            ?.filter { it.extension == "a" }
            ?.forEach { it.delete() }

        if (OperatingSystem.current().isMacOsX) {
            // CMake's install step already sets `@rpath/libespeak-ng.1.dylib` as the dylib's
            // install_name and binary's load reference. It also bakes the absolute install
            // prefix as an LC_RPATH on the binary — replace that with @loader_path so the
            // binary works after the install dir is relocated into the app bundle.
            exec {
                commandLine(
                    "install_name_tool", "-add_rpath",
                    "@loader_path/../lib", installedBinary.absolutePath
                )
                isIgnoreExitValue = true  // tolerate already-present
            }
            exec {
                commandLine(
                    "install_name_tool", "-delete_rpath",
                    File(espeakNgInstallDir, "lib").absolutePath,
                    installedBinary.absolutePath
                )
                isIgnoreExitValue = true  // tolerate not-present
            }
        }
        // No equivalent Linux rpath fixup: JNA loads the .so by absolute path, so the
        // binary's inability to find sibling lib/ when run standalone doesn't affect runtime
        // audio. If you want `bin/espeak-ng` to work as a debug aid inside the AppImage,
        // patchelf the binary with `--set-rpath '$ORIGIN/../lib'` or set LD_LIBRARY_PATH.

        if (!installedBinary.exists()) {
            throw GradleException(
                "eSpeak-ng build completed but ${installedBinary.absolutePath} is missing."
            )
        }
        println("eSpeak-ng built successfully.")
    }
}

// AppImage build configuration.

val appImageDir = "${buildDir}/appimage"
val appDirPath = "${appImageDir}/Simbrain.AppDir"

// Determine target architecture for AppImage (defaults to x86_64)
val appImageArch: String = linuxArch ?: "x86_64"

// JRE download URLs for Adoptium Temurin 17
val temurinJreUrls = mapOf(
    "x86_64" to "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jre_x64_linux_hotspot_17.0.13_11.tar.gz",
    "aarch64" to "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jre_aarch64_linux_hotspot_17.0.13_11.tar.gz"
)

// AppImageTool URLs
val appImageToolUrls = mapOf(
    "x86_64" to "https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage",
    "aarch64" to "https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-aarch64.AppImage"
)

/**
 * Downloads the JRE for AppImage bundling
 */
tasks.register("downloadJreForAppImage") {
    val jreUrl = temurinJreUrls[appImageArch] ?: throw GradleException("Unsupported architecture: $appImageArch")
    val jreDownloadDir = file("${buildDir}/jre-download")
    val jreTarball = file("${jreDownloadDir}/jre-${appImageArch}.tar.gz")
    val jreExtractDir = file("${buildDir}/jre-${appImageArch}")

    outputs.dir(jreExtractDir)

    doLast {
        jreDownloadDir.mkdirs()

        if (!jreTarball.exists()) {
            println("Downloading JRE 17 for ${appImageArch}...")
            ant.withGroovyBuilder {
                "get"("src" to jreUrl, "dest" to jreTarball, "verbose" to true)
            }
        }

        println("Extracting JRE...")
        jreExtractDir.deleteRecursively()
        jreExtractDir.mkdirs()

        exec {
            commandLine("tar", "-xzf", jreTarball.absolutePath,
                "-C", jreExtractDir.absolutePath,
                "--strip-components=1")
        }

        println("JRE extracted to: ${jreExtractDir.absolutePath}")
    }
}

/**
 * Downloads appimagetool for the target architecture
 */
tasks.register("downloadAppImageTool") {
    val toolUrl = appImageToolUrls[appImageArch] ?: throw GradleException("Unsupported architecture: $appImageArch")
    val toolsDir = file("${buildDir}/appimage-tools")
    val toolPath = file("${toolsDir}/appimagetool")

    outputs.file(toolPath)

    doLast {
        toolsDir.mkdirs()

        if (!toolPath.exists()) {
            println("Downloading appimagetool for ${appImageArch}...")
            ant.withGroovyBuilder {
                "get"("src" to toolUrl, "dest" to toolPath, "verbose" to true)
            }
            toolPath.setExecutable(true)
        }
    }
}

/**
 * Creates the AppDir structure with all required files
 */
tasks.register("prepareAppDir") {
    dependsOn("shadowJar", "downloadJreForAppImage", "generateBuildInfo", "buildEspeakNg")

    val jreDir = file("${buildDir}/jre-${appImageArch}")
    val appDir = file(appDirPath)

    doLast {
        // Clean previous AppDir
        appDir.deleteRecursively()
        appDir.mkdirs()

        // Create directory structure
        file("${appDir}/usr/lib").mkdirs()
        file("${appDir}/usr/share/applications").mkdirs()
        file("${appDir}/usr/share/icons/hicolor/512x512/apps").mkdirs()
        file("${appDir}/usr/share/licenses/simbrain").mkdirs()
        file("${appDir}/usr/simulations").mkdirs()

        // Copy Simbrain.jar
        copy {
            from("${buildDir}/libs/Simbrain.jar")
            into("${appDir}/usr/lib")
        }

        // Copy bundled eSpeak-ng. AppRun cd's into ${APPDIR}/usr, so placing it at
        // usr/espeak-ng matches the runtime resolver's `${cwd}/espeak-ng` lookup.
        copy {
            from(espeakNgInstallDir)
            into("${appDir}/usr/espeak-ng")
        }
        file("${appDir}/usr/espeak-ng/bin/espeak-ng").setExecutable(true, false)

        // Copy simulations folder
        copy {
            from("simulations")
            into("${appDir}/usr/simulations")
            exclude("**/*.zip")
        }

        // Copy license
        copy {
            from("LICENSE")
            into("${appDir}/usr/share/licenses/simbrain")
        }

        // Copy icon to root (for AppImage)
        copy {
            from("src/main/resources/simbrain_iconset/icon_512x512.png")
            into(appDir)
            rename { "simbrain.png" }
        }

        // Copy icon to hicolor directory
        copy {
            from("src/main/resources/simbrain_iconset/icon_512x512.png")
            into("${appDir}/usr/share/icons/hicolor/512x512/apps")
            rename { "simbrain.png" }
        }

        // Copy desktop file to root (for AppImage)
        copy {
            from("etc/appimage/simbrain.desktop")
            into(appDir)
        }

        // Copy desktop file to applications directory
        copy {
            from("etc/appimage/simbrain.desktop")
            into("${appDir}/usr/share/applications")
        }

        // Copy AppRun script
        copy {
            from("etc/appimage/AppRun")
            into(appDir)
        }
        file("${appDir}/AppRun").setExecutable(true, false)

        // Copy JRE into AppDir
        println("Copying JRE into AppDir...")
        copy {
            from(jreDir)
            into("${appDir}/jre")
        }

        // Ensure java binary is executable
        file("${appDir}/jre/bin/java").setExecutable(true, false)

        println("AppDir prepared at: ${appDir.absolutePath}")
    }
}

/**
 * Creates the AppImage using appimagetool
 */
tasks.register<Exec>("createAppImage") {
    dependsOn("prepareAppDir", "downloadAppImageTool")

    val archSuffix = appImageArch
    val appDir = file(appDirPath)
    val outputDir = file(dist)
    val appImageName = "Simbrain${versionName}${versionSuffixString}-${archSuffix}.AppImage"
    val appImagePath = file("${outputDir}/${appImageName}")
    val toolPath = file("${buildDir}/appimage-tools/appimagetool")

    outputs.file(appImagePath)

    doFirst {
        outputDir.mkdirs()

        // Set ARCH environment variable for appimagetool
        environment("ARCH", archSuffix)
    }

    // Use --appimage-extract-and-run for CI environments without FUSE
    commandLine(
        toolPath.absolutePath,
        "--appimage-extract-and-run",
        appDir.absolutePath,
        appImagePath.absolutePath
    )

    doLast {
        println("AppImage created: ${appImagePath.absolutePath}")
        println("Size: ${appImagePath.length() / 1024 / 1024} MB")
    }
}
