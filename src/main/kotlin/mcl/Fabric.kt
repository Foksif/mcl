package me._lisik.mcl

import com.google.gson.JsonObject
import com.google.gson.JsonParser

class Fabric(
    private val downloader: DownloadManager
) {

    fun profile(
        minecraftVersion: String,
        loaderVersion: String
    ): JsonObject {
        val url =
            "https://meta.fabricmc.net/v2/versions/loader/" +
                    "$minecraftVersion/$loaderVersion/profile/json"

        return JsonParser
            .parseString(
                downloader.read(url)
            )
            .asJsonObject
    }

    fun mavenPath(coordinate: String): String {
        val parts = coordinate.split(":")

        require(parts.size >= 3) {
            "Invalid Maven coordinate: $coordinate"
        }

        val group = parts[0].replace('.', '/')
        val artifact = parts[1]
        val version = parts[2]
        val classifier = parts.getOrNull(3)

        val fileName = buildString {
            append(artifact)
            append("-")
            append(version)

            if (!classifier.isNullOrBlank()) {
                append("-")
                append(classifier)
            }

            append(".jar")
        }

        return "$group/$artifact/$version/$fileName"
    }
}