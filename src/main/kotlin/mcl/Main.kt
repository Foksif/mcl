package me._lisik.mcl

import java.nio.file.Path
import kotlin.system.exitProcess

fun main() {
    val mcl = Mcl(
        Path.of(
            System.getProperty("user.home"),
            ".mcl"
        )
    )

    val minecraft = mcl.minecraft(
        version = "26.3",
        loader = "0.19.5"
    )

    minecraft.install { message ->
        println("[MCL] $message")
    }

    val process = minecraft.launch(
        user = MinecraftUser(
            username = "_lisik",
            uuid = "00000000-0000-0000-0000-000000000000"
        ),
        output = { line ->
            println("[Minecraft] $line")
        }
    )

    val exitCode = process.waitFor()

    println("[MCL] Minecraft exited with code $exitCode")
    exitProcess(exitCode)
}