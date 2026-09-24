plugins { id("com.android.application") version "9.4.1" apply false }

// Keep generated files out of OneDrive: its directory locks break incremental builds.
// Sources remain in the user-selected project; separate checkouts get separate caches.
val outputRoot = file(providers.gradleProperty("savingsBuildRoot").getOrElse(
    "${gradle.gradleUserHomeDir}/savings-guard-builds/${rootDir.absolutePath.hashCode().toUInt().toString(16)}"))
allprojects {
    layout.buildDirectory.set(File(outputRoot, if (path == ":") "root" else path.removePrefix(":").replace(':', '/')))
}
