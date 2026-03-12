plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "redcom1988.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    // OkHttp core is exposed via NetworkHelper.client property and extension functions
    // Data layer needs direct access to OkHttp types (Response, Request, Headers, etc.)
    api(libs.okhttp.core)
    
    // These are only used internally by NetworkHelper, not exposed in public API
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp.brotli)
    implementation(libs.okhttp.dnsoverhttps)
    
    // Serialization types are used in extension functions (parseAs, json object)
    // Data layer uses these for JSON encoding/decoding
    api(libs.serialization.json)
    api(libs.serialization.json.okio)
    api(libs.okio)
}