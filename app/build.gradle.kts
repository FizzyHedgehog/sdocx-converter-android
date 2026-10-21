plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.fizzyhedgehog.sdocxconverter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fizzyhedgehog.sdocxconverter"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters.addAll(listOf("arm64-v8a"))
        }
    }

    signingConfigs {
        // keystore.jks는 보안상 저장소에서 제거됨 (2026-10-21). 로컬 빌드 머신에만 존재하며,
        // 비밀번호는 gradle.properties(gitignore 대상) 또는 환경변수로만 주입한다. 하드코딩 금지.
        val keystoreFile = file("keystore.jks")
        if (keystoreFile.exists()) {
            val storePw = System.getenv("SDOCX_STORE_PASSWORD")
                ?: project.findProperty("SDOCX_STORE_PASSWORD") as String?
            val keyPw = System.getenv("SDOCX_KEY_PASSWORD")
                ?: project.findProperty("SDOCX_KEY_PASSWORD") as String?
            require(!storePw.isNullOrBlank() && !keyPw.isNullOrBlank()) {
                "서명 비밀번호가 없습니다. \$GRADLE_USER_HOME/gradle.properties 또는 환경변수에 " +
                    "SDOCX_STORE_PASSWORD / SDOCX_KEY_PASSWORD 를 설정하세요."
            }
            create("release") {
                storeFile = keystoreFile
                storePassword = storePw
                keyAlias = "sdocx"
                keyPassword = keyPw
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let { cs -> signingConfig = cs }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation("androidx.documentfile:documentfile:1.0.1")
}
