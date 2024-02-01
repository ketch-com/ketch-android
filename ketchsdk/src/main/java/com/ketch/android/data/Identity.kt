package com.ketch.android.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Identity(val name: String, val value: String) : Parcelable
