// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.services) apply false

    //  id("com.google.gms.google-services") version "4.4.4" apply false
}

//dependencies {
//    implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
//    // TODO: Add the dependencies for Firebase products you want to use
//    // When using the BoM, don't specify versions in Firebase dependencies
//    implementation("com.google.firebase:firebase-analytics")
//    // Add the dependencies for any other desired Firebase products
//    // https://firebase.google.com/docs/android/setup#available-libraries
//}
