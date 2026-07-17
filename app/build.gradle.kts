import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    kotlin("android")
    kotlin("kapt")
    id("com.android.application")
}

dependencies {
    compileOnly(project(":hideapi"))

    implementation(project(":core"))
    implementation(project(":service"))
    implementation(project(":design"))
    implementation(project(":common"))

    implementation(libs.kotlin.coroutine)
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.coordinator)
    implementation(libs.androidx.recyclerview)
    implementation(libs.google.material)
    implementation(libs.quickie.bundled)
    implementation(libs.androidx.activity.ktx)
}

tasks.getByName("clean", type = Delete::class) {
    delete(file("release"))
}

abstract class DownloadGeoFilesTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun download() {
        val geoFilesUrls = mapOf(
            "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/geoip.metadb" to "geoip.metadb",
            "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/geosite.dat" to "geosite.dat",
            // "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/country.mmdb" to "country.mmdb",
            "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/GeoLite2-ASN.mmdb" to "ASN.mmdb",
            "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/BundleMRS.7z" to "BundleMRS.7z",
        )

        // The URLs point at the floating "latest" release, so an existing file
        // can never be proven outdated; download only what is missing and let
        // `clean` force a refresh.
        geoFilesUrls.forEach { (downloadUrl, outputFileName) ->
            val outputPath = outputDir.get().file(outputFileName).asFile
            if (outputPath.exists()) {
                return@forEach
            }
            outputPath.parentFile.mkdirs()

            // Download via a temp file and move atomically so an interrupted
            // build can never leave a truncated file that "exists" and is
            // then skipped by the check above.
            val partPath = outputDir.get().file("$outputFileName.part").asFile
            URL(downloadUrl).openStream().use { input ->
                Files.copy(input, partPath.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            Files.move(
                partPath.toPath(),
                outputPath.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
            logger.lifecycle("$outputFileName downloaded to $outputPath")
        }
    }
}

val downloadGeoFiles = tasks.register<DownloadGeoFilesTask>("downloadGeoFiles") {
    outputDir.set(layout.buildDirectory.dir("generated/geoAssets"))
}

// addGeneratedSourceDirectory wires the task dependency into every consumer of
// the assets (mergeAssets, lint model writers, ...) automatically, and keeps
// the downloads under build/ where clean already removes them.
androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            downloadGeoFiles,
            DownloadGeoFilesTask::outputDir,
        )
    }
}