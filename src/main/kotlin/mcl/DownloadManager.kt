package me._lisik.mcl

import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.Executors

class DownloadManager(
    private val userAgent: String = "MCL/1.0",
    private val parallelism: Int = 8
) {

    private val executor =
        Executors.newFixedThreadPool(parallelism)

    fun read(url: String): String {
        val connection = open(url)

        return connection.inputStream
            .bufferedReader()
            .use { it.readText() }
    }

    fun download(
        url: String,
        target: Path,
        sha1: String? = null,
        status: ((String) -> Unit)? = null
    ) {
        Files.createDirectories(target.parent)

        if (Files.exists(target)) {
            if (
                sha1 == null ||
                sha1(target).equals(sha1, ignoreCase = true)
            ) {
                return
            }

            Files.delete(target)
        }

        status?.invoke("Downloading ${target.fileName}")

        val temporary =
            target.resolveSibling("${target.fileName}.tmp")

        try {
            val connection = open(url)

            BufferedInputStream(
                connection.inputStream,
                1024 * 1024
            ).use { input ->

                FileOutputStream(
                    temporary.toFile()
                ).use { output ->

                    input.copyTo(
                        output,
                        1024 * 1024
                    )
                }
            }

            if (sha1 != null) {
                val actual = sha1(temporary)

                check(
                    actual.equals(
                        sha1,
                        ignoreCase = true
                    )
                ) {
                    "SHA-1 mismatch: ${target.fileName}"
                }
            }

            Files.move(
                temporary,
                target,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (e: Exception) {
            Files.deleteIfExists(temporary)
            throw e
        }
    }

    fun downloadAll(
        files: Collection<Download>,
        status: ((String) -> Unit)? = null
    ) {
        if (files.isEmpty()) {
            return
        }

        val futures =
            files.map { file ->
                executor.submit {
                    download(
                        url = file.url,
                        target = file.target,
                        sha1 = file.sha1,
                        status = status
                    )
                }
            }

        try {
            futures.forEach { it.get() }
        } catch (e: Exception) {
            futures.forEach {
                it.cancel(true)
            }

            throw (
                    e.cause
                        ?: e
                    )
        }
    }

    fun shutdown() {
        executor.shutdown()
    }

    private fun open(
        url: String
    ): HttpURLConnection {

        val connection =
            URI(url)
                .toURL()
                .openConnection() as HttpURLConnection

        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 120_000

        connection.setRequestProperty(
            "User-Agent",
            userAgent
        )

        check(
            connection.responseCode in 200..299
        ) {
            "HTTP ${connection.responseCode}: $url"
        }

        return connection
    }

    private fun sha1(
        path: Path
    ): String {

        val digest =
            MessageDigest.getInstance("SHA-1")

        Files.newInputStream(path).use { input ->

            val buffer =
                ByteArray(1024 * 1024)

            while (true) {
                val read =
                    input.read(buffer)

                if (read <= 0) {
                    break
                }

                digest.update(
                    buffer,
                    0,
                    read
                )
            }
        }

        return digest.digest()
            .joinToString("") {
                "%02x".format(it)
            }
    }

    data class Download(
        val url: String,
        val target: Path,
        val sha1: String? = null
    )
}
