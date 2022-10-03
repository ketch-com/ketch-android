package com.ketch.android

class MockResponseFileReader(path: String) {

    val content: String

    init {
        ClassLoader.getSystemResourceAsStream(path).bufferedReader().use { reader ->
            content = reader.readText()
        }
    }
}