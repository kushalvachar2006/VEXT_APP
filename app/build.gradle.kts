import java.util.Properties

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidApplication)
    id("com.google.gms.google-services")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.chat_application"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.chat_application"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        multiDexEnabled = true

        resValue("string", "web_client_id", "\"${localProperties.getProperty("web_client_id")}\"")
        resValue("string", "API_KEY_MAINFEST", "\"${localProperties.getProperty("API_KEY_MAINFEST")}\"")
        resValue("string", "RENDER_LINK", "\"${localProperties.getProperty("RENDER_LINK")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/INDEX.LIST"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.credentials:credentials:1.2.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.0")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation ("com.github.yalantis:ucrop:2.2.8")//croping tool
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("com.google.firebase:firebase-messaging:23.4.0")
    implementation ("com.google.auth:google-auth-library-oauth2-http:1.19.0")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("com.infobip:google-webrtc:1.0.0035529")
    implementation("androidx.core:core:1.12.0") // For call-style
    implementation ("com.github.chrisbanes:PhotoView:2.3.0")//For PhotoView

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.androidx.core.splashscreen)
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-database")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}