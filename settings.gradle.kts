pluginManagement {
    repositories {
        // Plugin markers resolve best from portal / central
        gradlePluginPortal()
        mavenCentral()
        // AndroidX / Google artifacts — Java cannot reach dl.google.com here
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        mavenCentral()
        google()
    }
}

rootProject.name = "XrayPulse"
include(":app")
