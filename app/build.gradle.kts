import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.io.File
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.autoscan.elm327.mcujum"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // GEMINI_API_KEY берётся из app/.env (его читает Secrets-плагин). Если ключа
    // нет или он пустой (`GEMINI_API_KEY=`), подставляем заглушку — иначе
    // генерируется невалидный BuildConfig вида `String GEMINI_API_KEY = ;`.
    // Файл .env читаем напрямую, чтобы контролировать значение поля.
    val geminiApiKey: String = run {
      val props = Properties()
      val envFile = File(projectDir, ".env")
      if (envFile.exists()) envFile.inputStream().use { props.load(it) }
      props.getProperty("GEMINI_API_KEY")?.takeIf { it.isNotBlank() }
        ?: providers.environmentVariable("GEMINI_API_KEY").orNull?.takeIf { it.isNotBlank() }
        ?: ""
    }
    // Экранируем кавычки/обратные слэши, чтобы значение всегда было валидной
    // Java-строкой независимо от содержимого .env
    val escaped = geminiApiKey.ifBlank { "MY_GEMINI_API_KEY" }
      .replace("\\", "\\\\").replace("\"", "\\\"")
    buildConfigField("String", "GEMINI_API_KEY", "\"$escaped\"")
  }

  signingConfigs {
    // Стандартный Android debug-ключ: берём ~/.android/debug.keystore (создаётся
    // автоматически при сборке в Android Studio). Если его нет — генерируем в корне
    // проекта через keytool. Файл keystore в Git не хранится.
    val userDebugKeystore = File(System.getProperty("user.home"), ".android/debug.keystore")
    val projectDebugKeystore = File(rootDir, "debug.keystore")
    val debugStoreFile = when {
      userDebugKeystore.exists() -> userDebugKeystore
      projectDebugKeystore.exists() -> projectDebugKeystore
      else -> {
        if (!projectDebugKeystore.exists()) {
          val javaHome = System.getProperty("java.home")
          val keytool = File(javaHome, if (File(javaHome, "bin/keytool").exists())
            "bin/keytool" else "bin/keytool.exe")
          if (keytool.exists()) {
            providers.exec {
              commandLine(keytool.absolutePath, "-genkeypair",
                "-keystore", projectDebugKeystore.absolutePath,
                "-alias", "androiddebugkey",
                "-storepass", "android", "-keypass", "android",
                "-keyalg", "RSA", "-keysize", "2048",
                "-validity", "10000",
                "-dname", "CN=Android Debug,O=Android,C=US")
            }.result.get().assertNormalExitValue()
          }
        }
        projectDebugKeystore
      }
    }

    create("debugConfig") {
      storeFile = debugStoreFile
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Release-подпись включается только если задан KEYSTORE_PATH (например, в CI).
      // Без него release собирается unsigned — это не мешает assembleDebug.
      val releaseKeystore = System.getenv("KEYSTORE_PATH")?.let { File(it) }
      if (releaseKeystore != null && releaseKeystore.exists()) {
        signingConfigs.create("release") {
          storeFile = releaseKeystore
          storePassword = System.getenv("STORE_PASSWORD")
          keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
          keyPassword = System.getenv("KEY_PASSWORD")
        }
        signingConfig = signingConfigs.getByName("release")
      }
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
