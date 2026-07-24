import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
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

val coverageTaskDependencies =
    listOf(
        ":app:testDebugUnitTest",
        ":app:connectedDebugAndroidTest",
        ":data:test",
        ":domain:test",
        ":recovery:test",
        ":ui:testDebugUnitTest",
        ":ui:connectedDebugAndroidTest",
    )

project(":app").tasks.configureEach {
    if (name == "connectedDebugAndroidTest") {
        mustRunAfter(":ui:connectedDebugAndroidTest")
    }
}

val coverageExecutionData =
    files(
        project(":app").layout.buildDirectory.file(
            "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
        ),
        project(":ui").layout.buildDirectory.file(
            "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
        ),
        project(":data").layout.buildDirectory.file("jacoco/test.exec"),
        project(":domain").layout.buildDirectory.file("jacoco/test.exec"),
        project(":app").fileTree(
            project(":app").layout.buildDirectory.dir(
                "outputs/code_coverage/debugAndroidTest/connected",
            ),
        ) {
            include("**/*.ec")
        },
        project(":ui").fileTree(
            project(":ui").layout.buildDirectory.dir(
                "outputs/code_coverage/debugAndroidTest/connected",
            ),
        ) {
            include("**/*.ec")
        },
    )
val coverageSourceDirectories =
    files(
        subprojects.flatMap {
            listOf(
                it.layout.projectDirectory.dir("src/main/java"),
                it.layout.projectDirectory.dir("src/main/kotlin"),
            )
        },
    )
val coverageClassDirectories =
    files(
        project(":app").layout.buildDirectory.dir(
            "intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes",
        ),
        project(":ui").layout.buildDirectory.dir(
            "intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes",
        ),
        project(":data").layout.buildDirectory.dir("classes/kotlin/main"),
        project(":domain").layout.buildDirectory.dir("classes/kotlin/main"),
        project(":recovery").layout.buildDirectory.dir("classes/kotlin/main"),
    ).asFileTree.matching {
        exclude(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*ComposableSingletons*.*",
        )
    }

tasks.register<JacocoReport>("coverageReport") {
    group = "verification"
    description = "Runs all unit and connected UI tests and creates a combined coverage report."

    dependsOn(coverageTaskDependencies)
    executionData(coverageExecutionData)
    sourceDirectories.setFrom(coverageSourceDirectories)
    classDirectories.setFrom(coverageClassDirectories)

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.register<JacocoCoverageVerification>("coverageVerification") {
    group = "verification"
    description = "Fails when combined JVM and device-test coverage drops below the accepted floor."

    dependsOn("coverageReport")
    executionData(coverageExecutionData)
    sourceDirectories.setFrom(coverageSourceDirectories)
    classDirectories.setFrom(coverageClassDirectories)

    violationRules {
        rule {
            limit {
                counter = "INSTRUCTION"
                minimum = "0.87".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                minimum = "0.73".toBigDecimal()
            }
            limit {
                counter = "LINE"
                minimum = "0.88".toBigDecimal()
            }
            limit {
                counter = "METHOD"
                minimum = "0.84".toBigDecimal()
            }
            limit {
                counter = "CLASS"
                minimum = "0.96".toBigDecimal()
            }
        }
    }
}
