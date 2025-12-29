package org.haikm.nativeiosincompose

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform