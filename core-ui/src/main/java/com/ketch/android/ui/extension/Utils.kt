package com.ketch.android.ui.extension

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ketch.android.ui.R

fun Context.poweredByKetch() {
    openExternalLink(getString(R.string.powered_by_ketch))
}

fun Context.openExternalLink(link: String) {
    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
    startActivity(browserIntent)
}

