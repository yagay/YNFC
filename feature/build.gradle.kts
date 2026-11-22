plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

val configAuthority = providers.gradleProperty("ynfcConfigAuthority").orNull
    ?: "com.yagay.YNFC.config"
val standaloneVersionCode = 58
val runtimeVersionCode = providers.gradleProperty("ySuiteHostVersionCode")
    .orNull?.toIntOrNull() ?: standaloneVersionCode

android {
    namespace = "com.yagay.YNFC"
    compileSdk = 37

    defaultConfig {
        minSdk = 31
        buildConfigField("int", "VERSION_CODE", runtimeVersionCode.toString())
        buildConfigField("String", "VERSION_NAME", "\"1.0.57\"")
        buildConfigField("int", "HOOK_BUILD", "40")
        buildConfigField("String", "CONFIG_AUTHORITY", "\"$configAuthority\"")
        manifestPlaceholders["ynfcConfigAuthority"] = configAuthority
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("com.github.yagay.YSuite:ui") {
        version { branch = "main" }
    }
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("com.google.code.gson:gson:2.11.0")
    compileOnly("io.github.libxposed:api:102.0.0")
    testImplementation("junit:junit:4.13.2")
}
