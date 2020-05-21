package com.github.mmokeev.fsync.utils

import java.io.*
import java.nio.charset.*
import java.nio.file.*
import java.nio.file.attribute.*
import java.util.*

fun File.getId(): String? {
    val path = this.toPath()
    val fileAttributeView = Files.getFileAttributeView(
        path,
        UserDefinedFileAttributeView::class.java
    )
    return try {
        if (fileAttributeView.list().contains(USER_ID)) {
            val b = Files.getAttribute(path, USER_ID_ATTRIBUTE) as ByteArray
            String(b, Charset.forName("UTF-8"))
        } else {
            null
        }
    } catch (e: IOException) {
        null
    }
}

fun File.setId(): String? {
    val id = UUID.randomUUID().toString()
    return try {
        Files.setAttribute(this.toPath(), USER_ID_ATTRIBUTE, id.toByteArray(Charsets.UTF_8))
        id
    } catch (e: IOException) {
        null
    }
}