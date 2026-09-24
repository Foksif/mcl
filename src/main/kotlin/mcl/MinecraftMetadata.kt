package me._lisik.mcl

import com.google.gson.JsonObject
import com.google.gson.JsonParser

class MinecraftMetadata(
    private val downloader: DownloadManager
) {

    companion object {
        private const val VERSION_MANIFEST =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    }

    fun version(version: String): JsonObject {
        val root = JsonParser
            .parseString(
                downloader.read(VERSION_MANIFEST)
            )
            .asJsonObject

        val entry = root
            .getAsJsonArray("versions")
            .firstOrNull {
                it.asJsonObject
                    .get("id")
                    .asString == version
            }
            ?.asJsonObject
            ?: error("Minecraft version $version not found")

        return JsonParser
            .parseString(
                downloader.read(
                    entry.get("url").asString
                )
            )
            .asJsonObject
    }
}