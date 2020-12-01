package com.ketch.android.api.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

/**
 * Model for storing data of HTTP GET 'getFullConfiguration' response
 * Implements {@link Cacheable} interface in order to let it be stored by {@link CacheProvider} instance
 */
@Parcelize
data class Configuration(
    @SerializedName("v")
    val version: Int?,
    val language: String?,
    val organization: Organization?,
    @SerializedName("app")
    val applicationInfo: ApplicationInfo?,
    val environments: List<Environment>?,
    val environment: Environment?,
    val deployment: Deployment?,
    val privacyPolicy: PolicyDocument?,
    val termsOfService: PolicyDocument?,
    val rights: List<Right>?,
    val regulations: List<String>?,
    val purposes: List<Purpose>?,
    val policyScope: PolicyScope?,
    val identities: Map<String, Identity>?,
    val scripts: List<String>?,
    val services: Map<String, String>?,
    val options: Map<String, Int>?,
    override val cachedAt: Long?
) : Parcelable, Cacheable {

    override fun cacheableCopy(timestamp: Long): Cacheable =
        copy(cachedAt = timestamp)
}
