.PHONY: help run-standard run-compose run-integration-test-app test-integration test-integration-class

help: ## Show available commands
	@awk 'BEGIN {FS = ":.*?## "}; /^[a-zA-Z0-9_-]+:.*?## / {printf "  \033[36m%-24s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

run-standard: ## Install, launch, and stream logs for sample-app-standard
	@./scripts/run-sample.sh standard

run-compose: ## Install, launch, and stream logs for sample-app-compose
	@./scripts/run-sample.sh compose

run-integration-test-app: ## Install, launch, and stream logs for integration-tests harness
	@./scripts/run-sample.sh integration

test-integration: ## Run Espresso suite on a connected device/emulator
	@./gradlew :integration-tests:connectedDebugAndroidTest

test-integration-class: ## Run one test class (CLASS=com.ketch.android.integration.tests.ZCrossActivityShowTest)
	@test -n "$(CLASS)" || { echo "error: CLASS is required, e.g. make test-integration-class CLASS=com.ketch.android.integration.tests.KetchSdkIntegrationTest"; exit 1; }
	@./gradlew :integration-tests:connectedDebugAndroidTest \
		-Pandroid.testInstrumentationRunnerArguments.class="$(CLASS)"
