package com.rama.simplemenu

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform