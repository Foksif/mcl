package me._lisik.mcl

class MinecraftProcess(
    private val process: java.lang.Process
) {
    fun waitFor(): Int =
        process.waitFor()

    fun destroy() {
        process.destroy()
    }

    fun isAlive(): Boolean =
        process.isAlive
}