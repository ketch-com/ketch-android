# ketch
Mobile SDK for Android

## Install SDK

### Maven/Gradle dependency

1. Open root project `build.gradle`
2. Add new repository:
```groovy
allprojects {
    repositories {
        ...
        maven {
            url "https://b10s.jfrog.io/artifactory/ketch/"
            credentials {
                username = <repo_username>
                password = <repo_password>
            }
        }
        ...
    }
}
```
3. Open module's `build.gradle` file
4. Update dependencies:
```groovy
dependencies {
    ...
    implementation 'com.ketch.android:ketch:<version>@aar'
    ...
}
```
5. Sync gradle

### Manual

The manual install is still possible, you need to perform next steps:
1. Just put SDK AAR file into` <project>/<module>/libs`
2. Open module's `build.gradle` file
3. Update dependencies:
```groovy
dependencies {
    ...
    implementation fileTree(dir: 'libs', include: ['*.aar'])
    ...
}
```
4. (optional) Sync gradle

## Setup

In order to use Ketch resources, `KetchRepository` should be properly initialized and setup. `KetchRepository.Builder` class should be used for that puprose

- Method `organizationCode(String)`: The code of organization (required)
- Method `applicationCode(String)`: The code of application (required)
- Method `context(Context)`: android context
- Method `cacheProvider(CacheProvider)`: instance of `CacheProvider` interface implementation
- Throws: `IllegalStateException` in case if required parameters are missing

```kotlin
KetchRepository.Builder()
    .organizationCode("foo")
    .applicationCode("bar")
    .context(context)
    .cacheProvider(SharedPreferencesCacheProvider(context))
    .build()
```


## Requests

The next methods send requests to the back-end


### Get Configuration
Retrieves configuration data.
Result will be cached for each unique set of  of `organizationCode`, `applicationCode`, `environment`, `scope` and `languageCode`.
- Parameter `environment`: environment value that should match one of environment patterns
- Parameter `countryCode`: current country code (e.g. US)
- Parameter `languageCode`: current language code (e.g. en)
- Parameter `regionCode`: current region code (e.g. CA)
- Parameter `IP`: current IP address
- Returns:  `Flow` of `Result` with `Configuration` if successful and with an error if request or its handling failed

```kotlin
fun getConfiguration(
        environment: String,
        countryCode: String,
        languageCode: String,
        regionCode: String = "",
        IP: String = ""
): Flow<Result<RequestError, Configuration>>
```
```kotlin
job = CoroutineScope(Dispatchers.Main).launch {
    repository.getFullConfiguration(
        configuration = bootstrapConfiguration,
        environment = <environmentUrl>,
        countryCode = <countryCode>,
        languageCode = <languageCode>,
        regionCode = <regionCode>,
        IP = <currentLocationIP>,
    )
        .collect { result ->
            when (result) {
                is Result.Success -> // hande success
                is Result.Error -> // hande error
            }
        }
}
job.cancel() //job could be cancelled
```


### Get Consent

Retrieves currently set consent status.
Uses `organizationCode` to form a full URL.
Is executed on IO thread.
Result will be cached for each unique set of  of `organizationCode`, `applicationCode`, `environment`, `identities` and `purposes`.
- Parameter `configuration`: full configuration
- Parameter `identities`: collection of `identityCodes` and `identityValues`. Keys and values shouldn't be null
- Parameter `purposes`: collection of activity `names` and names of `legalBasisCode` of this activity. Keys and values shouldn't be null
- Returns: `Flow` of `Result` with `Map<String, ConsentStatus>>` if successful and with an error if request or its handling failed

```kotlin
fun getConsent(
        configuration: Configuration,
        identities: Iterable<IdentitySpace>,
        purposes: Iterable<Purpose>
): Flow<Result<RequestError, Map<String, ConsentStatus>>>
```
```kotlin
job = CoroutineScope(Dispatchers.Main).launch {
    repository.getConsentStatus(
        configuration = configuration,
        identities = <identitiesList>,
        purposes = <purposesList>
    )
        .collect { result ->
            when (result) {
                is Result.Success -> // hande success
                is Result.Error -> // hande error
            }
        }
}
job.cancel() //job could be cancelled
```

### Set Consent

Sends a request for updating consent status.
Uses `organizationCode` to form a full URL.
Is executed on IO thread.
- Parameter `configuration`: configuration
- Parameter `identities`: map of `identityCodes` and `identityValues`. Keys and values shouldn't be null
- Parameter `purposes`: map of consent names and information if this particular legalBasisCode should be allowed or not. Keys and values shouldn't be null
- Returns: `Flow` of `Result.Success` with `<Long>` receivedTime if successful and with an error if request or its handling failed

```kotlin
fun setConsent(
        configuration: Configuration,
        identities: Iterable<IdentitySpace>,
        purposes: Iterable<Purpose>
    ): Flow<Result<RequestError, Long>>
```
```kotlin
job = CoroutineScope(Dispatchers.Main).launch {
    repository.updateConsentStatus(
        configuration = configuration,
        identities = <identitiesMap>,
        consents = <consentsMap>,
        migrationOption = <migrationOption>
    )
        .collect { result ->
            when (result) {
                is Result.Success -> // hande success
                is Result.Error -> // hande error
            }
        }
}
job.cancel() //job could be cancelled
```

### Invoke Rights

Sends a request for updating consent status.
Uses `organizationCode` to form a full URL.
Is executed on IO thread.
- Parameter `configuration`: full configuration
- Parameter `identities`: map of `identityCodes` and `identityValues`. Keys and values shouldn't be null
- Parameter `userData`: consists user information like email
- Parameter `rights`: list of strings of rights. Rights shouldn't bu null
- Returns: `Flow` of `Result.Success` if successful and with an error if request or its handling failed

```kotlin
fun invokeRights(
        configuration: Configuration,
        identities: Iterable<IdentitySpace>,
        userData: UserData,
        rights: List<String>
): Flow<Result<RequestError, Unit>>
```
```kotlin
job = CoroutineScope(Dispatchers.Main).launch {
    repository?.invokeRights(
        configuration = config!!,
        identities = <identitiesMap>,
        userData = <userDataInformation>,
        rights = <rightsList>
    )
        .collect { result ->
            when (result) {
                is Result.Success -> // hande success
                is Result.Error -> // hande error
            }
        }
}
job.cancel() //job could be cancelled
```

## Cache
If `KetchRepository.Builder` class is not propagated with `cacheProvider`, caching for all requests will be skipped.
`SharedPreferencesCacheProvider` is the default implementation of `CacheProvider` interface. It could be used out of box or custom implementation could be set to `Builder`
As far as `SharedPreferencesCacheProvider` is based on default Android `SharedPreferences` component, it provides two-level cache out of box:

![SharedPreferencesCacheProvider scheme](/SharedPreferencesCacheProvider_scheme.jpg)
