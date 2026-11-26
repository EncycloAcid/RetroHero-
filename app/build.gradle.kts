plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun computeVersionFromTag(raw: String): Pair<Int, String>? {
    val clean = raw.removePrefix("v").trim()
    val match = Regex("""^(\d+)\.(\d+)\.(\d+)(?:-(.+))?$""").matchEntire(clean) ?: return null
    val major = match.groupValues[1].toInt()
    val minor = match.groupValues[2].toInt()
    val patch = match.groupValues[3].toInt()
    val suffix = match.groupValues[4]
    val stageBoost = if (suffix.isEmpty()) 5 else 0
    val code = major * 10_000_000 + minor * 10_000 + patch * 10 + stageBoost
    return code to clean
}

val resolvedVersion: Pair<Int, String> = run {
    val explicitName = (project.findProperty("versionName") as? String)
        ?: System.getenv("RETROHERO_VERSION_NAME")
        ?: System.getenv("GITHUB_REF_NAME")?.takeIf { it.startsWith("v") }
    val explicitCode = ((project.findProperty("versionCode") as? String)
        ?: System.getenv("RETROHERO_VERSION_CODE"))?.toIntOrNull()
    val parsed = explicitName?.let(::computeVersionFromTag)
    val fallbackCode = 20330
    val fallbackName = "0.2.33-preview"
    val code = explicitCode ?: parsed?.first ?: fallbackCode
    val name = parsed?.second ?: explicitName?.removePrefix("v") ?: fallbackName
    code to name
}

val ciKeystoreFile = System.getenv("RETROHERO_KEYSTORE_PATH")
    ?.takeIf { it.isNotBlank() }
    ?.let(::file)
    ?.takeIf { it.exists() }

android {
    namespace = "com.retrohero.app"
    compileSdk {
        version = release(37) { minorApiLevel = 1 }
    }
    ndkVersion = "27.2.12479018"

    defaultConfig {
        applicationId = "com.retrohero.app"
        minSdk = 26
        targetSdk = 36
        versionCode = resolvedVersion.first
        versionName = resolvedVersion.second

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }
    }

    signingConfigs {
        if (ciKeystoreFile != null) {
            create("persistent") {
                storeFile = ciKeystoreFile
                storePassword = System.getenv("RETROHERO_STORE_PASSWORD")
                keyAlias = System.getenv("RETROHERO_KEY_ALIAS")
                keyPassword = System.getenv("RETROHERO_KEY_PASSWORD")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            if (ciKeystoreFile != null) {
                signingConfig = signingConfigs.getByName("persistent")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Uses the persistent CI signing key whenever available so updates always install in-place
            // without uninstalling; falls back to the local debug key for developer builds.
            signingConfig = if (ciKeystoreFile != null) {
                signingConfigs.getByName("persistent")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    packaging {
        jniLibs {
            // Cores bundled in src/main/jniLibs must be extracted to disk to be dlopen()ed by path.
            useLegacyPackaging = true
        }
    }
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.foundation.ExperimentalFoundationApi",
        )
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.11.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.graphics:graphics-shapes:1.1.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("dev.chrisbanes.haze:haze:1.7.3")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.mlkit:text-recognition-japanese:16.0.1")
    implementation("com.google.mlkit:translate:17.0.3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation(composeBom)
    testImplementation("org.robolectric:robolectric:4.18.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
