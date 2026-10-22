package com.ramasimple

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform