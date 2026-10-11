import java.io.ByteArrayOutputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingSecret(name: String): String? =
    localProperties.getProperty(name)
        ?: (project.findProperty(name) as? String)
        ?: System.getenv(name.uppercase().replace('.', '_'))

val dashdazzStorePassword = signingSecret("dashdazz.storePassword")
val dashdazzKeyPassword = signingSecret("dashdazz.keyPassword")
val dashdazzKeyAlias = signingSecret("dashdazz.keyAlias") ?: "dashdazz"

val buildingRelease = gradle.startParameter.taskNames.any {
    it.contains("Release", ignoreCase = true)
}
if (buildingRelease) {
    if (dashdazzStorePassword == null) {
        error(
            "Missing signing password. Add dashdazz.storePassword to local.properties " +
                "or pass -Pdashdazz.storePassword=..."
        )
    }
    if (dashdazzKeyPassword == null) {
        error(
            "Missing key password. Add dashdazz.keyPassword to local.properties " +
                "or pass -Pdashdazz.keyPassword=..."
        )
    }
    if (!file("dashdazz.keystore.jks").exists()) {
        error("Missing keystore file app/dashdazz.keystore.jks")
    }
}

fun gitCommitCount(): Int? = try {
    val stdout = ByteArrayOutputStream()
    val result = project.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
        standardOutput = stdout
        isIgnoreExitValue = true
    }
    if (result.exitValue == 0) stdout.toString().trim().toIntOrNull() else null
} catch (_: Exception) {
    null
}

fun gitWorkingTreeDirty(): Boolean = try {
    val stdout = ByteArrayOutputStream()
    val result = project.exec {
        commandLine("git", "status", "--porcelain")
        standardOutput = stdout
        isIgnoreExitValue = true
    }
    if (result.exitValue != 0) false else stdout.toString().trim().isNotEmpty()
} catch (_: Exception) {
    false
}

val versionMajor = 2
val versionMinor = 0

val gitCommits = gitCommitCount()
val treeDirty = gitWorkingTreeDirty()
val versionPatchBase = 47
val versionPatch = (gitCommits ?: 5) + versionPatchBase

val versionLabel = buildString {
    append("DashDazz-$versionMajor.$versionMinor.$versionPatch")
    if (treeDirty) append("-dirty")
}

android {
    namespace = "com.dashdazz.light"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dashdazz.light"
        minSdk = 34
        targetSdk = 34

        versionCode = versionPatch
        versionName = if (gitCommits != null) {
            "$versionMajor.$versionMinor.$versionPatch"
        } else {
            "2.0"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("dashdazz.keystore.jks")
            storePassword = dashdazzStorePassword
            keyAlias = dashdazzKeyAlias
            keyPassword = dashdazzKeyPassword
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = false
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
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

base.archivesName.set(versionLabel)

fun adbPath(): String {
    val sdkDir = localProperties.getProperty("sdk.dir")
    if (sdkDir != null) {
        val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
        val exe = if (isWindows) "adb.exe" else "adb"
        val adb = file("$sdkDir/platform-tools/$exe")
        if (adb.exists()) return adb.absolutePath
    }
    return if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) "adb.exe" else "adb"
}

tasks.register<Exec>("deviceLogcat") {
    group = "device"
    description = "Stream logcat for the app's own tags (including warnings) to build/device-logcat.log"

    val adb = adbPath()
    val serial = (project.findProperty("android.adb.serial") as? String)
        ?: System.getenv("ANDROID_SERIAL")

    dependsOn(":app:installDebug")

    doFirst {
        adbExecutable(adb, serial, "logcat", "-c")
    }

    commandLine(
        adb,
        *(if (serial != null) arrayOf("-s", serial) else emptyArray()),
        "logcat", "-v", "time",
        "DashDazz:W", "DashDazzService:D", "AudioReactiveEngine:D", "DashDazzNotif:D", "*:S"
    )

    standardOutput = layout.buildDirectory.file("device-logcat.log").get().asFile.outputStream()
    errorOutput = standardOutput
    isIgnoreExitValue = true
}

fun adbExecutable(adb: String, serial: String?, vararg args: String) {
    try {
        val cmd = if (serial != null) arrayOf(adb, "-s", serial) + args else arrayOf(adb) + args
        ProcessBuilder(*cmd)
            .redirectErrorStream(true)
            .start()
            .waitFor()
    } catch (_: Exception) {
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-core")

    implementation("top.yukonga.miuix.kmp:miuix-android:0.3.5")

    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    testImplementation("junit:junit:4.13.2")
}

tasks.register("printVersion") {
    doLast {
        val suffix = if (treeDirty) " (tree dirty — debug APK marked -dirty)" else ""
        logger.lifecycle("---------------------------------------------")
        logger.lifecycle("  Version name        : $versionMajor.$versionMinor.$versionPatch$suffix")
        logger.lifecycle("  VersionCode (build) : $versionPatch  (git commit count + $versionPatchBase)")
        logger.lifecycle("  Commits on HEAD     : ${gitCommits ?: "git unavailable"}")
        logger.lifecycle("---------------------------------------------")
        logger.lifecycle("  Every commit bumps these automatically.")
    }
}
