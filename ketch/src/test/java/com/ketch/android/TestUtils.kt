package com.ketch.android

import java.io.File
import java.io.InputStreamReader

fun ClassLoader.loadFromFile(fileName: String): String {
    val inputStream = getResourceAsStream(fileName)
    val buffer = CharArray(1024)
    val out = StringBuilder()
    val input = InputStreamReader(inputStream, "UTF-8")

    while (true) {
        val rsz = input.read(buffer, 0, buffer.size)
        if (rsz < 0) {
            break
        }
        out.append(buffer, 0, rsz)
    }

    return out.toString()
}

fun getJson(path: String): String {
    val file = File("src/test/resources/$path")
    return String(file.readBytes())
}
