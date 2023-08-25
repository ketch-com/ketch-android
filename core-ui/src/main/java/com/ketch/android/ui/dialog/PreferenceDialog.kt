package com.ketch.android.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.ketch.android.api.request.User
import com.ketch.android.api.response.Consent
import com.ketch.android.api.response.ConsentsTab
import com.ketch.android.api.response.FullConfiguration
import com.ketch.android.api.response.OverviewTab
import com.ketch.android.api.response.Purpose
import com.ketch.android.api.response.Right
import com.ketch.android.api.response.RightsTab
import com.ketch.android.ui.R
import com.ketch.android.ui.adapter.RightListAdapter
import com.ketch.android.ui.databinding.PreferencesBinding
import com.ketch.android.ui.databinding.PreferencesConsentsBinding
import com.ketch.android.ui.databinding.PreferencesOverviewBinding
import com.ketch.android.ui.databinding.PreferencesRightsBinding
import com.ketch.android.ui.databinding.PreferencesRightsSentBinding
import com.ketch.android.ui.extension.MarkdownUtils
import com.ketch.android.ui.theme.ColorTheme
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

/**
 * Preference Dialog
 *
 * @param context - [android.content.Context]
 * @param configuration - [com.ketch.android.api.response.FullConfiguration]
 * @param consent - [com.ketch.android.api.response.Consent]
 * @param listener - [com.ketch.android.ui.dialog.PreferenceDialog.PreferencesDialogListener]
 */
