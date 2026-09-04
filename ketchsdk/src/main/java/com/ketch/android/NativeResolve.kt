package com.ketch.android

/**
 * Normalizes a `ketchNativeResolve` key: blank or missing is not a valid key to look up.
 */
internal fun parseNativeResolveKey(key: String?): String? =
    key?.trim()?.takeIf { it.isNotEmpty() }

/**
 * Identities that will be sent: [identities] supplied by the host app, plus whatever [lookup]
 * (reading storage) returns for each key the tag has resolved. A key the tag asked about but
 * that resolved to nothing yet is simply omitted, not sent as a blank value. Resolved values
 * win on collision, since they reflect the tag's current state.
 */
internal fun mergeResolvedIdentities(
    identities: Map<String, String>,
    resolvedIdentityKeys: Set<String>,
    lookup: (String) -> String?,
): Map<String, String> {
    val resolved = resolvedIdentityKeys.mapNotNull { key -> lookup(key)?.let { key to it } }.toMap()
    return identities + resolved
}
