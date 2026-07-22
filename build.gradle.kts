import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    jacoco
}

subprojects {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    pluginManager.apply("dev.detekt")

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        pluginManager.apply("jacoco")
    }

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        parallel = true
        source.setFrom(
            files(
                "src/main/java",
                "src/main/kotlin",
                "src/test/java",
                "src/test/kotlin",
                "src/androidTest/java",
                "src/androidTest/kotlin",
            ),
        )
    }
}

tasks.register<JacocoReport>("coverageReport") {
    group = "verification"
    description = "Runs all unit and connected UI tests and creates a combined coverage report."

    dependsOn(
        ":app:testDebugUnitTest",
        ":app:connectedDebugAndroidTest",
        ":data:test",
        ":domain:test",
        ":ui:testDebugUnitTest",
        ":ui:connectedDebugAndroidTest",
    )

    executionData(
        fileTree(rootDir) {
            include("**/build/jacoco/test.exec")
            include("**/build/outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
            include("**/build/outputs/code_coverage/debugAndroidTest/connected/**/*.ec")
        },
    )
    sourceDirectories.setFrom(
        files(
            subprojects.map { it.layout.projectDirectory.dir("src/main/kotlin") },
        ),
    )
    classDirectories.setFrom(
        files(
            project(":app").layout.buildDirectory.dir(
                "intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes",
            ),
            project(":ui").layout.buildDirectory.dir(
                "intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes",
            ),
            project(":data").layout.buildDirectory.dir("classes/kotlin/main"),
            project(":domain").layout.buildDirectory.dir("classes/kotlin/main"),
        ).asFileTree.matching {
            exclude(
                "**/R.class",
                "**/R$*.class",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                "**/*ComposableSingletons*.*",
            )
        },
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
