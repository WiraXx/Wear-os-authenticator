import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The Wearable Data Layer only connects apps with the same applicationId and signing key.
// Default pairs with the `play` phone flavor; pass -Pwear.applicationIdSuffix=.alpha (or .dev)
// to pair with the `alpha` (or `dev`) phone flavor.
val phoneApplicationIdSuffix: String = providers.gradleProperty("wear.applicationIdSuffix").getOrElse("")
val runNumber: Int = providers.environmentVariable("GITHUB_RUN_NUMBER").map(String::toInt).getOrElse(0)

android {
    namespace = "proton.android.authenticator.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "proton.android.authenticator$phoneApplicationIdSuffix"
        minSdk = 30
        targetSdk = 36
        versionCode = 10_310_000 + runNumber
        versionName = "1.3.10"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.play.services.wearable)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.material)

    implementation(projects.shared.wearsync)
}
