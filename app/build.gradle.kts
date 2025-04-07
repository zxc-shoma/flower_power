plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")

    id("com.google.gms.google-services")
    id("kotlin-kapt")

    kotlin("plugin.serialization") version "1.9.22"
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")


}

android {
    namespace = "com.example.flowerpower"
    compileSdk = 34



    defaultConfig {
        applicationId = "com.example.flowerpower"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }
    buildFeatures {
        viewBinding {
            enable = true
        }

    }
    repositories {
        google() // Репозиторий Google Maven
        mavenCentral() // Репозиторий Maven Central
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {


    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")



    implementation("androidx.compose.material3:material3:1.1.2")
    implementation ("androidx.cardview:cardview:1.0.0")

    implementation ("com.google.android.gms:play-services-auth:19.2.0")




    implementation(platform("com.google.firebase:firebase-bom:32.0.0"))

    implementation ("com.google.firebase:firebase-analytics")
    implementation ("com.google.firebase:firebase-firestore")
    implementation ("com.google.firebase:firebase-auth")
    implementation ("com.google.firebase:firebase-database")



    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation ("com.google.code.gson:gson:2.8.8")

    implementation ("androidx.navigation:navigation-fragment-ktx:2.7.0")
    implementation ("androidx.navigation:navigation-ui-ktx:2.7.0")
    implementation ("androidx.fragment:fragment:1.5.7")
    implementation ("com.google.android.gms:play-services-maps:18.1.0")




    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation ("com.github.bumptech.glide:annotations:4.16.0")
    kapt ("com.github.bumptech.glide:compiler:4.16.0")









    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")











}