import java.io.File
import org.w3c.dom.Element

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.compose.compiler)
}

android {
    // Code namespace stays dev.trooped.tvquickbars (upstream packages, no user-visible
    // impact); only the shipped application id is rebranded.
    namespace = "dev.trooped.tvquickbars"
    compileSdk = 36

    defaultConfig {
        // Bing-Bong's own Play identity. NOTE: this permanently forks the app's
        // identity -- Play treats com.zaiemv.bingbong as a different app from
        // upstream's dev.trooped.tvquickbars, forever. Never change it again
        // after the first Play upload.
        applicationId = "com.zaiemv.bingbong"
        minSdk = 28
        targetSdk = 35
        // CI overrides these via -PciVersionCode / -PciVersionName when publishing
        // sideload releases; local builds keep the values below.
        val baseVersionName = "1.3.3"
        versionCode = (findProperty("ciVersionCode") as String?)?.toIntOrNull() ?: 25
        versionName = (findProperty("ciVersionName") as String?) ?: baseVersionName
    }

    signingConfigs {
        create("release") {
            // Read properties for the keystore file location and passwords
            val keystorePath = findProperty("HA_QUICKBARS_KEYSTORE_FILE") as String?

            if (keystorePath != null) {
                val keystoreFile = file(keystorePath)
                if (keystoreFile.exists()) {
                    storeFile = keystoreFile
                    storePassword = System.getenv("HA_QUICKBARS_KEYSTORE_PASSWORD") ?: findProperty(
                        "HA_QUICKBARS_KEYSTORE_PASSWORD"
                    ) as String?
                    keyAlias = System.getenv("HA_QUICKBARS_KEY_ALIAS")
                        ?: findProperty("HA_QUICKBARS_KEY_ALIAS") as String?
                    keyPassword = System.getenv("HA_QUICKBARS_KEY_PASSWORD")
                        ?: findProperty("HA_QUICKBARS_KEY_PASSWORD") as String?
                }
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }

        create("profileable") {
            // This copies settings from your release build (like code shrinking)
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".profileable"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    composeOptions{
        kotlinCompilerExtensionVersion = "2.0.21"
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.navigation.runtime.android)
    val composeBom = platform("androidx.compose:compose-bom:2025.09.01")
    implementation(composeBom)
    testImplementation(composeBom)
    androidTestImplementation(composeBom)

    val lifecycle_version = "2.9.1"

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.leanback:leanback:1.0.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.fragment:fragment-ktx:1.8.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.mikepenz:material-design-iconic-typeface:2.2.0.8-kotlin")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.security:security-crypto-ktx:1.1.0-beta01") // Updated to beta
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") // Updated
    implementation("com.google.zxing:core:3.5.3")
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation(libs.androidx.core.animation)
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    // ―― Crypto layer -----------------------------------------------------
    implementation("com.google.crypto.tink:tink-android:1.18.0")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${lifecycle_version}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${lifecycle_version}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${lifecycle_version}")
    implementation("androidx.lifecycle:lifecycle-service:${lifecycle_version}")
    implementation("androidx.savedstate:savedstate-ktx:1.3.0")

    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // Jetpack Compose Dependencies (Updated)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.11.0") // This is not in the BOM, so it needs its own version.
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("org.jmdns:jmdns:3.6.2")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.caverock:androidsvg:1.4")
    //implementation("io.coil-kt:coil-svg:2.7.0")

    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-exoplayer-rtsp:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")
    implementation("androidx.media3:media3-ui-compose:1.8.0")
}

// ---------------------------------------------------------------------------
// Regression guard for the splash AnimatedVectorDrawable.
//
// Every <target android:name="..."> in an animated-vector must resolve to an
// element carrying that android:name in the vector it animates. When it does
// not, AnimatedVectorDrawable.start() throws at runtime:
//   IllegalStateException: Target with the name "..." cannot be found in the
//   VectorDrawable to be animated
// That exact crash shipped after the Bing-Bong rebrand replaced icon_svg.xml
// (new logo, no named elements) while icon_animated.xml kept targeting the old
// names. This task makes that class of breakage fail the build instead of only
// crashing on device. It uses only the JDK XML parser -- no added dependency.
// Wired into preBuild so it also runs in CI (assembleRelease).
// ---------------------------------------------------------------------------
val verifyAvdTargets by tasks.registering {
    group = "verification"
    description = "Fails if any animated-vector <target> name is absent from the vector it animates."
    val resDir = layout.projectDirectory.dir("src/main/res")
    inputs.dir(resDir)
    doLast {
        val ns = "http://schemas.android.com/apk/res/android"
        val dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = true
        val db = dbf.newDocumentBuilder()
        val drawableDir = resDir.dir("drawable").asFile
        val xmls = drawableDir.listFiles { f -> f.isFile && f.extension == "xml" }?.sorted() ?: emptyList()

        val problems = mutableListOf<String>()
        var avdCount = 0
        for (avd in xmls) {
            val avdRoot = db.parse(avd).documentElement
            if (avdRoot.tagName != "animated-vector") continue
            avdCount++

            val drawableRef = avdRoot.getAttributeNS(ns, "drawable") // e.g. "@drawable/icon_svg"
            val vectorName = drawableRef.substringAfterLast('/')
            val vectorFile = File(drawableDir, "$vectorName.xml")
            if (!vectorFile.exists()) {
                problems += "${avd.name}: android:drawable=\"$drawableRef\" -> $vectorName.xml not found"
                continue
            }

            val names = HashSet<String>()
            val els = db.parse(vectorFile).getElementsByTagName("*")
            for (i in 0 until els.length) {
                val el = els.item(i) as org.w3c.dom.Element
                val n = el.getAttributeNS(ns, "name")
                if (n.isNotEmpty()) names += n
            }

            val targets = db.parse(avd).getElementsByTagName("target")
            for (i in 0 until targets.length) {
                val t = targets.item(i) as org.w3c.dom.Element
                val tn = t.getAttributeNS(ns, "name")
                if (tn.isNotEmpty() && tn !in names) {
                    problems += "${avd.name}: <target name=\"$tn\"> has no element with that android:name in $vectorName.xml"
                }
            }
        }

        if (problems.isNotEmpty()) {
            throw org.gradle.api.GradleException(
                "verifyAvdTargets: AnimatedVectorDrawable target(s) do not resolve:\n  " +
                    problems.joinToString("\n  ")
            )
        }
        logger.lifecycle("verifyAvdTargets: OK ($avdCount animated-vector drawable(s) checked).")
    }
}

tasks.named("preBuild") { dependsOn(verifyAvdTargets) }
