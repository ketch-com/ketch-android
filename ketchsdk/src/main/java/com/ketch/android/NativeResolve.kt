package com.ketch.android

/**
 * Normalizes a `ketchNativeResolve` key: blank or missing is not a valid key to look up.
 */
internal fun parseNativeResolveKey(key: String?): String? =
    key?.trim()?.takeIf { it.isNotEmpty() }
