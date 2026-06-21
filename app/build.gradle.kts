plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.traintracker"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.traintracker"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    // For Room RxJava2 support (optional)
    // implementation(libs.room.rxjava2)
    // For Room RxJava3 support (optional)
    // implementation(libs.room.rxjava3)
    // For Room Guava support (optional)
    // implementation(libs.room.guava)
    // Test helpers
    testImplementation(libs.room.testing)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}