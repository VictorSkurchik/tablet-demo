import org.gradle.api.Action
import org.gradle.api.Task
import org.w3c.dom.Element
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

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

    testOptions {
        emulatorControl {
            enable = true
        }
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
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.adaptive)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.profileinstaller)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    implementation(platform(libs.kotlinx.coroutines.bom))
    implementation(libs.kotlinx.coroutines.core)

    debugImplementation(libs.leakcanary.android)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.device)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.window.testing)

    testImplementation(libs.androidx.lifecycle.viewmodel.savedstate)
    testImplementation(libs.junit)
    testImplementation(libs.koin.test)
}

val mergedReleaseManifest =
    layout.buildDirectory.file(
        "intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml",
    )
val dataExtractionRules = layout.projectDirectory.file("src/main/res/xml/data_extraction_rules.xml")

tasks.register("verifyReleaseArtifacts") {
    group = "verification"
    description =
        "Verifies release security, R8 mapping, startup optimization, artifacts, and profiles."
    dependsOn("assembleRelease", "bundleRelease")
    inputs
        .files(
            layout.buildDirectory.file("outputs/apk/release/app-release-unsigned.apk"),
            layout.buildDirectory.file("outputs/bundle/release/app-release.aab"),
            layout.buildDirectory.file("outputs/mapping/release/mapping.txt"),
            layout.buildDirectory.file(
                "intermediates/r8_metadata/release/minifyReleaseWithR8/r8-metadata.dat",
            ),
            mergedReleaseManifest,
            dataExtractionRules,
        ).withPropertyName("releaseArtifacts")

    doLast(
        Action<Task> {
            val artifacts = inputs.files.files
            val releaseApk = checkNotNull(artifacts.singleOrNull { it.extension == "apk" })
            val releaseBundle = checkNotNull(artifacts.singleOrNull { it.extension == "aab" })
            val mapping = checkNotNull(artifacts.singleOrNull { it.name == "mapping.txt" })
            val r8Metadata = checkNotNull(artifacts.singleOrNull { it.name == "r8-metadata.dat" })

            check(releaseApk.isFile && releaseApk.length() > 0L) {
                "Release APK was not produced: $releaseApk"
            }
            check(releaseBundle.isFile && releaseBundle.length() > 0L) {
                "Release AAB was not produced: $releaseBundle"
            }
            check(mapping.isFile && mapping.length() > 0L) {
                "R8 mapping was not produced: $mapping"
            }
            check(r8Metadata.isFile && r8Metadata.length() > 0L) {
                "R8 metadata was not produced: $r8Metadata"
            }

            val r8MetadataContent = r8Metadata.readText()
            val firstDexStart = r8MetadataContent.indexOf("\"dexFiles\":[")
            val firstDexMetadata =
                if (firstDexStart >= 0) {
                    val firstDexEnd = r8MetadataContent.indexOf('}', startIndex = firstDexStart)
                    r8MetadataContent.substring(
                        startIndex = firstDexStart,
                        endIndex = firstDexEnd.coerceAtLeast(firstDexStart),
                    )
                } else {
                    ""
                }
            check("\"startup\":true" in firstDexMetadata) {
                "R8 did not mark the primary classes.dex as startup-optimized"
            }
            check(
                "\"isDexLayoutOptimizationEnabled\":true" in r8MetadataContent &&
                    "\"isProfileGuidedOptimizationEnabled\":true" in r8MetadataContent,
            ) {
                "R8 startup profile optimizations are not enabled"
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
        },
    )

    doLast(
        Action<Task> {
            val androidXmlNamespace = "http://schemas.android.com/apk/res/android"
            val manifestFile =
                checkNotNull(
                    inputs.files.files.singleOrNull { it.name == "AndroidManifest.xml" },
                )
            check(manifestFile.isFile) {
                "Merged release manifest was not produced: $manifestFile"
            }
            val manifest =
                DocumentBuilderFactory
                    .newInstance()
                    .apply {
                        isNamespaceAware = true
                        isXIncludeAware = false
                        setExpandEntityReferences(false)
                        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                    }.newDocumentBuilder()
                    .parse(manifestFile)
            val packageName = manifest.documentElement.getAttribute("package")
            val applicationNodes = manifest.getElementsByTagName("application")
            check(applicationNodes.length == 1) {
                "Release manifest must contain exactly one application element"
            }
            val application = applicationNodes.item(0) as Element
            check(application.getAttributeNS(androidXmlNamespace, "allowBackup") == "false") {
                "Release must disable Android backup"
            }
            check(application.getAttributeNS(androidXmlNamespace, "fullBackupContent") == "false") {
                "Release must disable legacy full backup"
            }
            check(
                application.getAttributeNS(androidXmlNamespace, "dataExtractionRules") ==
                    "@xml/data_extraction_rules",
            ) {
                "Release must use the verified Android 12+ data extraction rules"
            }
            check(application.getAttributeNS(androidXmlNamespace, "usesCleartextTraffic") == "false") {
                "Release must explicitly disable cleartext traffic"
            }
            check(application.getAttributeNS(androidXmlNamespace, "debuggable") != "true") {
                "Release application must not be debuggable"
            }
            check(application.getAttributeNS(androidXmlNamespace, "testOnly") != "true") {
                "Release application must not be test-only"
            }
            val profileableNodes = manifest.getElementsByTagName("profileable")
            var hasShellProfileable = false
            for (index in 0 until profileableNodes.length) {
                val profileable = profileableNodes.item(index) as Element
                hasShellProfileable =
                    hasShellProfileable ||
                    profileable.getAttributeNS(androidXmlNamespace, "shell") == "true"
            }
            check(!hasShellProfileable) {
                "Release application must not be profileable by shell"
            }

            val requestedPermissions = mutableSetOf<String>()
            val permissionTags =
                listOf(
                    "uses-permission",
                    "uses-permission-sdk-23",
                    "uses-permission-sdk-m",
                )
            for (permissionTag in permissionTags) {
                val permissionNodes = manifest.getElementsByTagName(permissionTag)
                for (index in 0 until permissionNodes.length) {
                    requestedPermissions +=
                        (permissionNodes.item(index) as Element)
                            .getAttributeNS(androidXmlNamespace, "name")
                }
            }
            val dynamicReceiverPermission =
                "$packageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
            check(
                requestedPermissions == setOf(dynamicReceiverPermission),
            ) {
                "Unexpected release permissions: $requestedPermissions"
            }

            val declaredPermissionNodes = manifest.getElementsByTagName("permission")
            check(declaredPermissionNodes.length == 1) {
                "Release must declare exactly one application-specific permission"
            }
            val declaredPermission = declaredPermissionNodes.item(0) as Element
            check(
                declaredPermission.getAttributeNS(androidXmlNamespace, "name") ==
                    dynamicReceiverPermission &&
                    declaredPermission.getAttributeNS(androidXmlNamespace, "protectionLevel") ==
                    "signature",
            ) {
                "Dynamic receiver permission must be application-specific and signature-protected"
            }

            val unapprovedExportedComponents = mutableListOf<String>()
            val componentTypes = listOf("activity", "activity-alias", "provider", "receiver", "service")
            for (componentType in componentTypes) {
                val componentNodes = manifest.getElementsByTagName(componentType)
                for (index in 0 until componentNodes.length) {
                    val component = componentNodes.item(index) as Element
                    if (component.getAttributeNS(androidXmlNamespace, "exported") != "true") continue
                    val name = component.getAttributeNS(androidXmlNamespace, "name")
                    val isLauncher =
                        componentType == "activity" &&
                            name == "$packageName.MainActivity"
                    val isDumpProtected =
                        component.getAttributeNS(androidXmlNamespace, "permission") ==
                            "android.permission.DUMP"
                    if (!isLauncher && !isDumpProtected) {
                        unapprovedExportedComponents += name
                    }
                }
            }
            check(unapprovedExportedComponents.isEmpty()) {
                "Unapproved exported release components: $unapprovedExportedComponents"
            }
        },
    )

    doLast(
        Action<Task> {
            val deniedBackupDomains =
                setOf(
                    "database",
                    "device_database",
                    "device_file",
                    "device_root",
                    "device_sharedpref",
                    "external",
                    "file",
                    "root",
                    "sharedpref",
                )
            val extractionRulesFile =
                checkNotNull(
                    inputs.files.files.singleOrNull { it.name == "data_extraction_rules.xml" },
                )
            val extractionRules =
                DocumentBuilderFactory
                    .newInstance()
                    .apply {
                        isNamespaceAware = true
                        isXIncludeAware = false
                        setExpandEntityReferences(false)
                        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                    }.newDocumentBuilder()
                    .parse(extractionRulesFile)
            for (sectionName in listOf("cloud-backup", "device-transfer")) {
                val sectionNodes = extractionRules.getElementsByTagName(sectionName)
                check(sectionNodes.length == 1) {
                    "Data extraction rules must contain exactly one $sectionName element"
                }
                val section = sectionNodes.item(0) as Element
                val exclusionNodes = section.getElementsByTagName("exclude")
                val excludedDomains = mutableSetOf<String>()
                var everyPathIsRoot = true
                for (index in 0 until exclusionNodes.length) {
                    val exclusion = exclusionNodes.item(index) as Element
                    excludedDomains += exclusion.getAttribute("domain")
                    everyPathIsRoot = everyPathIsRoot && exclusion.getAttribute("path") == "."
                }
                check(
                    exclusionNodes.length == deniedBackupDomains.size &&
                        excludedDomains == deniedBackupDomains &&
                        everyPathIsRoot,
                ) {
                    "$sectionName must deny every Android backup domain"
                }
            }
        },
    )
}
