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

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
