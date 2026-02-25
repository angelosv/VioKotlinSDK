# Migration Guide: Reachu to Vio Kotlin SDK

The Reachu platform has rebranded to Vio. Alongside this brand change, the Kotlin SDK has undergone significant renaming of its modules and classes to mirror the Swift SDK and the new brand identity.

This guide outlines the changes required to migrate your existing Reachu Kotlin SDK integration to the new Vio Kotlin SDK.

## Module Renaming

The main modules have been updated to reflect the `Vio` prefix. You will need to update your Gradle dependencies and import statements accordingly.

| Old Module Name | New Module Name |
| :--- | :--- |
| `ReachuCore` | `VioCore` |
| `ReachuDesignSystem` | `VioDesignSystem` |
| `ReachuUI` | `VioUI` |
| `ReachuEngagementUI` | `VioEngagementUI` |
| `ReachuEngagementSystem` | `VioEngagementSystem` |
| `ReachuCastingUI` | `VioCastingUI` |
| `ReachuCheckoutUI` | `VioCheckoutUI` |

## Class Prefix Changes

Similarly, classes previously prefixed with `Reachu` have generally been renamed to use the `Vio` prefix. This applies across all modules, including UI components, configurations, and core systems.

**Examples of common class renaming:**

| Old Class Name | New Class Name |
| :--- | :--- |
| `ReachuConfiguration` | `VioConfiguration` |
| `ReachuUI` | `VioUI` |
| `ReachuColors` | `VioColors` |
| `ReachuTypography` | `VioTypography` |
| `ReachuProductCard` | `VioProductCard` |
| `ReachuCheckoutOverlayController` | `VioCheckoutOverlayController` |
| `ReachuCartManager` | `VioCartManager` |
| `ReachuEngagementManager` | `VioEngagementManager` |
| `ReachuPoll` | `VioPoll` |

*Note: You may find that some internal aliases (`typealias`) are still temporarily available to ease migration, but it is highly recommended to update your codebase to use the new `Vio` types.*

## API URL Changes

If you were manually configuring the SDK environment to point to custom Reachu URLs, please note that the platform URLs have also changed:

- Development / Sandbox URL: `https://graph-ql-dev.vio.live` (previously pointing to a reachu domain)
- Production URL: `https://graph-ql.vio.live`

Additionally, deeply linked URLs or return URLs (e.g., for payments) might use the new `vio://` scheme instead of `reachu://`.
Example: `returnUrl = "vio://klarna/callback"`

If you use `VioEnvironment.PRODUCTION` or `VioEnvironment.SANDBOX`, the SDK handles these URL resolutions internally.

## Steps for Migration

1.  **Update Dependencies:** Change all SDK artifacts in your `build.gradle.kts` to point to the new package names (if published under a new group coordinate) or the updated module artifacts.
2.  **Global Find & Replace:** Run a project-wide search for `Reachu` and replace it with `Vio`. Review each change to ensure it applies to the SDK classes.
3.  **Update Configurations:** If you were using raw JSON configuration files, rename them (e.g., `vio-config.json` to `vio-config.json`) and update their contents to use the new `vio.live` endpoints.
4.  **Recompile & Test:** Build the project. The compiler will highlight any missed renaming. Thoroughly test the integration, paying special attention to initialization and checkout flows.
