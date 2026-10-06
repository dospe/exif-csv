import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// GitHub Actions passes its run number so every build gets a new, increasing versionCode.
val buildNumber: Int = System.getenv("APP_BUILD_NUMBER")?.toIntOrNull() ?: 0

android {
    namespace = "io.github.dospe.exifcsv"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.dospe.exifcsv"
        minSdk = 26
        targetSdk = 36
        versionCode = if (buildNumber > 0) buildNumber else 1
        versionName = "1.0.$buildNumber"
    }

    signingConfigs {
        create("release") {
            // The signing key is intentionally committed to the repository (see README): this app is
            // distributed through GitHub Releases, not through Google Play. Override via environment
            // variables if you fork the project and want your own key.
            storeFile = rootProject.file(System.getenv("APP_KEYSTORE_FILE") ?: "keystore/release.jks")
            storePassword = System.getenv("APP_KEYSTORE_PASSWORD") ?: "exifcsv-public"
            keyAlias = System.getenv("APP_KEY_ALIAS") ?: "exifcsv"
            keyPassword = System.getenv("APP_KEY_PASSWORD") ?: "exifcsv-public"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.exifinterface)

    testImplementation(libs.junit)
}
