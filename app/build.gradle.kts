import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "by.vsdev.tablet.demo"
    compileSdk {
        version =
            release(
                libs.versions.compileSdk
                    .get()
                    .toInt(),
            ) {
                minorApiLevel =
                    libs.versions.compileSdkMinor
                        .get()
                        .toInt()
            }
    }

    defaultConfig {
        applicationId = "by.vsdev.tablet.demo"
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 1
        versionName = "0.0.1"
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
            versionNameSuffix = "-debug"
        }
        release {
            versionNameSuffix = "-release"
            isMinifyEnabled = true
            isShrinkResources = true
        }
        create("benchmark") {
            initWith(getByName("release"))
            versionNameSuffix = "-benchmark"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    baselineProfile(project(":benchmark"))

    implementation(project(":ui"))
    implementation(project(":domain"))
    implementation(project(":data"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.profileinstaller)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    debugImplementation(libs.leakcanary.android)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.ui.test.junit4)

    testImplementation(libs.androidx.lifecycle.viewmodel.savedstate)
    testImplementation(libs.junit)
    testImplementation(libs.koin.test)
}

tasks.register("verifyReleaseArtifacts") {
    group = "verification"
    description = "Builds release artifacts and verifies R8 mapping and packaged Baseline Profiles."
    dependsOn("assembleRelease", "bundleRelease")
    inputs
        .files(
            layout.buildDirectory.file("outputs/apk/release/app-release-unsigned.apk"),
            layout.buildDirectory.file("outputs/bundle/release/app-release.aab"),
            layout.buildDirectory.file("outputs/mapping/release/mapping.txt"),
        ).withPropertyName("releaseArtifacts")

    doLast {
        val artifacts = inputs.files.files
        val releaseApk = checkNotNull(artifacts.singleOrNull { it.extension == "apk" })
        val releaseBundle = checkNotNull(artifacts.singleOrNull { it.extension == "aab" })
        val mapping = checkNotNull(artifacts.singleOrNull { it.name == "mapping.txt" })

        check(releaseApk.isFile && releaseApk.length() > 0L) {
            "Release APK was not produced: $releaseApk"
        }
        check(releaseBundle.isFile && releaseBundle.length() > 0L) {
            "Release AAB was not produced: $releaseBundle"
        }
        check(mapping.isFile && mapping.length() > 0L) {
            "R8 mapping was not produced: $mapping"
        }
        check(
            ZipFile(releaseApk).use {
                it.getEntry("assets/dexopt/baseline.prof") != null
            },
        ) {
            "Release APK does not contain assets/dexopt/baseline.prof"
        }
        check(
            ZipFile(releaseBundle).use {
                it.getEntry(
                    "BUNDLE-METADATA/com.android.tools.build.profiles/baseline.prof",
                ) != null
            },
        ) {
            "Release AAB does not contain a Baseline Profile"
        }
    }
}
