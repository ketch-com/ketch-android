package com.ketch.android.data

import com.google.gson.Gson

/*
* https://global.ketchcdn.com/web/v3//config/ketch_samples/android/boot.js?ketch_log=DEBUG
*       &ketch_lang=en&ketch_jurisdiction=default&ketch_region=US
*       &ketch_show=preferences&ketch_preferences_tabs=overviewTab,rightsTab,consentsTab,subscriptionsTab
 */
fun getIndexHtml(
    orgCode: String,
    propertyName: String,
    logLevel: String,
    ketchMobileSdkUrl: String,
    language: String? = null,
    jurisdiction: String? = null,
    identities: Map<String, String> = emptyMap(),
    region: String? = null,
    environment: String? = null,
    forceShow: String? = null,
    preferencesTabs: String? = null,
    preferencesTab: String? = null,
    age: Int? = null,
    ageLower: Int? = null,
    ageUpper: Int? = null,
    bottomPadding: String = "0px",
    topPadding: String = "0px",
    cssStyleOverride: String? = null,
    webResourceUrlOverrides: Map<String, String> = emptyMap(),
): String {
    val paramsJson = buildInitParamsJson(
        orgCode = orgCode,
        propertyName = propertyName,
        ketchMobileSdkUrl = ketchMobileSdkUrl,
        logLevel = logLevel,
        language = language,
        jurisdiction = jurisdiction,
        identities = identities,
        region = region,
        environment = environment,
        forceShow = forceShow,
        preferencesTabs = preferencesTabs,
        preferencesTab = preferencesTab,
        age = age,
        ageLower = ageLower,
        ageUpper = ageUpper,
        webResourceUrlOverrides = webResourceUrlOverrides,
    )

    return """
<html>
  <head>
    <style>
      body {
        height: 100dvh;
        width: 100dvw;
        min-height: -webkit-fill-available;
        --safe-area-inset-bottom: $bottomPadding;
        --safe-area-inset-top: $topPadding;
      }
      [role="dialog"], .ketch-experience, #ketch-root, [data-ketch-root] {
        display: block !important;
        visibility: visible !important;
        opacity: 1 !important;
        z-index: 2147483647 !important;
      }
    </style>
    ${cssStyleOverride?.takeIf { it.isNotBlank() }?.let { "<style>$it</style>" } ?: ""}
    <meta
      name="viewport"
      content="width=device-width, height=device-height, initial-scale=1, viewport-fit=cover"
    />
  </head>
  <body>
    <script>
      (function () {
        function emitEvent(event, args) {
          if (
            window.androidListener ||
            (window.webkit && window.webkit.messageHandlers) ||
            (window.ReactNativeWebView && window.ReactNativeWebView.postMessage)
          ) {
            const filteredArgs = [];
            for (const arg of args) {
              if (arg !== this) filteredArgs.push(arg);
            }
            let argument;
            if (filteredArgs.length === 1 && typeof filteredArgs[0] === 'string') {
              argument = filteredArgs[0];
            } else if (filteredArgs.length === 1) {
              argument = JSON.stringify(filteredArgs[0]);
            } else if (filteredArgs.length > 1) {
              argument = JSON.stringify(filteredArgs);
            }
            if (window.androidListener && event in window.androidListener) {
              if (filteredArgs.length === 0) {
                window.androidListener[event]();
              } else {
                window.androidListener[event](argument);
              }
            } else if (
              window.webkit &&
              window.webkit.messageHandlers &&
              event in window.webkit.messageHandlers
            ) {
              window.webkit.messageHandlers[event].postMessage(argument);
            } else if (window.ReactNativeWebView && window.ReactNativeWebView.postMessage) {
              window.ReactNativeWebView.postMessage(JSON.stringify({ event, data: argument }));
            }
          }
        }

        window.addEventListener('error', function (event) {
          emitEvent('error', [String(event.message || 'Unknown error')]);
        });
        window.addEventListener('unhandledrejection', function (event) {
          var reason = event.reason && event.reason.message ? event.reason.message : 'Unhandled rejection';
          emitEvent('error', [String(reason)]);
        });

        window.semaphore = window.semaphore || [];
        window.ketch = function () {
          window.semaphore.push(arguments);
        };
        window.__noop = function () {};

        var params = $paramsJson;
        window.__ketchParams = params;

        ${webResourceUrlOverridesInstallScriptBody()}

        function __ketchApplyUrlFromParams(p) {
          try {
            var urlParams = {
              organizationCode: p.organizationCode,
              propertyCode: p.propertyCode,
              ketch_lang: p.languageCode || null,
              ketch_log: p.logLevel || null,
              ketch_mobilesdk_url: p.ketch_mobilesdk_url || null,
              ketch_env: p.environmentName || null,
              ketch_jurisdiction: p.jurisdictionCode || null,
              ketch_region: p.regionCode || null,
              ketch_show: p.ketch_show || null,
              ketch_preferences_tabs: p.ketch_preferences_tabs || null,
              ketch_preferences_tab: p.ketch_preferences_tab || null,
              ketch_age: p.age != null && p.age >= 0 ? String(p.age) : null,
              ketch_age_lower: p.ageLower != null && p.ageLower >= 0 ? String(p.ageLower) : null,
              ketch_age_upper: p.ageUpper != null && p.ageUpper >= 0 ? String(p.ageUpper) : null,
            };
            if (p.identities && typeof p.identities === 'object') {
              Object.keys(p.identities).forEach(function (k) {
                urlParams[k] = p.identities[k];
              });
            }
            Object.keys(urlParams).forEach(function (k) {
              if (urlParams[k] == null || urlParams[k] === '') delete urlParams[k];
            });
            var qs = new URLSearchParams(urlParams).toString();
            history.replaceState({}, '', (location.pathname || '/') + (qs ? ('?' + qs) : '') + (location.hash || ''));
          } catch (e) {
            console.error('applyUrl error', e);
          }
        }

        var WEB_BASE = params.ketch_mobilesdk_url || 'https://global.ketchcdn.com/web/v3';

        function loadScript(src) {
          return new Promise(function (resolve, reject) {
            try {
              var s = document.createElement('script');
              s.src = src;
              s.async = true;
              s.defer = true;
              s.onload = resolve;
              s.onerror = reject;
              document.head.appendChild(s);
            } catch (e) {
              reject(e);
            }
          });
        }

        function loadBoot() {
          return loadScript(
            WEB_BASE + '/config/' + params.organizationCode + '/' + params.propertyCode + '/boot.js'
          );
        }

        function bindEventListeners() {
          try {
            window.ketch('on', 'willShowExperience', function (p) {
              emitEvent('willShowExperience', [p]);
            });
            window.ketch('on', 'hasShownExperience', function () {
              emitEvent('hasShownExperience', ['1']);
            });
            window.ketch('on', 'hideExperience', function (p) {
              emitEvent('hideExperience', [p]);
            });
            window.ketch('on', 'environment', function (e) {
              emitEvent('environment', [e]);
            });
            window.ketch('on', 'regionInfo', function (r) {
              emitEvent('regionInfo', [r]);
            });
            window.ketch('on', 'jurisdiction', function (j) {
              emitEvent('jurisdiction', [j]);
            });
            window.ketch('on', 'identities', function (i) {
              emitEvent('identities', [i]);
            });
            window.ketch('on', 'consent', function (c) {
              emitEvent('consent', [c]);
            });
            window.ketch('on', 'usprivacy_updated_data', function (k, a) {
              emitEvent('usprivacy_updated_data', [k, a]);
            });
            window.ketch('on', 'gpp_updated_data', function (k, a) {
              emitEvent('gpp_updated_data', [k, a]);
            });
            window.ketch('on', 'tcf_updated_data', function (k, a) {
              emitEvent('tcf_updated_data', [k, a]);
            });
          } catch (e) {
            console.error('bindEventListeners failed', e);
          }
        }

        function maybeAutoShow() {
          try {
            var showConsent =
              params.forceConsentExperience ||
              params.ketch_show === 'consent' ||
              params.ketch_show === 'cd';
            if (showConsent) {
              try { window.ketch('showConsent'); } catch (_) {
                try { window.ketch('showExperience', 'consent', window.__noop); } catch (_) {
                  try { window.ketch('renderExperience', 'consent', window.__noop); } catch (_) {}
                }
              }
            }
            var showPreferences =
              params.forcePreferenceExperience || params.ketch_show === 'preferences';
            if (showPreferences) {
              try { window.ketch('showPreferences'); } catch (_) {
                try { window.ketch('showExperience', 'preferences', window.__noop); } catch (_) {
                  try { window.ketch('renderExperience', 'preferences', window.__noop); } catch (_) {}
                }
              }
            }
          } catch (e) {
            console.error('autoShow error', e);
          }
        }

        window.ketch('getFullConfig', function (cfg) {
          emitEvent('onConfigLoaded', [cfg]);
          bindEventListeners();
          maybeAutoShow();
        });

        if (params.webResourceUrlOverrides) {
          installWebResourceUrlOverrides(params.webResourceUrlOverrides);
        }

        __ketchApplyUrlFromParams(params);
        loadBoot().catch(function (e) {
          console.error('asset load error (boot)', e && e.message ? e.message : e);
        });

        function getDialogSize() {
          var domElem = document.querySelector('#lanyard_root div[role="dialog"]');
          if (!domElem) return;
          return domElem.getBoundingClientRect();
        }

        function triggerOutsideTapDismiss() {
          var selectors = [
            '#lanyard_root button[aria-label="close banner"]',
            '#lanyard_root button[aria-label="close modal"]',
          ];
          for (var i = 0; i < selectors.length; i++) {
            var btn = document.querySelector(selectors[i]);
            if (btn) {
              btn.click();
              return true;
            }
          }
          return false;
        }

        document.body.addEventListener('touchstart', function (e) {
          if (e.target === document.body) {
            if (!triggerOutsideTapDismiss()) {
              emitEvent('tapOutside', [getDialogSize()]);
            }
          }
        });
      })();
    </script>
  </body>
</html>
""".trimIndent()
}

