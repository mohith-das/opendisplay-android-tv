# Build and release

Development requires JDK 21 and Android SDK 36.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Production releases use `keystore.properties`, which is ignored, to point at a protected PKCS#12 keystore stored outside this repository. The tag workflow restores that same key from encrypted GitHub Actions secrets, runs tests and release lint, signs and verifies the APK, generates its SHA-256 checksum, and publishes both files.

See the build, signing, and contribution sections in [README.md](README.md) and [CONTRIBUTING.md](CONTRIBUTING.md). Never commit signing material, credentials, tokens, local SDK paths, or generated APKs.
