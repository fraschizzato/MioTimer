# Third-party notices

MioTimer is built with third-party software. Those components remain governed by their own licenses; the MioTimer license does not replace or override them.

The source project currently references, among others:

| Component | Role | Upstream |
| --- | --- | --- |
| AndroidX Core KTX | Android application support | https://developer.android.com/jetpack/androidx |
| AndroidX Activity Compose | Compose activity integration | https://developer.android.com/jetpack/androidx |
| AndroidX Lifecycle Runtime KTX | Android lifecycle support | https://developer.android.com/jetpack/androidx |
| Jetpack Compose / Material 3 | UI toolkit | https://developer.android.com/compose |
| Gson | Local JSON serialization | https://github.com/google/gson |
| Kotlin | Programming language / build plugin | https://kotlinlang.org/ |
| Gradle | Build tooling | https://gradle.org/ |
| Android Gradle Plugin | Android build tooling | https://developer.android.com/build |

AndroidX and Gson components are commonly distributed under Apache License 2.0 terms; build tools and their transitive components may use additional licenses.

This notice is intended as a practical source-repository summary, not as a substitute for the license metadata of the exact dependency graph. Before distributing a production binary, regenerate/review the resolved dependencies and include any notices required by the versions actually shipped.
