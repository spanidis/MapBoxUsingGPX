pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Mapbox Maven repository
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            // Do not change the username below. It should always be "mapbox" (not your username).
            credentials.username = "mapbox"
            // Use the secret token stored in gradle.properties as the password
            //credentials.password = System.getProperty("MAPBOX_DOWNLOADS_TOKEN")
            credentials.password = "sk.eyJ1Ijoienp6MTk4MCIsImEiOiJjbHkycWd0dmowMDJ1MmtzNzFud2w5NjdtIn0.0RORT43PXKViPI7O34xa6Q"
            authentication.create<BasicAuthentication>("basic")
        }
    }
}

rootProject.name = "MyMapBoxGPXapp"
include(":app")