internal class PreferenceDialog(
    context: Context,
    configuration: FullConfiguration,
    consent: Consent,
    private val listener: PreferencesDialogListener,
) : BaseDialog(context, configuration, consent) {

    private val invokeRightSent = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configuration.experiences?.preference?.let { preference ->
            val binding = PreferencesBinding.inflate(LayoutInflater.from(context))
            val theme = configuration.theme?.let {
                ColorTheme.preferenceColorTheme(it)
            }

            binding.theme = theme

            binding.title.text = preference.title

            binding.tabLayout.apply {
                addTab(newTab().setText(preference.overview.tabName))
                if (preference.consents != null) {
                    addTab(newTab().setText(preference.consents.tabName))
                }
                if (preference.rights != null) {
                    addTab(newTab().setText(preference.rights.tabName))
                }

                addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        binding.overview.root.isVisible = tab?.position == 0
                        binding.consents.root.isVisible = tab?.position == 1
                        binding.rights.root.isVisible = tab?.position == 2 && !invokeRightSent.value
                        binding.invokeRightSent.root.isVisible = tab?.position == 2 && invokeRightSent.value
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab?) {
                    }

                    override fun onTabReselected(tab: TabLayout.Tab?) {
                    }
                })
            }

            collectState(invokeRightSent) {
                binding.rights.root.isVisible = binding.tabLayout.selectedTabPosition == 2 && !it
                if (preference.rights != null) {
                    (binding.rights.rightsList.adapter as RightListAdapter).reset()
                    binding.rights.requestDetails.text = null
                }
                binding.invokeRightSent.root.isVisible =
                    binding.tabLayout.selectedTabPosition == 2 && it
            }

            buildOverviewTab(theme, binding.overview, preference.overview)
            if (preference.consents != null) {
                buildConsentsTab(theme, binding.consents, preference.consents, configuration)
            }
            if (preference.rights != null) {
                buildRightsTab(theme, binding.rights, preference.rights, configuration.rights)
                buildRightSentTab(theme, binding.invokeRightSent)
            }

            setContentView(binding.root)
            setCanceledOnTouchOutside(false)
            setCancelable(true)

            setOnShowListener { listener.onShow(this) }
            setOnDismissListener { listener.onHide(this) }

            binding.poweredByKetch.isVisible = configuration.theme?.watermark ?: true
        }
    }

    private fun buildOverviewTab(theme: ColorTheme?, binding: PreferencesOverviewBinding, overviewTab: OverviewTab) {
        binding.theme = theme
        binding.bodyTitle.text = overviewTab.bodyTitle
        binding.bodyTitle.isVisible = overviewTab.bodyTitle?.isNotEmpty() == true

        MarkdownUtils.markdown(
            context,
            binding.bodyDescription,
            overviewTab.bodyDescription ?: "",
            configuration
        )

        binding.exitSettingsButton.setOnClickListener {
            cancel()
        }
    }

    private fun buildConsentsTab(
        theme: ColorTheme?,
        binding: PreferencesConsentsBinding,
        consentsTab: ConsentsTab,
        configuration: FullConfiguration
    ) {
        binding.theme = theme

        binding.purposes.purposesView.buildUi(
            binding.theme,
            consentsTab.bodyTitle,
            consentsTab.bodyDescription,
            configuration,
            consent
        )

        binding.purposes.purposesView.categoryClickListener = {
            buildDataCategories(binding.theme, binding, it)

            binding.purposes.root.isVisible = false
            binding.categories.root.isVisible = true
        }

        binding.purposes.purposesView.vendorClickListener = {
            buildVendors(binding.theme, binding, it)

            binding.purposes.root.isVisible = false
            binding.vendors.root.isVisible = true
        }

        binding.purposes.firstButton.apply {
            text = consentsTab.buttonText
            isVisible = consentsTab.buttonText.isNotEmpty() == true

            setOnClickListener {
                val items = binding.purposes.purposesView.items.associate {
                    it.purpose.code to it.accepted.toString()
                }

                val purposes: Map<String, String>? = consent.purposes?.map {
                    val accepted = items[it.key] ?: it.value
                    Pair(it.key, accepted)
                }?.toMap()

                consent.purposes = purposes

                listener.onConsentsButtonClick(this@PreferenceDialog, consent)
            }
        }

        binding.purposes.exitSettingsButton.setOnClickListener {
            cancel()
        }
    }

    private fun buildVendors(theme: ColorTheme?, binding: PreferencesConsentsBinding, purpose: Purpose) {
        binding.theme = theme
        binding.vendors.vendorsView.buildUi(theme, purpose.name, purpose.description, configuration, consent)
        binding.vendors.vendorsView.onBackClickListener = {
            consent.vendors = binding.vendors.vendorsView.items.filter {
                it.accepted
            }.map {
                it.vendor.id
            }

            binding.purposes.root.isVisible = true
            binding.vendors.root.isVisible = false
        }
    }

    private fun buildDataCategories(theme: ColorTheme?, binding: PreferencesConsentsBinding, purpose: Purpose) {
        binding.theme = theme
        binding.categories.dataCategoriesView.buildUi(
            theme,
            purpose.name,
            purpose.description,
            purpose.categories,
            configuration
        )
        binding.categories.dataCategoriesView.onBackClickListener = {
            binding.purposes.root.isVisible = true
            binding.categories.root.isVisible = false
        }
    }

    private fun buildRightsTab(
        theme: ColorTheme?,
        binding: PreferencesRightsBinding,
        rightsTab: RightsTab,
        rights: List<Right>?,
    ) {
        binding.theme = theme
        binding.bodyTitle.text = rightsTab.bodyTitle
        binding.bodyTitle.isVisible = rightsTab.bodyTitle?.isNotEmpty() == true

        MarkdownUtils.markdown(
            context,
            binding.bodyDescription,
            rightsTab.bodyDescription ?: "",
            configuration
        )

        val adapter = RightListAdapter(binding.theme)
        val items: List<Right> = rights ?: emptyList()
        adapter.submitList(items)

        binding.rightsList.apply {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }

        val countries = Locale.getISOCountries().map {
            Locale("", it)
        }.associate {
            Pair(it.isO3Country, it.displayCountry.trim())
        }

        val countryAdapter = ArrayAdapter(context, R.layout.dropdown_item, countries.values.sorted())
        binding.country.apply {
            setAdapter(countryAdapter)
            setText(countries[Locale.getDefault().isO3Country])
            countryAdapter.filter.filter(null)
            setOnItemClickListener { _, _, _, _ ->
                binding.country.error = null
            }
        }

        binding.invokeRightButton.apply {
            text = rightsTab.buttonText
            isVisible = rightsTab.buttonText.isNotEmpty() == true

            setOnClickListener { view ->
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
                if (validateFields(binding)) {
                    adapter.selectedItem?.let { selectedRight ->
                        val user = User(
                            email = binding.email.text.toString(),
                            first = binding.firstName.text.toString(),
                            last = binding.lastName.text.toString(),
                            country = binding.country.text.toString(),
                            stateRegion = null,
                            description = binding.requestDetails.text.toString(),
                            phone = binding.phone.text.toString(),
                            postalCode = binding.postalCode.text.toString(),
                            addressLine1 = binding.addressLine1.text.toString(),
                            addressLine2 = binding.addressLine2.text.toString(),
                        )
                        listener.onRightButtonClick(this@PreferenceDialog, selectedRight, user)
                        invokeRightSent.value = true
                    }
                }
            }
        }

        binding.exitSettingsButton.setOnClickListener {
            cancel()
        }
    }

    private fun buildRightSentTab(theme: ColorTheme?, binding: PreferencesRightsSentBinding) {
        binding.theme = theme

        binding.exitSettingsButton.setOnClickListener {
            cancel()
        }

        binding.newRequestButton.setOnClickListener {
            invokeRightSent.value = false
        }
    }

    private fun validateFields(binding: PreferencesRightsBinding): Boolean {
        var noError = true
        if (binding.requestDetails.text.isNullOrBlank()) {
            binding.requestDetails.error = context.getString(R.string.error_field_required)
            noError = false
        }

        if (binding.firstName.text.isNullOrBlank()) {
            binding.firstName.error = context.getString(R.string.error_field_required)
            noError = false
        }

        if (binding.lastName.text.isNullOrBlank()) {
            binding.lastName.error = context.getString(R.string.error_field_required)
            noError = false
        }

        if (binding.email.text.isNullOrBlank()) {
            binding.email.error = context.getString(R.string.error_field_required)
            noError = false
        } else if (!emailRegex.matches(binding.email.text.toString())) {
            binding.email.error = context.getString(R.string.error_invalid_email)
            noError = false
        }

        if (binding.country.text.isNullOrBlank()) {
            binding.country.error = context.getString(R.string.error_field_required)
            noError = false
        }

        return noError
    }

    override fun onStart() {
        super.onStart()
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    interface PreferencesDialogListener {
        fun onShow(preferenceDialog: PreferenceDialog)
        fun onHide(preferenceDialog: PreferenceDialog)
        fun onConsentsButtonClick(preferenceDialog: PreferenceDialog, consent: Consent)
        fun onRightButtonClick(preferenceDialog: PreferenceDialog, right: Right, user: User)
        fun showModal(preferenceDialog: PreferenceDialog)
    }

    companion object {
        // Email must match correct format "example@email.com"
        private val emailRegex = Regex(
            "(?:[A-Za-z0-9!#\$%&'*+/=?^_`{|}~-]+" +
                    "(?:\\.[A-Za-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"" +
                    "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-" +
                    "\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")" +
                    "@(?:(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])" +
                    "?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.)" +
                    "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[A-Za-z0-9-]*[A-Za-z0-9]:" +
                    "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"
        )
    }
}