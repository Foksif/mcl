package me._lisik.mcl

import me._lisik.mcl.loader.FabricLoader
import kotlin.test.Test
import kotlin.test.assertEquals

class MclTest {

    @Test
    fun createsMinecraft() {
        val minecraft = Mcl.minecraft(
            version = "26.3",
            loader = FabricLoader("0.19.5")
        )

        assertEquals("26.3", minecraft.version.id)
        assertEquals("0.19.5", minecraft.loader.id)
        assertEquals("26.3-0.19.5", minecraft.id)
    }
}