private fun buildInitParamsJson(
    orgCode: String,
    propertyName: String,
    ketchMobileSdkUrl: String,
    logLevel: String,
    language: String?,
    jurisdiction: String?,
    identities: Map<String, String>,
    region: String?,
    environment: String?,
    forceShow: String?,
    preferencesTabs: String?,
    preferencesTab: String?,
    age: Int?,
    ageLower: Int?,
    ageUpper: Int?,
    webResourceUrlOverrides: Map<String, String>,
): String {
    val params = linkedMapOf<String, Any>(
        "organizationCode" to orgCode,
        "propertyCode" to propertyName,
        "ketch_mobilesdk_url" to ketchMobileSdkUrl,
        "logLevel" to logLevel,
    )
    language?.takeIf { it.isNotBlank() }?.let { params["languageCode"] = it }
    jurisdiction?.takeIf { it.isNotBlank() }?.let { params["jurisdictionCode"] = it }
    region?.takeIf { it.isNotBlank() }?.let { params["regionCode"] = it }
    environment?.takeIf { it.isNotBlank() }?.let { params["environmentName"] = it }
    preferencesTabs?.takeIf { it.isNotBlank() }?.let { params["ketch_preferences_tabs"] = it }
    preferencesTab?.takeIf { it.isNotBlank() }?.let { params["ketch_preferences_tab"] = it }
    if (age != null && age >= 0) params["age"] = age
    if (ageLower != null && ageLower >= 0) params["ageLower"] = ageLower
    if (ageUpper != null && ageUpper >= 0) params["ageUpper"] = ageUpper
    if (identities.isNotEmpty()) params["identities"] = identities
    if (webResourceUrlOverrides.isNotEmpty()) params["webResourceUrlOverrides"] = webResourceUrlOverrides

    when (forceShow?.lowercase()) {
        "consent", "cd" -> {
            params["forceConsentExperience"] = true
            params["ketch_show"] = "consent"
        }
        "preferences" -> {
            params["forcePreferenceExperience"] = true
            params["ketch_show"] = "preferences"
        }
        null, "" -> Unit
        else -> params["ketch_show"] = forceShow
    }

    return Gson().toJson(params)
}

