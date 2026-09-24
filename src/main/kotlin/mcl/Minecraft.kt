package me._lisik.mcl

import com.google.gson.JsonObject
import java.nio.file.Files
import java.nio.file.Path
import java.io.File
import com.google.gson.JsonParser

class Minecraft(
    private val home: Path,
    val version: String,
    val loader: String? = null
) {

    companion object {
        private const val MINECRAFT_REPOSITORY =
            "https://libraries.minecraft.net/"

        private const val FABRIC_REPOSITORY =
            "https://maven.fabricmc.net/"
    }

    private val downloader =
        DownloadManager()

    private val metadata =
        MinecraftMetadata(downloader)

    private val fabricApi =
        Fabric(downloader)


    private val instanceId =
        if (loader != null) {
            "$version-$loader"
        } else {
            version
        }

    val directory: Path
        get() = home
            .resolve("instances")
            .resolve(instanceId)

    private val versionsDirectory =
        home.resolve("versions")

    private val librariesDirectory =
        home.resolve("libraries")

    private val assetsDirectory =
        home.resolve("assets")

    private val nativesDirectory =
        home.resolve("natives")
            .resolve(instanceId)

    private var vanilla: JsonObject? = null
    private var fabric: JsonObject? = null

    private var installed = false

    fun install(
        status: ((String) -> Unit)? = null
    ) {
        prepareDirectories()

        status?.invoke(
            "Loading Minecraft $version metadata"
        )

        val minecraft =
            metadata.version(version)

        vanilla = minecraft

        status?.invoke(
            "Downloading Minecraft"
        )

        downloadClient(
            minecraft,
            status
        )

        if (loader != null) {
            status?.invoke(
                "Loading Fabric $loader metadata"
            )

            fabric =
                fabricApi.profile(
                    version,
                    loader
                )

            status?.invoke(
                "Downloading Fabric libraries"
            )

            downloadFabricLibraries(
                fabric!!,
                status
            )
        }

        status?.invoke(
            "Downloading Minecraft libraries"
        )

        downloadLibraries(
            minecraft,
            status
        )

        status?.invoke(
            "Downloading assets"
        )

        downloadAssets(
            minecraft,
            status
        )

        installed = true

        status?.invoke(
            "Minecraft $version installed"
        )
    }

    fun launch(
        user: MinecraftUser,
        jvmArguments: List<String> = emptyList(),
        gameArguments: List<String> = emptyList(),
        java: Path? = null,
        output: ((String) -> Unit)? = null
    ): MinecraftProcess {

        check(installed) {
            "Minecraft is not installed. Call install() first."
        }

        require(
            user.username.matches(
                Regex("[A-Za-z0-9_]{3,16}")
            )
        ) {
            "Invalid Minecraft username"
        }

        val minecraft =
            vanilla ?: error("Metadata not loaded")

        val classpath =
            buildClasspath(minecraft)

        val mainClass =
            fabric
                ?.get("mainClass")
                ?.asString
                ?: "net.minecraft.client.main.Main"

        val nativesPath =
            nativesDirectory
                .toAbsolutePath()
                .normalize()

        val jnaPath =
            nativesPath
                .resolve("jna")

        val lwjglPath =
            nativesPath
                .resolve("lwjgl")

        val nettyPath =
            nativesPath
                .resolve("netty")

        Files.createDirectories(nativesPath)
        Files.createDirectories(jnaPath)
        Files.createDirectories(lwjglPath)
        Files.createDirectories(nettyPath)

        val systemLibraryPath =
            when (CurrentPlatform.platform) {
                Platform.LINUX ->
                    "/run/opengl-driver/lib"

                else ->
                    ""
            }

        val currentJavaLibraryPath =
            System.getProperty("java.library.path")
                .orEmpty()

        val javaLibraryPath =
            listOf(
                lwjglPath.toString(),
                nativesPath.toString(),
                systemLibraryPath,
                currentJavaLibraryPath
            )
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(File.pathSeparator)

        val lwjglLibraryPath =
            lwjglPath.toString()

        val arguments =
            mutableListOf<String>()

        arguments += "--enable-native-access=ALL-UNNAMED"

        arguments += "-Djava.library.path=$javaLibraryPath"
        arguments += "-Dorg.lwjgl.librarypath=$lwjglLibraryPath"

        arguments += "-Djna.tmpdir=$jnaPath"
        arguments += "-Dorg.lwjgl.system.SharedLibraryExtractPath=$lwjglPath"
        arguments += "-Dio.netty.native.workdir=$nettyPath"

        arguments += "-Xms512M"
        arguments += "-Xmx2G"

        arguments += jvmArguments

        arguments += "-cp"
        arguments += classpath

        arguments += mainClass

        arguments += buildGameArguments(
            minecraft,
            user,
            gameArguments
        )

        val javaExecutable =
            java ?: findJava()

        val command =
            mutableListOf<String>()

        command += javaExecutable.toString()
        command += arguments

        output?.invoke(
            "Launching Minecraft: ${command.joinToString(" ")}"
        )

        val processBuilder =
            ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)

        if (CurrentPlatform.platform == Platform.LINUX) {
            val environment =
                processBuilder.environment()

            val currentLdLibraryPath =
                environment["LD_LIBRARY_PATH"]
                    .orEmpty()

            val ldLibraryPath =
                listOf(
                    lwjglPath.toString(),
                    nativesPath.toString(),
                    systemLibraryPath,
                    currentLdLibraryPath
                )
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(File.pathSeparator)

            environment["LD_LIBRARY_PATH"] =
                ldLibraryPath
        }

        val process =
            processBuilder.start()

        Thread {
            process.inputStream
                .bufferedReader()
                .useLines { lines ->
                    lines.forEach { line ->
                        output?.invoke(line)
                    }
                }
        }.apply {
            isDaemon = true
            start()
        }

        return MinecraftProcess(process)
    }

    private fun prepareDirectories() {
        Files.createDirectories(directory)

        Files.createDirectories(
            directory.resolve("mods")
        )

        Files.createDirectories(
            directory.resolve("resourcepacks")
        )

        Files.createDirectories(
            directory.resolve("saves")
        )

        Files.createDirectories(
            versionsDirectory
        )

        Files.createDirectories(
            librariesDirectory
        )

        Files.createDirectories(
            assetsDirectory
        )

        Files.createDirectories(
            nativesDirectory
        )

        Files.createDirectories(
            nativesDirectory.resolve("java")
        )

        Files.createDirectories(
            nativesDirectory.resolve("jna")
        )

        Files.createDirectories(
            nativesDirectory.resolve("lwjgl")
        )

        Files.createDirectories(
            nativesDirectory.resolve("netty")
        )
    }

    private fun downloadClient(
        json: JsonObject,
        status: ((String) -> Unit)?
    ) {
        val client =
            json.getAsJsonObject("downloads")
                .getAsJsonObject("client")

        val target =
            versionsDirectory
                .resolve(version)
                .resolve("$version.jar")

        downloader.download(
            client.get("url").asString,
            target,
            client.get("sha1")?.asString,
            status
        )
    }

    private fun downloadLibraries(
        json: JsonObject,
        status: ((String) -> Unit)?
    ) {
        val downloads =
            json.getAsJsonArray("libraries")
                .mapNotNull { element ->

                    val library =
                        element.asJsonObject

                    if (!rulesAllow(library)) {
                        return@mapNotNull null
                    }

                    val artifact =
                        library
                            .getAsJsonObject("downloads")
                            ?.getAsJsonObject("artifact")
                            ?: return@mapNotNull null

                    DownloadManager.Download(
                        url = artifact.get("url").asString,
                        target = librariesDirectory.resolve(
                            artifact.get("path").asString
                        ),
                        sha1 = artifact.get("sha1")?.asString
                    )
                }

        downloader.downloadAll(
            downloads,
            status
        )
    }

    private fun downloadFabricLibraries(
        json: JsonObject,
        status: ((String) -> Unit)?
    ) {
        val downloads =
            json.getAsJsonArray("libraries")
                .mapNotNull { element ->
                    val library = element.asJsonObject

                    val name =
                        library.get("name")
                            ?.asString
                            ?: return@mapNotNull null

                    val baseUrl =
                        library.get("url")
                            ?.asString
                            ?: FABRIC_REPOSITORY

                    val path =
                        fabricApi.mavenPath(name)

                    DownloadManager.Download(
                        url = baseUrl.trimEnd('/') + "/" + path,
                        target = librariesDirectory.resolve(path)
                    )
                }

        downloader.downloadAll(
            downloads,
            status
        )
    }

    private fun downloadAssets(
        json: JsonObject,
        status: ((String) -> Unit)?
    ) {
        val assetIndex =
            json.getAsJsonObject("assetIndex")

        val id =
            assetIndex.get("id").asString

        val indexPath =
            assetsDirectory
                .resolve("indexes")
                .resolve("$id.json")

        downloader.download(
            assetIndex.get("url").asString,
            indexPath,
            assetIndex.get("sha1")?.asString,
            status
        )

        val index =
            JsonParser
                .parseString(
                    Files.readString(indexPath)
                )
                .asJsonObject

        val downloads =
            index.getAsJsonObject("objects")
                .entrySet()
                .map { (_, element) ->

                    val hash =
                        element.asJsonObject
                            .get("hash")
                            .asString

                    val prefix =
                        hash.substring(0, 2)

                    val target =
                        assetsDirectory
                            .resolve("objects")
                            .resolve(prefix)
                            .resolve(hash)

                    DownloadManager.Download(
                        url =
                            "https://resources.download.minecraft.net/" +
                                    "$prefix/$hash",
                        target = target,
                        sha1 = hash
                    )
                }

        downloader.downloadAll(
            downloads,
            status
        )
    }

    private fun buildClasspath(
        json: JsonObject
    ): String {

        val result =
            linkedSetOf<Path>()

        json.getAsJsonArray("libraries")
            .forEach { element ->

                val library =
                    element.asJsonObject

                if (!rulesAllow(library)) {
                    return@forEach
                }

                val artifact =
                    library
                        .getAsJsonObject("downloads")
                        ?.getAsJsonObject("artifact")
                        ?: return@forEach

                result.add(
                    librariesDirectory.resolve(
                        artifact.get("path").asString
                    )
                )
            }

        fabric
            ?.getAsJsonArray("libraries")
            ?.forEach { element ->

                val library =
                    element.asJsonObject

                val name =
                    library.get("name")
                        ?.asString
                        ?: return@forEach

                result.add(
                    librariesDirectory.resolve(
                        fabricApi.mavenPath(name)
                    )
                )
            }

        result.add(
            versionsDirectory
                .resolve(version)
                .resolve("$version.jar")
        )

        return result.joinToString(
            File.pathSeparator
        ) {
            it.toAbsolutePath()
                .normalize()
                .toString()
        }
    }

    private fun buildGameArguments(
        json: JsonObject,
        user: MinecraftUser,
        extra: List<String>
    ): List<String> {

        val result =
            mutableListOf<String>()

        result += "--username"
        result += user.username

        result += "--version"
        result += version

        result += "--gameDir"
        result += directory.toAbsolutePath().toString()

        result += "--assetsDir"
        result += assetsDirectory.toAbsolutePath().toString()

        result += "--assetIndex"
        result += json
            .getAsJsonObject("assetIndex")
            .get("id")
            .asString

        result += "--uuid"
        result += user.uuid

        result += "--accessToken"
        result += user.accessToken

        result += "--userType"
        result += user.userType

        result += "--versionType"
        result += "release"

        result += extra

        return result
    }

    private fun rulesAllow(
        library: JsonObject
    ): Boolean {

        val rules =
            library.getAsJsonArray("rules")
                ?: return true

        var allowed = false

        rules.forEach { element ->

            val rule =
                element.asJsonObject

            val action =
                rule.get("action")
                    ?.asString
                    ?: return@forEach

            val os =
                rule.getAsJsonObject("os")

            if (os == null) {
                allowed = action == "allow"
                return@forEach
            }

            val name =
                os.get("name")?.asString

            val arch =
                os.get("arch")?.asString

            val matches =
                (name == null ||
                        name == CurrentPlatform.id) &&
                        (arch == null ||
                                arch == CurrentPlatform.architecture)

            if (matches) {
                allowed = action == "allow"
            }
        }

        return allowed
    }

    private fun findJava(): Path {
        val executable =
            if (CurrentPlatform.platform ==
                Platform.WINDOWS
            ) {
                "java.exe"
            } else {
                "java"
            }

        return Path.of(
            System.getProperty("java.home"),
            "bin",
            executable
        )
    }
}