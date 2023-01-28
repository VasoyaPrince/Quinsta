package com.example.quinsta.model

data class Data(
    val adsProvider: List<AdsProvider>,
    val banner: Any,
    val description: Any,
    val id: String,
    val installed: Int,
    val logo: String,
    val name: String,
    val opened: Int,
    val packageName: String,
    val platform: String,
    val screenshot: List<Any>
)