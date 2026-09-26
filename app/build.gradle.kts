plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val appVersionName = "1.0"

// CI passes -PcanaryRunNumber=<run_number> so the app can tell whether the
// published canary is newer than itself. Local builds fall back to 0, which
// compares as "older than everything" and simply offers an update.
val canaryRunNumber = (project.findProperty("canaryRunNumber") as String?)?.toIntOrNull() ?: 0

android {
    namespace = "com.geno1024.pictureframe"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.geno1024.pictureframe"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = appVersionName
        buildConfigField("int", "CANARY_RUN_NUMBER", "$canaryRunNumber")
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
        buildConfig = true
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
