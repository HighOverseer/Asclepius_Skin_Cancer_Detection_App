import org.jetbrains.kotlin.fir.declarations.builder.buildScript

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("com.google.devtools.ksp") version "1.9.10-1.0.13" apply false
}

buildscript{
    repositories{
        mavenCentral()
    }
    dependencies{
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.51.1")
    }
}