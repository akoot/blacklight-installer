import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    id("com.gradleup.shadow") version "9.2.2"
}
val filekitVersion = "0.12.0"

kotlin {
    jvm()
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(compose.materialIconsExtended)
            implementation("com.squareup.okhttp3:okhttp:5.3.0")
            implementation("io.github.vinceglb:filekit-core:${filekitVersion}")
            implementation("io.github.vinceglb:filekit-dialogs:${filekitVersion}")
            implementation("io.github.vinceglb:filekit-dialogs-compose:${filekitVersion}")
            implementation("io.github.vinceglb:filekit-coil:${filekitVersion}")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}


compose.desktop {
    application {
        mainClass = "me.akoot.bldl.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "me.akoot.bldl"
            packageVersion = "1.0.0"
        }
    }
}

// Configure ShadowJar to create a portable JAR
tasks.shadowJar {
    archiveBaseName.set("BlacklightInstaller")
    archiveClassifier.set("") // removes "-all" suffix
    archiveVersion.set("") // no version number in name
    manifest {
        attributes["Main-Class"] = "me.akoot.bldl.MainKt"
    }
}

// Optional shortcut task
tasks.register("portableJar") {
    dependsOn("shadowJar")
    group = "build"
    description = "Builds a portable, runnable JAR file."
}