package me._lisik.mcl

enum class Platform {
    WINDOWS,
    LINUX,
    MACOS
}

object CurrentPlatform {

    val platform: Platform
        get() {
            val os = System.getProperty("os.name")
                .lowercase()

            return when {
                "win" in os -> Platform.WINDOWS
                "mac" in os || "darwin" in os -> Platform.MACOS
                else -> Platform.LINUX
            }
        }

    val id: String
        get() = when (platform) {
            Platform.WINDOWS -> "windows"
            Platform.LINUX -> "linux"
            Platform.MACOS -> "osx"
        }

    val architecture: String
        get() {
            val arch = System.getProperty("os.arch")
                .lowercase()

            return when (arch) {
                "amd64", "x86_64" -> "x86_64"
                "aarch64", "arm64" -> "arm64"
                else -> arch
            }
        }
}