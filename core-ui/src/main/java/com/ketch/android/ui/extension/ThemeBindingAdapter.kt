package com.ketch.android.ui.extension

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.databinding.BindingAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import com.ketch.android.ui.R

@ColorInt
private fun getColor(hex: String): Int = try {
    Color.parseColor(hex)
} catch (ex: IllegalArgumentException) {
    android.R.color.transparent
}

@BindingAdapter("backgroundColor")
fun setBackgroundColor(view: View, color: String?) {
    color?.let {
        view.setBackgroundColor(getColor(it))
    }
}

@BindingAdapter("textColor")
fun setTextColor(view: TextView, color: String?) {
    color?.let {
        view.setTextColor(getColor(it))
    }
}

@BindingAdapter("textColorLink")
fun setTextColorLink(view: TextView, color: String?) {
    color?.let {
        view.setLinkTextColor(getColor(it))
    }
}

@BindingAdapter("drawableTint")
fun setDrawableTint(view: ImageView, color: String?) {
    color?.let {
        val colorFilter = PorterDuffColorFilter(getColor(it), PorterDuff.Mode.SRC_ATOP)
        view.drawable.colorFilter = colorFilter
    }
}

@BindingAdapter("strokeColor")
fun setStrokeColor(button: MaterialButton, color: String?) {
    color?.let {
        val states = arrayOf(intArrayOf())

        val colors = intArrayOf(
            getColor(it)
        )

        button.strokeColor = ColorStateList(states, colors)
    }
}

@BindingAdapter("borderRadius")
fun setBorderRadius(button: MaterialButton, radius: Int?) {
    radius?.let {
        button.cornerRadius = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            it.toFloat(),
            button.context.resources.displayMetrics
        ).toInt()
    }
}

@BindingAdapter("iconTint")
fun setIconTint(view: MaterialCheckBox, color: String?) {
    color?.let {
        val states = arrayOf(intArrayOf())

        val colors = intArrayOf(
            getColor(it)
        )

        view.buttonTintList = ColorStateList(states, colors)
    }
}

@BindingAdapter(value = ["trackTintOn", "trackTintOff"], requireAll = true)
fun setTrackTint(view: SwitchMaterial, trackTintOn: String?, trackTintOff: String?) {
    if (trackTintOn != null && trackTintOff != null) {
        val states = arrayOf(
            intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked),
            intArrayOf(android.R.attr.state_enabled, -android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_enabled, android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_enabled, -android.R.attr.state_checked),
        )

        val colorThumbTintOn = getColor(trackTintOn)
        val colorThumbTintOff = getColor(trackTintOff)
        val colorThumbTintDisabled = R.color.disabled

        val colors = intArrayOf(
            colorThumbTintOn,
            colorThumbTintOff,
            colorThumbTintDisabled,
            colorThumbTintDisabled
        )

        view.trackTintList = ColorStateList(states, colors)
    }
}

@SuppressLint("UseCompatTextViewDrawableApis")
@BindingAdapter("iconTint")
fun setIconTint(view: TextView, color: String?) {
    color?.let {
        val states = arrayOf(intArrayOf())

        val colors = intArrayOf(
            getColor(it)
        )

        view.compoundDrawableTintList = ColorStateList(states, colors)
    }
}

@BindingAdapter("tabIndicatorColor")
fun setTabIndicatorColor(view: TabLayout, color: String?) {
    color?.let {
        view.setSelectedTabIndicatorColor(getColor(it))
    }
}

@BindingAdapter("tabTextColor")
fun setTabTextColor(view: TabLayout, color: String?) {
    color?.let {
        val selectedColor = getColor(it)
        val normalColor =
            Color.argb(154, Color.red(selectedColor), Color.blue(selectedColor), Color.green(selectedColor))
        view.setTabTextColors(normalColor, selectedColor)
    }
}

@BindingAdapter("buttonTint")
fun setButtonTint(view: RadioButton, color: String?) {
    color?.let {
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf()
        )

        val selectedColor = getColor(it)
        val normalColor =
            Color.argb(154, Color.red(selectedColor), Color.blue(selectedColor), Color.green(selectedColor))

        val colors = intArrayOf(
            selectedColor,
            normalColor
        )

        view.buttonTintList = ColorStateList(states, colors)
    }
}

@BindingAdapter("boxBackgroundColor")
fun setBoxBackgroundColor(view: TextInputLayout, color: String?) {
    color?.let {
        view.boxBackgroundColor = getColor(it)
    }
}

@BindingAdapter("textColorHint")
fun setTextColorHint(view: TextInputLayout, color: String?) {
    color?.let {
        val states = arrayOf(intArrayOf())

        val colors = intArrayOf(
            getColor(it)
        )

        view.hintTextColor = ColorStateList(states, colors)
    }
}
