import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.compose.compiler)
}
apply(from = "../version.gradle.kts")

//applyKtorWasmWorkaround(libs.versions.ktor.get())

kotlin {
//    @OptIn(ExperimentalWasmDsl::class)
//    listOf(
//        js(),
//        wasmJs(),
//    ).forEach {
//        it.moduleName = "ShowcaseApp"
//        it.browser {
//            commonWebpackConfig {
//                outputFileName = "ShowcaseApp.js"
////                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
////                    static = (static ?: mutableListOf()).apply {
////                        // Serve sources to debug inside browser
////                        add(project.projectDir.path)
////                    }
////                }
//            }
//        }
//        it.binaries.executable()
//    }

    androidTarget {
        compilations.all {
            compileTaskProvider {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
//                    jvmTarget.set(JvmTarget.JVM_1_8)
//                    freeCompilerArgs.add("-Xjdk-release=${JavaVersion.VERSION_1_8}")
                }
            }
        }
        // https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-test.html
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant {
            sourceSetTree.set(KotlinSourceSetTree.test)
            dependencies {
                debugImplementation(libs.androidx.testManifest)
                implementation(libs.androidx.junit4)
            }
        }
    }
    
    jvm("desktop")
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
//        tvosX64(),
//        tvosArm64(),
//        tvosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.datetime)
            implementation(libs.coil)
            implementation(libs.coil.network.ktor)
            implementation(libs.napier)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.ktor.client.serialization.kotlinx.json)
            implementation(libs.okio)
            implementation(libs.kstore)
            implementation(libs.compottie)
            implementation(libs.xmlutil.core)
            implementation(libs.xmlutil.serialization)
            implementation(libs.navigation.compose)
            implementation(libs.filekit.core)
            implementation(libs.filekit.compose)
            implementation(libs.ktor.network)
            val supabaseBom = project.dependencies.platform(libs.supabase)
            implementation(supabaseBom)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.realtime)
            implementation(libs.supabase.auth)
            implementation(project(":showcase-api"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.okio.fakefs)
        }

        androidMain.dependencies {
            api(libs.androidx.activity.compose)
            api(libs.androidx.appcompat)
            api(libs.androidx.core.ktx)
            api(libs.compose.ui.tooling.preview)

            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.bundles.lottie)
            implementation(compose.uiTooling)
            implementation(libs.kstore.file)

        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.ktor.client.ios)
            implementation(libs.kstore.file)
        }

//        jsMain.dependencies {
//            implementation(libs.kstore.storage)
//            implementation(libs.okio.js)
//            implementation(libs.ktor.client.js)
//        }

//        wasmJsMain.dependencies {
//            implementation(libs.kstore.storage)
//            implementation(libs.okio.js)
//            implementation(libs.ktor.client.js)
//            implementation(npm("uuid", "9.0.0"))
//        }


        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.common)
                implementation(compose.desktop.currentOs)
                implementation(libs.flatlaf)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.kstore.file)
                implementation(libs.appdirs)
            }
        }
    }
}


android {
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    namespace = "com.alpha.showcase.common"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        testOptions.targetSdk = libs.versions.android.targetSdk.get().toInt()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        jvmToolchain(17)
    }
}

// https://youtrack.jetbrains.com/issue/KTOR-5587
fun Project.applyKtorWasmWorkaround(version: String) {
    configurations.all {
        if (name.startsWith("wasmJs")) {
            resolutionStrategy.eachDependency {
                if (requested.group.startsWith("io.ktor") &&
                    requested.name.startsWith("ktor-client-")) {
                    useVersion(version)
                }
            }
        }
    }
}


buildConfig {
    // BuildConfig configuration here.
    // https://github.com/gmazzo/gradle-buildconfig-plugin#usage-in-kts
    useKotlinOutput { topLevelConstants = true }
    packageName("com.alpha.showcase.common")

    val localProperties = gradleLocalProperties(rootDir, providers)
    val tmdbApiKey: String = localProperties.getProperty("TMDB_API_KEY")
    require(tmdbApiKey.isNotEmpty()) {
        "Register your api TMDB_API_KEY place it in local.properties as `TMDB_API_KEY`"
    }

    val pexelsApiKey: String = localProperties.getProperty("PEXELS_API_KEY")
    require(pexelsApiKey.isNotEmpty()) {
        "Register your api PEXELS_API_KEY place it in local.properties as `PEXELS_API_KEY`"
    }

    val unsplashApiKey: String = localProperties.getProperty("UNSPLASH_API_KEY")
    require(unsplashApiKey.isNotEmpty()) {
        "Register your api UNSPLASH_API_KEY place it in local.properties as `UNSPLASH_API_KEY`"
    }


    buildConfigField("PEXELS_API_KEY", pexelsApiKey)
    buildConfigField("UNSPLASH_API_KEY", unsplashApiKey)
    buildConfigField("TMDB_API_KEY", tmdbApiKey)


    val supabase_url: String = localProperties.getProperty("SUPABASE_URL")
    val supabase_anon_key: String = localProperties.getProperty("SUPABASE_ANON_KEY")

    buildConfigField("SUPABASE_URL", supabase_url)
    buildConfigField("SUPABASE_ANON_KEY", supabase_anon_key)


    val versionCode: String = project.extra["versionCode"].toString()
    val versionName: String = project.extra["versionName"].toString()
    val gitHash: String = project.extra["gitHash"].toString()
    val versionHash: String = project.extra["versionHash"].toString()
    val author: String = project.extra["author"].toString()
    val email: String = project.extra["email"].toString()

    buildConfigField("versionCode", versionCode)
    buildConfigField("versionName", versionName)
    buildConfigField("gitHash", gitHash)
    buildConfigField("versionHash", versionHash)
    buildConfigField("author", author)
    buildConfigField("email", email)

    buildConfigField("DEBUG", true)

    println("--------------------------------")
    println("versionCode: $versionCode")
    println("versionName: $versionName")
    println("gitHash: $gitHash")
    println("versionHash: $versionHash")
    println("author: $author")
    println("email: $email")
    println("--------------------------------")
}