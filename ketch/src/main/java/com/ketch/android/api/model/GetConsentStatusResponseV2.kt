package com.ketch.android.api.model

import android.os.Parcelable
import com.ketch.android.model.Consent
import kotlinx.android.parcel.Parcelize

/**
 * Model for storing data of HTTP POST 'GetConsentStatus' response
 * Implements {@link Cacheable} interface in order to let it be stored by {@link CacheProvider} instance
 */
@Parcelize
data class GetConsentStatusResponseV2(
    val consents: List<Consent>,
    override val cachedAt: Long? = null
) : Parcelable, Cacheable {

    companion object {
        const val version = 1
    }

    /**
     * Return a copy of {@link GetConsentStatusResponse} with provided timestamp set
     */
    override fun cacheableCopy(timestamp: Long): Cacheable =
        copy(cachedAt = timestamp)
}
