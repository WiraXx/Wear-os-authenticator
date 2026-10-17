# Proton Authenticator

This repository contains the source code for the Proton Authenticator Android application.

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
alt="Get it on Google Play"
height="80">](https://play.google.com/store/apps/details?id=proton.android.authenticator)

## How to build

If you want to build the app locally, please refer to the [BUILD.md](./docs/public/BUILD.md) file.

## Wear OS

The `:wear` module is a Wear OS companion app. Once installed on a watch paired with the phone, it automatically receives the phone's entries and account name through the Wearable Data Layer (no separate login), stores them encrypted with the Android Keystore and computes the codes offline.

The [Build Android & Wear OS](.github/workflows/build.yml) GitHub Action builds two phone/watch pairs and publishes the raw APKs on the `latest-build` pre-release (and as workflow artifacts):

- `play`: application id `proton.android.authenticator` (same as the Play Store app, uninstall it first).
- `alpha`: application id `proton.android.authenticator.alpha` (installs next to the Play Store app).

```shell
adb install ProtonAuthenticator-phone-alpha-debug.apk                  # phone
adb -s <watch-ip>:<port> install ProtonAuthenticator-wear-alpha-debug.apk  # watch (adb over Wi-Fi)
```

The two apps must share the same application id and signing key: install a matching pair from the same release, or set the `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and (optionally) `ANDROID_KEY_PASSWORD` repository secrets to sign every build with your own key. The Wear OS sync is not available in the `fdroid` flavor (it needs Google Play Services). Locally: `./gradlew :app:assemblePlayProdDebug :wear:assembleDebug` (use `-Pwear.applicationIdSuffix=.alpha` with the `alpha` flavor).

## Develop

If you want to contribute to the application, please refer to the [CONTRIBUTING.md](CONTRIBUTING.md) file.

## Help us translate

If you want to help us to translate the application, you can learn more about it on [our blog post](https://proton.me/blog/translation-community).

## License

The code and data files in this distribution are licensed under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. See <https://www.gnu.org/licenses/> for a copy of this license.

See [LICENSE](LICENSE) file
