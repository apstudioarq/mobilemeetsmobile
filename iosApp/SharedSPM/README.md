# SharedKMM (SPM)

This local Swift package wraps the KMM `shared` module as a binary XCFramework.

## Generate XCFramework

From repository root:

```bash
./gradlew :shared:prepareSharedSpm
```

That command generates:

- `iosApp/SharedSPM/shared.xcframework`

## Add to Xcode

1. Open your iOS project in Xcode.
2. Go to `File` -> `Add Package Dependencies...`
3. Click `Add Local...`
4. Select folder: `iosApp/SharedSPM`
5. Add product: `SharedKMM` to your app target.
6. In your app target build settings, add SQLite linker flag:
   - `OTHER_LDFLAGS` includes `-lsqlite3`

After linking, import the module in Swift as:

```swift
import shared
```
