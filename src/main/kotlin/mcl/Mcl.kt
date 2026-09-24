package me._lisik.mcl

import java.nio.file.Path

class Mcl(
    val home: Path
) {
    fun minecraft(
        version: String,
        loader: String? = null
    ): Minecraft {
        return Minecraft(
            home = home,
            version = version,
            loader = loader
        )
    }
}