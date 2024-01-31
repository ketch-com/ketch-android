package com.ketch.sample

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.gson.Gson
import com.ketch.android.Ketch
import com.ketch.android.KetchSdk
import com.ketch.android.data.Consent
import com.ketch.sample.databinding.ActivityMainBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch


class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    private val advertisingId = MutableStateFlow<String?>(null)

    private val languages = arrayOf("EN", "FR")
    private val jurisdictions = arrayOf("default", "gdpr")
    private val regions = arrayOf("US", "FR", "GB")

    private val listener = object : Ketch.Listener {

        override fun onEnvironmentUpdated(environment: String?) {
            Log.d(TAG, "onEnvironmentUpdated: environment = $environment")
        }

        override fun onRegionInfoUpdated(regionInfo: String?) {
            Log.d(TAG, "onRegionInfoUpdated: regionInfo = $regionInfo")
        }

        override fun onJurisdictionUpdated(jurisdiction: String?) {
            Log.d(TAG, "onJurisdictionUpdated: jurisdiction = $jurisdiction")
        }

        override fun onIdentitiesUpdated(identities: String?) {
            Log.d(TAG, "onIdentitiesUpdated: identities = $identities")
        }

        override fun onConsentUpdated(consent: Consent) {
            val consentJson = Gson().toJson(consent)
            Log.d(TAG, "onConsentUpdated: consent = $consentJson")
        }

        override fun onError(errMsg: String?) {
            Log.e(TAG, "onError: errMsg = $errMsg")
        }

        override fun onUSPrivacyUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "onUSPrivacyUpdated: $values")
        }

        override fun onTCFUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "onTCFUpdated: $values")
        }

        override fun onGPPUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "onGPPUpdated: $values")
        }
    }

    private val ketch: Ketch by lazy {
        KetchSdk.create(
            this,
            supportFragmentManager,
            ORG_CODE,
            PROPERTY,
            listener
        ).build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()

        setParameters()

        loadAdvertisingId(binding)

        collectState(advertisingId) {
            with(binding) {
                progressBar.isVisible = false
            }
            it?.let {
                with(ketch) {
                    setIdentities(mapOf(ADVERTISING_ID_CODE to it))
                    load()
                }
            }
        }
    }

    private fun setupUI() {
        with(binding) {
            val languageAdapter: ArrayAdapter<String> =
                ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, languages)
            spLanguage.adapter = languageAdapter

            val jurisdictionAdapter: ArrayAdapter<String> =
                ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, jurisdictions)
            spJurisdiction.adapter = jurisdictionAdapter

            val regionAdapter: ArrayAdapter<String> =
                ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, regions)
            spRegion.adapter = regionAdapter

            buttonSetParameters.setOnClickListener {
                setParameters()
                ketch.load()
            }

            buttonShowPreferences.setOnClickListener {
                ketch.showPreferences()
            }

            buttonShowPreferencesTab.setOnClickListener {
                getPreferencesTab()?.let {
                    ketch.showPreferencesTab(it)
                }
            }

            buttonShowConsent.setOnClickListener {
                ketch.let {
                    it.setBannerWindowPosition(getPosition())
                    it.setModalWindowPosition(getPosition())
                    it.forceShowConsent()
                }
            }

            buttonShowSharedPreferences.setOnClickListener {
                sharedPreferencesString.text = getSharedPreferencesString() ?: ""
            }
        }
    }

    private fun setParameters() {
        with(binding) {
            spLanguage.selectedItemPosition.let {
                ketch.setLanguage(languages[it])
            }
            spJurisdiction.selectedItemPosition.let {
                ketch.setJurisdiction(jurisdictions[it])
            }
            spRegion.selectedItemPosition.let {
                ketch.setRegion(regions[it])
            }
        }
    }

    private fun getPosition(): Ketch.WindowPosition? = when (binding.rgPosition.checkedRadioButtonId) {
        R.id.rbConfig -> null
        R.id.rbTop -> Ketch.WindowPosition.TOP
        R.id.rbBottom -> Ketch.WindowPosition.BOTTOM
        R.id.rbBottomLeft -> Ketch.WindowPosition.BOTTOM_LEFT
        R.id.rbBottomRight -> Ketch.WindowPosition.BOTTOM_RIGHT
        R.id.rbBottomMiddle -> Ketch.WindowPosition.BOTTOM_MIDDLE
        R.id.rbCenter -> Ketch.WindowPosition.CENTER
        else -> Ketch.WindowPosition.BOTTOM_MIDDLE
    }

    private fun getPreferencesTab(): Ketch.PreferencesTab? = when (binding.rgPreferencesTab.checkedRadioButtonId) {
        R.id.rbOverviewTab -> Ketch.PreferencesTab.OVERVIEW
        R.id.rbRightsTab -> Ketch.PreferencesTab.RIGHTS
        R.id.rbConsentsTab -> Ketch.PreferencesTab.CONSENTS
        else -> null
    }

    private fun getSharedPreferencesString(): String? = when (binding.rgSharedPreferences.checkedRadioButtonId) {
        R.id.rbTCF -> ketch.getTCFTCString()
        R.id.rbUSPrivacy -> ketch.getUSPrivacyString()
        R.id.rbGPP -> ketch.getGPPHDRGppString()
        else -> null
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun loadAdvertisingId(binding: ActivityMainBinding) {
        binding.progressBar.isVisible = true
        GlobalScope.launch(Dispatchers.IO) {
            try {
                advertisingId.value =
                    AdvertisingIdClient.getAdvertisingIdInfo(applicationContext).id
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        R.string.cannot_get_advertising_id_toast,
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                launch(Dispatchers.Main) {
                    binding.progressBar.isVisible = false
                }
            }
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        private const val ORG_CODE = "bluebird"
        private const val PROPERTY = "mobile"
        private const val ADVERTISING_ID_CODE = "aaid"
    }
}