private fun webResourceUrlOverridesInstallScriptBody(): String =
    """
        function installWebResourceUrlOverrides(overrides) {
          if (!overrides || !Object.keys(overrides).length) return;
          function resolveUrl(url) {
            if (!url) return url;
            if (overrides[url]) return overrides[url];
            var base = url.split('?')[0].split('#')[0];
            if (base !== url && overrides[base]) return overrides[base];
            for (var key in overrides) {
              if (!Object.prototype.hasOwnProperty.call(overrides, key)) continue;
              if (key === url || key === base) continue;
              if (key.charAt(0) === '/' && base.indexOf(key) !== -1) return overrides[key];
              if (key.indexOf('://') !== -1) continue;
              if (base.endsWith(key) || base.indexOf('/' + key) !== -1) return overrides[key];
            }
            return url;
          }
          var srcDesc = Object.getOwnPropertyDescriptor(HTMLScriptElement.prototype, 'src');
          if (srcDesc && srcDesc.set) {
            var nativeSrcSet = srcDesc.set;
            var nativeSrcGet = srcDesc.get;
            Object.defineProperty(HTMLScriptElement.prototype, 'src', {
              set: function (value) { nativeSrcSet.call(this, resolveUrl(value)); },
              get: nativeSrcGet,
              configurable: true,
            });
          }
          var origSetAttribute = Element.prototype.setAttribute;
          Element.prototype.setAttribute = function (name, value) {
            if (name === 'src' && this.tagName === 'SCRIPT') {
              return origSetAttribute.call(this, name, resolveUrl(value));
            }
            return origSetAttribute.call(this, name, value);
          };
          if (window.fetch) {
            var origFetch = window.fetch.bind(window);
            window.fetch = function (input, init) {
              if (typeof input === 'string') {
                var mapped = resolveUrl(input);
                if (mapped !== input) input = mapped;
              } else if (input && input.url) {
                var mappedUrl = resolveUrl(input.url);
                if (mappedUrl !== input.url) input = new Request(mappedUrl, input);
              }
              return origFetch(input, init);
            };
          }
        }
    """.trimIndent()
