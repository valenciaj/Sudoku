import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "com.valenciaj.sudoku"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.valenciaj.sudoku"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"

        // Client ID de la OAuth App de GitHub (es público, no es un secreto).
        // Se pone en gradle.properties (githubClientId=...) o en la variable SUDOKU_GITHUB_CLIENT_ID.
        val githubClientId = (findProperty("githubClientId") as String?)?.takeIf { it.isNotBlank() }
            ?: System.getenv("SUDOKU_GITHUB_CLIENT_ID")
            ?: ""
        buildConfigField("String", "GITHUB_CLIENT_ID", "\"$githubClientId\"")
    }

    // Firma de release: desde keystore.properties (local, no se sube a git)
    // o desde variables de entorno (GitHub Actions). Sin ellas, el release sale sin firmar.
    val keystoreProps = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    fun signingValue(prop: String, env: String): String? =
        keystoreProps.getProperty(prop) ?: System.getenv(env)
    val storeFilePath = signingValue("storeFile", "SUDOKU_KEYSTORE_FILE")

    signingConfigs {
        if (storeFilePath != null) {
            create("release") {
                storeFile = rootProject.file(storeFilePath)
                storePassword = signingValue("storePassword", "SUDOKU_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "SUDOKU_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "SUDOKU_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
