/*
 * Copyright (C) 2026 Rootify - Aby - FoxLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// ---- CORE IMPORTS & LIBRARIES ----
// Standard library dependencies for dynamic versioning and build orchestration.
import java.util.Properties
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

// ---- APPLICATION VERSIONING CONFIGURATION ----
// Logic for dynamic version code and version name generation based on build context.
fun getAppVersionConfig(): Map<String, Any> {
    val versionFile = file("version.properties")
    val props = Properties()
    
    // --- File initialization
    if (!versionFile.exists()) {
        versionFile.createNewFile()
    }
    
    // --- Property persistence loading
    FileInputStream(versionFile).use { props.load(it) }

    // --- Build context determination
    // Prioritizes explicit -Pctx=[context] followed by shorthand flags (-Palpha, -Pbeta, etc.).
    val buildContext = when {
        project.hasProperty("ctx") -> project.property("ctx").toString()
        project.hasProperty("alpha") -> "alpha"
        project.hasProperty("beta") -> "beta"
        project.hasProperty("rc") -> "rc"
        project.hasProperty("stable") -> "stable"
        else -> "stable"
    }
    
    // --- Versioning group mapping
    val groupKey = buildContext
    
    // --- Smart Version Increment Logic
    // Increments version code strictly for Release builds to prevent development-time bloat.
    val isReleaseBuild = project.gradle.startParameter.taskNames.any { 
        (it.contains("assemble") || it.contains("bundle")) && it.contains("Release") 
    }
    
    val currentCount = if (isReleaseBuild) {
        val next = (props["${groupKey}_count"]?.toString() ?: "0").toInt() + 1
        props["${groupKey}_count"] = next.toString()
        
        // --- Save properties with copyright header
        FileOutputStream(versionFile).use { out ->
            out.write(("""#
# Copyright (C) 2026 Rootify - Aby - FoxLabs
# Licensed under the Apache License, Version 2.0
#
""").trimIndent().toByteArray())
            props.store(out, null)
        }
        next
    } else {
        (props["${groupKey}_count"]?.toString() ?: "0").toInt()
    }

    // --- Semantic version construction
    val baseVersion = when (buildContext) {
        "alpha", "beta" -> "0.9.$currentCount"
        "rc" -> "0.9.9.$currentCount"
        else -> "1.0.$currentCount"
    }

    // --- Metadata generation
    val timestamp = SimpleDateFormat("yyMMdd").format(Date())
    
    return mapOf(
        "code" to (timestamp.toLong() * 100 + currentCount).toInt(),
        "name" to "$baseVersion-$buildContext",
        "label" to baseVersion,
        "context" to buildContext,
        "build" to currentCount
    )
}

// Global configuration initialization
val appConfig = getAppVersionConfig()

// ---- PLUGIN CONFIGURATION ----
// Android and Flutter integration plugin definitions.
plugins {
    id("com.android.application")
    id("kotlin-android")
    id("dev.flutter.flutter-gradle-plugin")
}

// ---- ANDROID PROJECT SETTINGS ----
// Core SDK targets, compilation options, and build variant configurations.
android {
    namespace = "com.aby.rootify"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    // --- Java & Kotlin Language Compatibility
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // --- Default Application Configuration
    defaultConfig {
        applicationId = "com.aby.rootify"
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        
        // Dynamic version assignment from getAppVersionConfig
        versionCode = appConfig["code"] as Int
        versionName = appConfig["name"] as String
    }

    // --- Build Variant Management
    buildTypes {
        getByName("debug") {
            // Standard development mode (JIT)
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            // Production deployment mode (AOT)
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            isShrinkResources = false
            isMinifyEnabled = false
        }
    }
}

// ---- POST-BUILD AUTOMATION PIPELINE ----
// Automated deployment logic executed after successful compilation.
afterEvaluate {
    tasks.named("assembleRelease") {
        doLast {
            val outputDir = file("$buildDir/outputs/flutter-apk")
            val homeDir = System.getProperty("user.home")
            val date = SimpleDateFormat("yyyyMMdd").format(Date())
            val projectName = "rootify"
            val label = appConfig["label"]
            val ctx = appConfig["context"].toString()
            val bNumber = appConfig["build"]

            // --- Destination Directory Mapping
            val destDirName = ctx.capitalize()
            val destDir = File("$homeDir/Apps/$destDirName")
            
            if (!destDir.exists()) {
                destDir.mkdirs()
            }
            
            // --- ABI-Specific Deployment (ARM Optimized)
            if (outputDir.exists()) {
                outputDir.listFiles()?.forEach { file ->
                    // Process only relevant release APKs
                    if (file.name.startsWith("app-") && file.name.endsWith("-release.apk")) {
                        
                        // --- Architecture Filtering (Exclude x86/x86_64)
                        val abi = when {
                            file.name.contains("arm64-v8a") -> "arm64-v8a"
                            file.name.contains("armeabi-v7a") -> "armeabi-v7a"
                            else -> null // Skip non-ARM architectures
                        }
                        
                        if (abi != null) {
                            val newName = "$projectName-$abi-$label-$ctx-$date-b$bNumber.apk"
                            val destFile = File(destDir, newName)
                            
                            // Deploy file to targeted Apps directory
                            file.copyTo(destFile, overwrite = true)
                            println("Success: $newName -> ~/Apps/$destDirName/")
                        }
                    }
                }
            }
        }
    }
}

// ---- FLUTTER CORE INTEGRATION ----
flutter {
    source = "../.."
}
