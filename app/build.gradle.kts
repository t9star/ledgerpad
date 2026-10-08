import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing is read from keystore.properties (git-ignored) when present.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// Google's official AdMob TEST ids. Replace the release values with real ids from the AdMob console.
val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val testBanner = "ca-app-pub-3940256099942544/9214589741"
val testInterstitial = "ca-app-pub-3940256099942544/1033173712"
val testNative = "ca-app-pub-3940256099942544/2247696110"

android {
    namespace = "jp.tpp.t9s.ledgerpad"
    compileSdk = 36

    defaultConfig {
        applicationId = "jp.tpp.t9s.ledgerpad"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = testAdmobAppId
            buildConfigField("String", "AD_BANNER_ID", "\"$testBanner\"")
            buildConfigField("String", "AD_INTERSTITIAL_ID", "\"$testInterstitial\"")
            buildConfigField("String", "AD_NATIVE_ID", "\"$testNative\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
            // TODO: replace with real AdMob ids before publishing
            manifestPlaceholders["admobAppId"] = keystoreProps.getProperty("admobAppId", testAdmobAppId)
            buildConfigField("String", "AD_BANNER_ID", "\"${keystoreProps.getProperty("adBannerId", testBanner)}\"")
            buildConfigField("String", "AD_INTERSTITIAL_ID", "\"${keystoreProps.getProperty("adInterstitialId", testInterstitial)}\"")
            buildConfigField("String", "AD_NATIVE_ID", "\"${keystoreProps.getProperty("adNativeId", testNative)}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        aidl = false
        shaders = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Explicit versions override old transitive versions pulled in by other SDKs
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.androidx.biometric)
    implementation(libs.kotlinx.serialization.json)

    // Monetization
    implementation(libs.billing.ktx)
    implementation(libs.play.services.ads)
    implementation(libs.ump)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
