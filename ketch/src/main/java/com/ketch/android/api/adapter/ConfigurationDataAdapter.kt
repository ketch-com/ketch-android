package com.ketch.android.api.adapter

import com.ketch.android.api.model.*
import com.ketch.android.api.model.Configuration
import mobile.MobileOuterClass

object ConfigurationDataAdapter  {
    fun toModel(dto: MobileOuterClass.GetConfigurationResponse): Configuration {
        return Configuration(
            applicationInfo = ApplicationInfo(dto.app.code, dto.app.name, dto.app.platform),
            deployment = Deployment(dto.deployment.code, dto.deployment.version),
            environment = Environment(
                dto.environment.code,
                dto.environment.pattern,
                dto.environment.hash
            ),
            identities = dto.identitiesMap.mapValues { v ->
                IdentityType(
                    v.value.type,
                    v.value.variable
                )
            },
            language = dto.language,
            options = dto.optionsMap,
            organization = Organization(dto.organization.name, dto.organization.code),
            policyScope = PolicyScope(
                dto.policyScope.defaultScopeCode,
                dto.policyScope.scopesMap,
                dto.policyScope.code
            ),
            privacyPolicy = PolicyDocument(
                dto.privacyPolicy.code,
                dto.privacyPolicy.version,
                dto.privacyPolicy.url
            ),
            purposes = dto.purposesList.map {
                ConfigPurpose(
                    it.code,
                    it.name,
                    it.description,
                    it.legalBasisCode,
                    it.requiresPrivacyPolicy,
                    it.requiresOptIn,
                    it.allowsOptOut
                )
            },
            regulations = dto.regulationsList,
            rights = dto.rightsList.map { Right(it.code, it.name, it.description) },
            services = dto.servicesMap,
            termsOfService = PolicyDocument(dto.termsOfService.code, dto.termsOfService.version, dto.termsOfService.url),
            cachedAt = null
        )
    }

}