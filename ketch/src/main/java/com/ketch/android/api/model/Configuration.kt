package com.ketch.android.api.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Model for storing data of HTTP GET 'getFullConfiguration' response
 * Implements {@link Cacheable} interface in order to let it be stored by {@link CacheProvider} instance
 */
@Parcelize
data class Configuration(
    val applicationInfo: ApplicationInfo?,
    val language: String?,
    val organization: Organization?,
    val environment: Environment?,
    val deployment: Deployment?,
    val privacyPolicy: PolicyDocument?,
    val termsOfService: PolicyDocument?,
    val rights: List<Right>?,
    val regulations: List<String>?,
    val purposes: List<ConfigPurpose>?,
    val policyScope: PolicyScope?,
    val identities: Map<String, IdentityType>?,
    val services: Map<String, String>?,
    val options: Map<String, Int>?,
    override val cachedAt: Long?
) : Parcelable, Cacheable {

    override fun cacheableCopy(timestamp: Long): Cacheable =
        copy(cachedAt = timestamp)

    companion object {
        const val version = 1
    }
}
