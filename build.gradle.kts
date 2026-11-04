// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.0" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
    // KSP for Room compiler. Version must track the Kotlin one above.
    id("com.google.devtools.ksp") version "1.8.10-1.0.9" apply false
}