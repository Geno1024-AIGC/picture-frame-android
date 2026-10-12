import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// CI passes -PcanaryRunNumber=<run_number> and -PcanaryCommit=<sha>.
//
// versionCode is the run number so the platform itself orders canaries and the
// updater can compare against the installed package rather than a constant
// baked into the APK. versionName pairs it with the short commit so a build is
// identifiable by eye, e.g. "128-a1b2c3d4".
//
// A local build defaults to 1, which AGP requires to be positive, and so always
// looks older than a canary. It also cannot be installed over a canary because
// Android refuses a downgrade: either uninstall the canary first, or build with
// -PcanaryRunNumber=999999 to sit above whatever CI has reached.
val canaryRunNumber = (project.findProperty("canaryRunNumber") as String?)?.toIntOrNull() ?: 1
val canaryCommit = ((project.findProperty("canaryCommit") as String?) ?: "local").take(8)
val appVersionName = "$canaryRunNumber-$canaryCommit"

// Release signing, shared across the geno1024-aigc org. The rules live in
// RULES/KEYSTORE.md and the reading order here follows them: the environment
// first, which is how CI injects it, then the git-ignored local.properties.
// Nothing is hardcoded and nothing is committed.
//
// Every project signed by this key shares one identity, which is what lets the
// updater install a new canary over the old one. A missing keystore leaves the
// release build unsigned instead of failing configuration, so a local
// assembleRelease still works; the workflow refuses to publish an unsigned APK.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun keystoreSecret(property: String, env: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(property)?.takeIf { it.isNotBlank() }

val releaseStoreFile = keystoreSecret("keystore.storeFile", "KEYSTORE_STORE_FILE")
val releaseStorePassword = keystoreSecret("keystore.storePassword", "KEYSTORE_STORE_PASSWORD")
val releaseKeyPassword =
    keystoreSecret("keystore.keyPassword", "KEYSTORE_KEY_PASSWORD") ?: releaseStorePassword
val releaseKeyAlias =
    keystoreSecret("keystore.keyAlias", "KEYSTORE_KEY_ALIAS") ?: "geno"

android {
    namespace = "com.geno1024.pictureframe"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.geno1024.pictureframe"
        minSdk = 24
        targetSdk = 37
        versionCode = canaryRunNumber
        versionName = appVersionName
    }

    signingConfigs {
        val storeFileValue = releaseStoreFile
        val storePasswordValue = releaseStorePassword
        if (storeFileValue != null && storePasswordValue != null) {
            create("release") {
                storeFile = File(storeFileValue)
                storePassword = storePasswordValue
                keyPassword = releaseKeyPassword
                keyAlias = releaseKeyAlias
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Left unset when there is no keystore, which yields an unsigned APK
            // rather than a configuration failure.
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(25)
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}

tasks.register("printVersionName") {
    val version = appVersionName
    doLast { println(version) }
}
