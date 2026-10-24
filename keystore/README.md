# Debug signing key

`prompter-debug.jks` is a **debug-only** signing key committed so that every
GitHub Actions build signs the APK with the same certificate. This allows
`adb install -r` to update the app without uninstalling it.

- Alias: `prompterdebug`
- Store/key password: `prompterdebug`
- Certificate SHA-256:
  `6B:8A:27:78:33:4F:4F:49:A2:E1:5D:2E:40:1F:89:49:05:FF:0F:BB:2A:88:2F:1B:68:7C:02:B5:29:95:CC:33`

This key must **not** be used for a public release. A separate release keystore
and signing configuration will be created when release signing is implemented.
