package me._lisik.mcl

data class MinecraftUser(
    val username: String,
    val uuid: String,
    val accessToken: String = "0",
    val userType: String = "legacy"
)