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
            packageName = "BlacklightInstaller"
            packageVersion = "1.1.0"
        }
    }
}

tasks.register<Exec>("createExeWithAdminPrompt") {
    dependsOn("createDistributable")

    commandLine(
        "mt.exe",
        "-manifest", "src/jvmMain/resources/elevate.manifest",
        "-outputresource:build/compose/binaries/main/app/BlacklightInstaller/BlacklightInstaller.exe;#1"
    )
}