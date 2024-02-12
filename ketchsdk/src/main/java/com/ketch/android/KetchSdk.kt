package com.ketch.android

import android.content.Context
import androidx.fragment.app.FragmentManager

/**
 * Factory to create the Ketch singleton.
 *
 *         val ketch = KetchSdk.create(
 *             activity,
 *             fragmentManager,
 *             organization = ORGANIZATION,
 *             property = PROPERTY,
 *             listener
 *         )
 **/
object KetchSdk {
    /**
     * Creates the Ketch Builder
     *
     * @param context - an Activity Context to access application assets
     * @param fragmentManager - The FragmentManager this KetchDialogFragment will be added to.
     * @param organization - your organization code
     * @param property - the property name
     * @param listener - Ketch.Listener
     * @param override url
     */
    fun create(
        context: Context,
        fragmentManager: FragmentManager,
        organization: String,
        property: String,
        listener: Ketch.Listener,
        url: String? = null
    ): Ketch.Builder {
        return Ketch.Builder.create(
            context = context,
            fragmentManager = fragmentManager,
            orgCode = organization,
            property = property,
            listener = listener,
            url = url
        )
    }
}