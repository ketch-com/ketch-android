package com.ketch.android.ccpa

import com.ketch.android.api.response.Banner
import com.ketch.android.api.response.BannerPosition
import com.ketch.android.api.response.CanonicalPurpose
import com.ketch.android.api.response.Consent
import com.ketch.android.api.response.ConsentExperience
import com.ketch.android.api.response.ConsentsTab
import com.ketch.android.api.response.Deployment
import com.ketch.android.api.response.Environment
import com.ketch.android.api.response.Experience
import com.ketch.android.api.response.ExperienceButtonAction
import com.ketch.android.api.response.ExperienceButtonDestination
import com.ketch.android.api.response.ExperienceDefault
import com.ketch.android.api.response.FullConfiguration
import com.ketch.android.api.response.Identity
import com.ketch.android.api.response.Jit
import com.ketch.android.api.response.JurisdictionInfo
import com.ketch.android.api.response.Modal
import com.ketch.android.api.response.ModalPosition
import com.ketch.android.api.response.Organization
import com.ketch.android.api.response.OverviewTab
import com.ketch.android.api.response.PolicyDocument
import com.ketch.android.api.response.PreferenceExperience
import com.ketch.android.api.response.Property
import com.ketch.android.api.response.Purpose
import com.ketch.android.api.response.RightsTab
import com.ketch.android.api.response.Theme
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class CCPATest {

    private val organizationConfig = FullConfiguration(
        language = "en",
        organization = Organization(
            code = "transcenda"
        ),
        property = Property(
            code = "android_prop",
            name = "android_prop",
            platform = "ANDROID"
        ),
        environments = listOf(
            Environment(
                code = "stage",
                hash = "1333812840345508246",
                pattern = null
            )
        ),
        environment = Environment(
            code = "stage",
            hash = "1333812840345508246",
            pattern = null
        ),
        jurisdiction = JurisdictionInfo(
            code = "default",
            defaultJurisdictionCode = "default",
            variable = null,
            scopes = null
        ),
        identities = mapOf(
            Pair(
                "swb_android_prop",
                Identity(
                    type = "managedCookie",
                    variable = "_swb"
                )
            )
        ),
        deployment = Deployment(
            code = "default_deployment_plan",
            version = 1662711181
        ),
        regulations = listOf(
            "ccpaca"
        ),
        rights = null,
        purposes = listOf(
            Purpose(
                canonicalPurposeCode = "essential_services",
                code = "essential_services",
                description = "Collection and processing of personal data to enable functionality that is essential to providing our services, including security activities, debugging, authentication, and fraud prevention, as well as contacting you with information related to products/services you have used or purchased; we may set essential cookies or other trackers for these purposes.",
                legalBasisCode = "disclosure",
                legalBasisDescription = "Data subject has been provided with adequate disclosure regarding the processing",
                legalBasisName = "Disclosure",
                name = "Essential Services",
                requiresDisplay = true,
                requiresPrivacyPolicy = true,
                allowsOptOut = null,
                categories = null,
                cookies = null,
                dataSubjectTypeCodes = null,
                requiresOptIn = true,
                tcfID = "1",
                tcfType = "purpose"
            )
        ),
        canonicalPurposes = mapOf(
            Pair(
                "analytics", CanonicalPurpose(
                    code = "analytics",
                    name = "analytics",
                    purposeCodes = listOf(
                        "analytics",
                        "tcf.purpose_1",
                        "somepurpose_key"
                    )
                )
            ),
            Pair(
                "behavioral_advertising", CanonicalPurpose(
                    code = "behavioral_advertising",
                    name = "behavioral_advertising",
                    purposeCodes = listOf(
                        "behavioral_advertising",
                        "tcf.purpose_1",
                        "somepurpose_key"
                    )
                )
            ),
            Pair(
                "data_broking", CanonicalPurpose(
                    code = "data_broking",
                    name = "data_broking",
                    purposeCodes = listOf(
                        "data_broking",
                        "tcf.purpose_1",
                        "somepurpose_key"
                    )
                )
            )
        ),
        experiences = Experience(
            consentExperience = ConsentExperience(
                banner = Banner(
                    buttonText = "I understand",
                    secondaryButtonText = "Cancel",
                    footerDescription = "Welcome! We’re glad you’re here and want you to know that we respect your privacy and your right to control how we collect, use, and share your personal data.",
                    primaryButtonAction = ExperienceButtonAction.SAVE_CURRENT_STATE,
                    secondaryButtonDestination = ExperienceButtonDestination.PREFERENCE,
                    title = "Your Privacy",
                    extensions = null,
                    showCloseIcon = null
                ),
                code = "default_consent___disclosure",
                experienceDefault = ExperienceDefault.BANNER,
                jit = Jit(
                    acceptButtonText = "Save choices",
                    bodyDescription = "Please indicate whether you consent to our collection and use of your data in order to perform the operation(s) you’ve requested.",
                    declineButtonText = "Cancel",
                    moreInfoDestination = ExperienceButtonDestination.MODAL,
                    title = "Your Privacy",
                    moreInfoText = null,
                    extensions = null,
                    showCloseIcon = null
                ),
                modal = Modal(
                    bodyDescription = "Welcome! We’re glad you're here and want you to know that we respect your privacy and your right to control how we collect, use, and share your personal data. Listed below are the purposes for which we process your data--please indicate whether you consent to such processing.",
                    buttonText = "Save choices",
                    title = "Your Privacy",
                    bodyTitle = null,
                    consentTitle = null,
                    hideConsentTitle = null,
                    hideLegalBases = null,
                    extensions = null,
                    showCloseIcon = null
                ),
                version = 1663598228,
                extensions = null,
            ),
            preference = PreferenceExperience(
                code = "default_preference_management",
                consents = ConsentsTab(
                    bodyDescription = "We collect and use data--including, where applicable, your personal data--for the purposes listed below. Please indicate whether or not that's ok with you by toggling the switches below.",
                    bodyTitle = "Choose how we use your data",
                    buttonText = "Submit",
                    tabName = "Preferences",
                    extensions = null
                ),
                overview = OverviewTab(
                    bodyDescription = "Welcome! We're glad you're here and want you to know that we respect your privacy and your right to control how we collect, use, and store your personal data.",
                    tabName = "Overview",
                    bodyTitle = null,
                    extensions = null
                ),
                rights = RightsTab(
                    bodyDescription = "Applicable privacy laws give you certain rights with respect to our collection, use, and storage of your personal data, and we welcome your exercise of those rights. Please complete the form below so that we can validate and fulfill your request.",
                    bodyTitle = "Exercise your rights",
                    buttonText = "Submit",
                    tabName = "Your Rights",
                    extensions = null
                ),
                title = "Your Privacy",
                version = 1662711180,
            )
        ),
        services = mapOf(
            Pair(
                "lanyard",
                "https://global.ketchcdn.com/transom/route/switchbit/lanyard/transcenda/lanyard.js"
            ),
        ),
        options = mapOf(
            Pair("appDivs", "hubspot-messages-iframe-container")
        ),
        privacyPolicy = PolicyDocument(null, 0, null),
        termsOfService = PolicyDocument(null, 0, null),
        theme = Theme(
            bannerBackgroundColor = "#01090E",
            bannerButtonColor = "#ffffff",
            bannerContentColor = "#ffffff",
            bannerPosition = BannerPosition.BOTTOM,
            buttonBorderRadius = 5,
            code = "default",
            description = "Ketch default theme",
            formButtonColor = "#071a24",
            formContentColor = "#071a24",
            formHeaderBackgroundColor = "#071a24",
            modalButtonColor = "#071a24",
            modalContentColor = "#071a24",
            modalHeaderBackgroundColor = "#f6f6f6",
            modalPosition = ModalPosition.CENTER,
            name = "Default",
            bannerSecondaryButtonColor = null,
            formHeaderContentColor = null,
            modalHeaderContentColor = null,
            watermark = null,
            formSwitchOnColor = null,
            formSwitchOffColor = null,
            modalSwitchOnColor = null,
            modalSwitchOffColor = null,
            purposeButtonsLookIdentical = null
        ),
        scripts = null,
        vendors = null,
        translations = null
    )

    private val consent = Consent(
        purposes = mapOf(
            Pair("analytics", "true"),
            Pair("behavioral_advertising", "true"),
            Pair("data_broking", "true"),
            Pair("email_marketing", "true"),
            Pair("essential_services", "true"),
            Pair("somepurpose_key", "true"),
            Pair("tcf.purpose_1", "true")
        ),
        vendors = null
    )

    @Test
    fun `CCPA Test`() {
        val ccpaString = CCPAString.encode(
            configuration = organizationConfig,
            consent = consent,
            notice = true,
            lspa = true
        )

        assertEquals(CCPA_STRING, ccpaString)
    }

    companion object {
        private const val CCPA_STRING = "1YYY"
    }
}