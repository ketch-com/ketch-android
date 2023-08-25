package com.ketch.android.ui.theme

import com.ketch.android.api.response.Theme

/**
 * Represents the current color theme
 */
data class ColorTheme(
    val headerBackgroundColor: String?,
    val headerTextColor: String?,

    val bodyBackgroundColor: String?,
    val bodyTextColor: String?,
    val bodyLinkColor: String?,

    val switchOffColor: String?,
    val switchOnColor: String?,

    val buttonBorderRadius: Int,

    val firstButtonBackgroundColor: String?,
    val firstButtonBorderColor: String?,
    val firstButtonTextColor: String?,

    val secondButtonBackgroundColor: String?,
    val secondButtonBorderColor: String?,
    val secondButtonTextColor: String?,

    val rejectAllButtonBackgroundColor: String?,
    val rejectAllButtonBorderColor: String?,
    val rejectAllButtonTextColor: String?,

    val acceptAllButtonBackgroundColor: String?,
    val acceptAllButtonBorderColor: String?,
    val acceptAllButtonTextColor: String?,
) {
    companion object {
        fun bannerColorTheme(theme: Theme): ColorTheme =
            ColorTheme(
                headerBackgroundColor = theme.bannerBackgroundColor,
                headerTextColor = theme.bannerContentColor,

                bodyBackgroundColor = theme.bannerBackgroundColor,
                bodyTextColor = theme.bannerContentColor,
                bodyLinkColor = theme.bannerButtonColor,

                switchOffColor = null,
                switchOnColor = null,

                buttonBorderRadius = theme.buttonBorderRadius,

                firstButtonBackgroundColor = theme.bannerButtonColor,
                firstButtonBorderColor = theme.bannerButtonColor,
                firstButtonTextColor = theme.bannerBackgroundColor,

                secondButtonBackgroundColor = if (theme.bannerSecondaryButtonVariant == "outlined") theme.bannerBackgroundColor else theme.bannerSecondaryButtonColor,
                secondButtonBorderColor = if (theme.bannerSecondaryButtonVariant == "outlined") theme.bannerSecondaryButtonColor else theme.bannerButtonColor,
                secondButtonTextColor = if (theme.bannerSecondaryButtonVariant == "outlined") theme.bannerSecondaryButtonColor else theme.bannerButtonColor,

                rejectAllButtonBackgroundColor = null,
                rejectAllButtonBorderColor = null,
                rejectAllButtonTextColor = null,

                acceptAllButtonBackgroundColor = null,
                acceptAllButtonBorderColor = null,
                acceptAllButtonTextColor = null
            )

        fun modalColorTheme(theme: Theme): ColorTheme =
            ColorTheme(
                headerBackgroundColor = theme.modalHeaderBackgroundColor,
                headerTextColor = theme.modalHeaderContentColor,

                bodyBackgroundColor = "#ffffff",
                bodyTextColor = theme.modalContentColor,
                bodyLinkColor = theme.modalContentColor,

                switchOffColor = theme.modalSwitchOffColor ?: "#7C868D",
                switchOnColor = theme.modalSwitchOnColor ?: theme.modalContentColor,

                buttonBorderRadius = theme.buttonBorderRadius,

                firstButtonBackgroundColor = theme.modalButtonColor,
                firstButtonBorderColor = theme.modalButtonColor,
                firstButtonTextColor = theme.modalHeaderBackgroundColor,

                secondButtonBackgroundColor = theme.modalHeaderBackgroundColor,
                secondButtonBorderColor = theme.modalButtonColor,
                secondButtonTextColor = theme.modalButtonColor,

                rejectAllButtonBackgroundColor = "#F2F2F7FF",
                rejectAllButtonBorderColor = "#F2F2F7FF",
                rejectAllButtonTextColor = theme.modalContentColor,

                acceptAllButtonBackgroundColor = if (theme.purposeButtonsLookIdentical != null && theme.purposeButtonsLookIdentical == true) "#F2F2F7FF" else "#ffffff",
                acceptAllButtonBorderColor = if (theme.purposeButtonsLookIdentical != null && theme.purposeButtonsLookIdentical == true) "#F2F2F7FF" else theme.modalContentColor,
                acceptAllButtonTextColor = theme.modalContentColor
            )

        fun preferenceColorTheme(theme: Theme): ColorTheme =
            ColorTheme(
                headerBackgroundColor = theme.formHeaderBackgroundColor,
                headerTextColor = theme.formHeaderContentColor ?: "#ffffff",

                bodyBackgroundColor = "#ffffff",
                bodyTextColor = theme.formContentColor,
                bodyLinkColor = theme.formContentColor,

                switchOffColor = theme.formSwitchOffColor ?: "#7C868D",
                switchOnColor = theme.formSwitchOnColor ?: theme.formContentColor,

                buttonBorderRadius = theme.buttonBorderRadius,

                firstButtonBackgroundColor = theme.formButtonColor,
                firstButtonBorderColor = theme.formButtonColor,
                firstButtonTextColor = "#ffffff",

                secondButtonBackgroundColor = "#ffffff",
                secondButtonBorderColor = theme.formButtonColor,
                secondButtonTextColor = theme.formButtonColor,

                rejectAllButtonBackgroundColor = "#F2F2F7FF",
                rejectAllButtonBorderColor = "#F2F2F7FF",
                rejectAllButtonTextColor = theme.modalContentColor,

                acceptAllButtonBackgroundColor = "#ffffff",
                acceptAllButtonBorderColor = theme.modalContentColor,
                acceptAllButtonTextColor = theme.modalContentColor
            )
    }
}