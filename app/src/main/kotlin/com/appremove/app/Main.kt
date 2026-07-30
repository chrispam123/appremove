package com.appremove.app

import com.appremove.coreimage.CoreImageInfo
import com.appremove.coreml.CoreMlInfo
import com.appremove.data.DataInfo
import com.appremove.domain.DomainInfo

fun main() {
    val modules =
        listOf(
            DomainInfo.MODULE_NAME,
            DataInfo.MODULE_NAME,
            CoreImageInfo.MODULE_NAME,
            CoreMlInfo.MODULE_NAME,
        )
    println("appremove skeleton OK -> modules linked: ${modules.joinToString()}")
}
