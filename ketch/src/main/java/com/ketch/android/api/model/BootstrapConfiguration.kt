package com.ketch.android.api.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

/**
 * Model for storing data of HTTP GET 'getBootstrapConfiguration' response
 * Implements {@link Cacheable} interface in order to let it be stored by {@link CacheProvider} instance
 */
@Parcelize
data class BootstrapConfiguration(
    @SerializedName("v")
    val version: Int?,
    val organization: Organization?,
    @SerializedName("app")
    val applicationInfo: ApplicationInfo?,
    val environments: List<Environment>?,
    val policyScope: PolicyScope?,
    val identities: Map<String, Identity>?,
    val scripts: List<String>?,
    val services: Map<String, String>?,
    val options: Map<String, Int>?,
    override val cachedAt: Long?
) : Parcelable, Cacheable {

    fun getDefaultEnvironment(): Environment? =
        environments?.firstOrNull {
            it.code == DEFAULT_ENVIRONMENT_CODE
        }

    override fun cacheableCopy(timestamp: Long): Cacheable =
        copy(cachedAt = timestamp)

    companion object {
        private const val DEFAULT_ENVIRONMENT_CODE = "production"
    }
}
