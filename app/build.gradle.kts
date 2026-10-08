plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Supply only to the local release process; never put passwords in tracked properties.
val releaseStore = providers.environmentVariable("MWP_RELEASE_STORE_FILE").orNull
val releaseAlias = providers.environmentVariable("MWP_RELEASE_KEY_ALIAS").orNull
val releaseStorePassword = providers.environmentVariable("MWP_RELEASE_STORE_PASSWORD").orNull
val releaseKeyPassword = providers.environmentVariable("MWP_RELEASE_KEY_PASSWORD").orNull
val hasReleaseSigning = listOf(releaseStore, releaseAlias, releaseStorePassword, releaseKeyPassword)
    .all { !it.isNullOrBlank() }

abstract class CopyLegalAssets : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
    @get:javax.inject.Inject
    abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun copyFiles() {
        fileSystemOperations.copy {
            from(sourceFiles)
            into(outputDirectory.dir("legal"))
        }
    }
}
val copyLegalAssets = tasks.register<CopyLegalAssets>("copyLegalAssets") {
    sourceFiles.from(rootProject.files("LICENSE", "LICENSE-APACHE-2.0.txt", "THIRD_PARTY_NOTICES.md", "PRIVACY.md"))
    outputDirectory.set(layout.buildDirectory.dir("generated/legalAssets"))
}

android {
    namespace = "uk.ac.warwick.plus"
    compileSdk { version = release(37) { minorApiLevel = 0 } }
    defaultConfig {
        applicationId = "io.github.nook001.mywarwickplus"
        minSdk = 28
        targetSdk = 37
        versionCode = 40
        versionName = "0.26.0-beta.2"
        buildConfigField("boolean", "PERFORMANCE_TRACING", "false")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true; resValues = true }
    androidResources { localeFilters += "en" }
    packaging {
        resources.excludes += setOf("**/*.kotlin_builtins", "DebugProbesKt.bin")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    signingConfigs {
        if (hasReleaseSigning) create("release") {
            storeFile = file(requireNotNull(releaseStore))
            keyAlias = releaseAlias
            storePassword = releaseStorePassword
            keyPassword = releaseKeyPassword
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "MyWarwick+ Debug")
        }
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("profile") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".profile"
            versionNameSuffix = "-profile"
            resValue("string", "app_name", "MyWarwick+ Profile")
            isDebuggable = false
            matchingFallbacks += "release"
            buildConfigField("boolean", "PERFORMANCE_TRACING", "true")
        }
    }
}
androidComponents.onVariants { variant ->
    variant.sources.assets?.addGeneratedSourceDirectory(copyLegalAssets, CopyLegalAssets::outputDirectory)
}

// Unsigned builds remain useful for source contributors. Distribution requires this check.
tasks.register("checkReleaseSigning") {
    doLast {
        check(hasReleaseSigning) { "Release signing is missing. Use tools/release/package.ps1." }
        check(file(requireNotNull(releaseStore)).isFile) { "Release keystore was not found." }
    }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

composeCompiler {
    if (providers.gradleProperty("composeReports").orNull == "true") {
        reportsDestination.set(layout.buildDirectory.dir("reports/compose"))
        metricsDestination.set(layout.buildDirectory.dir("reports/compose"))
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.browser:browser:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    implementation("androidx.room:room-runtime:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20260814")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    // Compose's transitive Espresso version uses reflection removed on newer Android releases.
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }
