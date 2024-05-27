package com.ketch.android.data

const val INDEX_HTML =
    "<html>\n" +
        "  <head>\n" +
        "    <style>\n" +
        "      body {\n" +
        "        height: 100dvh;\n" +
        "        width: 100dvw;\n" +
        "        min-height: -webkit-fill-available;\n" +
        "      }\n" +
        "    </style>\n" +
        "    <meta\n" +
        "      name=\"viewport\"\n" +
        "      content=\"width=device-width, height=device-height, initial-scale=1, viewport-fit=cover\"\n" +
        "    />\n" +
        "  </head>\n" +
        "  <body>\n" +
        "    <script>\n" +
        "\n" +
        "      window.semaphore = window.semaphore || [];\n" +
        "      window.ketch = function () {\n" +
        "        window.semaphore.push(arguments);\n" +
        "      };\n" +
        "\n" +
        "      // Simulating events similar to ones coming from lanyard.js\n" +
        "      // TODO: remove this once JS SDK covers all required events\n" +
        "      function emitEvent(event, args) {\n" +
        "        if (\n" +
        "          window.androidListener ||\n" +
        "          (window.webkit && window.webkit.messageHandlers) ||\n" +
        "          (window.ReactNativeWebView && window.ReactNativeWebView.postMessage)\n" +
        "        ) {\n" +
        "          const filteredArgs = [];\n" +
        "          for (const arg of args) {\n" +
        "            if (arg !== this) {\n" +
        "              filteredArgs.push(arg);\n" +
        "            }\n" +
        "          }\n" +
        "          let argument;\n" +
        "          if (\n" +
        "            filteredArgs.length === 1 &&\n" +
        "            typeof filteredArgs[0] === 'string'\n" +
        "          ) {\n" +
        "            argument = filteredArgs[0];\n" +
        "          } else if (filteredArgs.length === 1) {\n" +
        "            argument = JSON.stringify(filteredArgs[0]);\n" +
        "          } else if (filteredArgs.length > 1) {\n" +
        "            argument = JSON.stringify(filteredArgs);\n" +
        "          }\n" +
        "          if (window.androidListener && event in window.androidListener) {\n" +
        "            if (filteredArgs.length === 0) {\n" +
        "              window.androidListener[event]();\n" +
        "            } else {\n" +
        "              window.androidListener[event](argument);\n" +
        "            }\n" +
        "          } else if (\n" +
        "            window.webkit &&\n" +
        "            window.webkit.messageHandlers &&\n" +
        "            event in window.webkit.messageHandlers\n" +
        "          ) {\n" +
        "            window.webkit.messageHandlers[event].postMessage(argument);\n" +
        "          } else if (window.ReactNativeWebView && window.ReactNativeWebView.postMessage) {\n" +
        "            window.ReactNativeWebView.postMessage(JSON.stringify({ event, data: argument }))\n" +
        "          } else {\n" +
        "            console.warn('Can\\'t pass message to native code because \${event} handler is not registered');\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "\n" +
        "      // This is required to detect the moment when Ketch Tag is loaded sucessfully and ready\n" +
        "      // TODO: Remove this once lanyard.js will emit \"onConfigLoaded\" event, to avoid redundant \"config.json\" requests to server\n" +
        "      ketch('getFullConfig', function (config) {\n" +
        "        emitEvent('onConfigLoaded', [config]);\n" +
        "      });\n" +
        "\n" +
        "      // Simulating \"error\" event\n" +
        "      // Capturing all the unhandled crashes of Ketch Tag\n" +
        "      window.addEventListener('error', (event) => {\n" +
        "        const errorMessage = '\${event.message}';\n" +
        "        emitEvent('error', [errorMessage]);\n" +
        "      });\n" +
        "\n" +
        "      // Capturing all the unhandled promise rejections of Ketch Tag\n" +
        "      window.addEventListener('unhandledrejection', (event) => {\n" +
        "        const errorMessage = '\${event.reason.message}';\n" +
        "        emitEvent('error', [errorMessage]);\n" +
        "      });\n" +
        "\n" +
        "      // Capturing all the internal loggin for errors handled by Ketch Tag\n" +
        "      // TODO: Remove this once lanyard.js will emit error events\n" +
        "      ((logger) => {\n" +
        "        var oldErr = logger.error;\n" +
        "        logger.error = (...args) => {\n" +
        "          emitEvent('error', [args.join(' ')]);\n" +
        "          oldErr(...args);\n" +
        "        };\n" +
        "      })(window.console);\n" +
        "\n" +
        "      // A temporary workaround to get banner/modal dimensions on tablets\n" +
        "      // TODO: remove this once there will be a way to get dialogs position from JS SDK\n" +
        "      function getDialogSize() {\n" +
        "        var domElem = document.querySelector(\n" +
        "          '#lanyard_root div[role=\"dialog\"]'\n" +
        "        );\n" +
        "        if (!domElem) {\n" +
        "          return;\n" +
        "        }\n" +
        "        var domRect = domElem.getBoundingClientRect();\n" +
        "        if (domRect) {\n" +
        "          return domRect;\n" +
        "        }\n" +
        "      }\n" +
        "\n" +
        "      function initKetchTag(parameters) {\n" +
        "        console.log('Ketch Tag is initialization started...');\n" +
        "        // Use parameters to set SDK query params here\n" +
        "        const urlParams = new URLSearchParams(parameters);\n" +
        "        window.history.replaceState({}, '', '?' + urlParams.toString());\n" +
        "\n" +
        "        console.log('Ketch Parameters BEFORE:', parameters);\n" +
        "        // Get query parameters\n" +
        "        let params = new URL(document.location).searchParams;\n" +
        "\n" +
        "        console.log('Ketch Parameters AFTER:', params);\n" +
        "        // Get url override from query parameters\n" +
        "        let url =\n" +
        "          params.get('ketch_mobilesdk_url') ||\n" +
        "          'https://global.ketchcdn.com/web/v3';\n" +
        "\n" +
        "        // Get property name from query parameters\n" +
        "        let propertyName = params.get('propertyCode');\n" +
        "\n" +
        "        // Get organization code from query parameters\n" +
        "        let orgCode = params.get('organizationCode');\n" +
        "        console.log('Ketch org data:', orgCode, propertyName, url);\n" +
        "\n" +
        "        if (orgCode && propertyName) {\n" +
        "          var e = document.createElement('script');\n" +
        "          e.type = 'text/javascript';\n" +
        "          e.src = `\${url}/config/\${orgCode}/\${propertyName}/boot.js`;\n" +
        "          e.defer = e.async = !0;\n" +
        "          document.getElementsByTagName('head')[0].appendChild(e);\n" +
        "        }\n" +
        "      }\n" +
        "    </script>\n" +
        "    <script>\n" +
        "      // We put the script inside body, otherwise document.body will be null\n" +
        "      // Trigger taps outside the dialog\n" +
        "      document.body.addEventListener('touchstart', function (e) {\n" +
        "        if (e.target === document.body) {\n" +
        "          emitEvent('tapOutside', [getDialogSize()]);\n" +
        "        }\n" +
        "      });\n" +
        "    </script>\n" +
        "  </body>\n" +
        "</html>"