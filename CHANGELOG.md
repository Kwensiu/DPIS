# Changelog

## [1.6.3](https://github.com/Kwensiu/DPIS/compare/v1.6.2...v1.6.3) (2026-04-25)


### UI

* optimize the warning dialog for restarting/launching system apps ([#32](https://github.com/Kwensiu/DPIS/issues/32)) ([c0f767b](https://github.com/Kwensiu/DPIS/commit/c0f767b1ed80ed131479bd601c05f134b63bdc5b))

## [1.6.2](https://github.com/Kwensiu/DPIS/compare/v1.6.1...v1.6.2) (2026-04-24)


### Bug Fixes

* avoid app list reload on rotation ([#26](https://github.com/Kwensiu/DPIS/issues/26)) ([a56a9e8](https://github.com/Kwensiu/DPIS/commit/a56a9e87b7cde0362d78ef9f60e44c5e59d4322c))
* unexpectedly enabled auto-rotate on launch ([#30](https://github.com/Kwensiu/DPIS/issues/30)) ([189e235](https://github.com/Kwensiu/DPIS/commit/189e235dfa0152b822b2127e00690500bc6af891))


### Refactoring

* MainActivity architecture and unify update flow ([dc048b9](https://github.com/Kwensiu/DPIS/commit/dc048b91c110a8722d047f467acb7927d0d59693))

## [1.6.1](https://github.com/Kwensiu/DPIS/compare/v1.6.0...v1.6.1) (2026-04-23)


### Bug Fixes

* keep field-rewrite font mode when system hooks are enabled ([4388a86](https://github.com/Kwensiu/DPIS/commit/4388a864ff60a4f43e15f99f44c719bb79d878da))


### Performance

* next-phase hardening for per-app display config and hook policy ([#29](https://github.com/Kwensiu/DPIS/issues/29)) ([f0a354d](https://github.com/Kwensiu/DPIS/commit/f0a354dbb73277c66d8b6bb273521b4cf0a19f08))

## [1.6.0](https://github.com/Kwensiu/DPIS/compare/v1.5.0...v1.6.0) (2026-04-23)


### Features

* add config backup import/export flow ([#15](https://github.com/Kwensiu/DPIS/issues/15)) ([343f0c0](https://github.com/Kwensiu/DPIS/commit/343f0c0e9c05dc7c82d549128e9d5614b105b24f))


### Bug Fixes

* optimize app list refresh updates ([#10](https://github.com/Kwensiu/DPIS/issues/10)) ([920655b](https://github.com/Kwensiu/DPIS/commit/920655b9f597486725a7d2a809d8160674b829dd))
* remove nested backup dialog borders ([#15](https://github.com/Kwensiu/DPIS/issues/15)) ([e754b60](https://github.com/Kwensiu/DPIS/commit/e754b60d16ed0d16533fdb70f07ac7b22310e3c0))
* stabilize async icon loading and eliminate list icon flicker ([0e22595](https://github.com/Kwensiu/DPIS/commit/0e22595eb65d8fa03e887560514031f163339476))
* stabilize viewport density across orientation ([#8](https://github.com/Kwensiu/DPIS/issues/8)) ([7f3b235](https://github.com/Kwensiu/DPIS/commit/7f3b235820f39a72f42d8d7d5f622724278afc59))


### Styles

* use custom dialogs for config backup actions ([#15](https://github.com/Kwensiu/DPIS/issues/15)) ([aa330a8](https://github.com/Kwensiu/DPIS/commit/aa330a84d827d8883fc9d0433f86be76b7d0fb55))

## [1.5.0](https://github.com/Kwensiu/DPIS/compare/v1.4.1...v1.5.0) (2026-04-23)


### Features

* improve update prompt flow and dialog reuse ([a3a97f5](https://github.com/Kwensiu/DPIS/commit/a3a97f5626c25d21c5ed053555b64a52e977d8c5))


### Bug Fixes

* show app label instead of package name in toast messages ([#13](https://github.com/Kwensiu/DPIS/issues/13)) ([67c5f22](https://github.com/Kwensiu/DPIS/commit/67c5f227f36d6d683fe06d7b4a6743e54f0afd56))


### UI

* fix dialog input focus, save feedback, keyboard handling, revert snackbar to toast ([96ee2b6](https://github.com/Kwensiu/DPIS/commit/96ee2b6c8fa7c4f9a86dfb5d48c3c418305a74c6))
* fix status bar inversion, touch feedback, fixed toolbar, input clear buttons, toast→snackbar, icon adjustments, bottom sheet simplification ([31b052b](https://github.com/Kwensiu/DPIS/commit/31b052b9aa3730d7c726493793ca9c4bd48f4ab5))

## [1.4.1](https://github.com/Kwensiu/DPIS/compare/v1.4.0...v1.4.1) (2026-04-23)


### Bug Fixes

* decouple system hook toggle from system scope ([#17](https://github.com/Kwensiu/DPIS/issues/17)) ([bfc7e68](https://github.com/Kwensiu/DPIS/commit/bfc7e68adb577e7924c0ca1d48a2f33f9ad287c4)), closes [#11](https://github.com/Kwensiu/DPIS/issues/11)

## [1.4.0](https://github.com/Kwensiu/DPIS/compare/v1.3.0...v1.4.0) (2026-04-22)


### Features

* polish app list help actions and tutorial dialog ([5f9bd94](https://github.com/Kwensiu/DPIS/commit/5f9bd9462d470fd2829049a6645b05df6043b7c9))
* refine app config sheet interactions and warning states ([d3cca7d](https://github.com/Kwensiu/DPIS/commit/d3cca7db479fc2f982e2a436de1c30beb39fa3b1))


### Bug Fixes

* polish help tutorial theming and layout smoke coverage ([7cdbde3](https://github.com/Kwensiu/DPIS/commit/7cdbde36c0a24ed924ccceb69a027db9ec0e6be1))


### UI

* add shared touch haptic feedback foundation ([6a342c4](https://github.com/Kwensiu/DPIS/commit/6a342c4480948d494feb60000280c0d487693f75))

## [1.3.0](https://github.com/Kwensiu/DPIS/compare/v1.2.0...v1.3.0) (2026-04-22)


### Features

* harden app detail sheet structure and layout ([1d69b7c](https://github.com/Kwensiu/DPIS/commit/1d69b7c29e9bf261f19bf2d316603e98123803f5))

## [1.2.0](https://github.com/Kwensiu/DPIS/compare/v1.1.0...v1.2.0) (2026-04-21)


### Features

* add in-app update download and install flow ([f83d05b](https://github.com/Kwensiu/DPIS/commit/f83d05bf2de7df1103fbc9be9a308b0bdc47b35d))
* add startup disclaimer gate with persistent consent ([626aaf4](https://github.com/Kwensiu/DPIS/commit/626aaf40c02b6097ec1a36c69d7443641041323a))


### Bug Fixes

* align package identifiers after applicationId rename ([dd6139c](https://github.com/Kwensiu/DPIS/commit/dd6139c351ae13127d3d2137ba1765ea60bbd669))
* align system hook emulation hint with effective scope state ([6e8818b](https://github.com/Kwensiu/DPIS/commit/6e8818b23ae4ce45559fda729f8270a4366b0620))
* improve font debug diagnostics and stats routing ([3009676](https://github.com/Kwensiu/DPIS/commit/30096763114d60d0f77e6449a6ead414e7e0fe75))
* keep app detail sheet open and refresh status in-sheet ([fb4bc98](https://github.com/Kwensiu/DPIS/commit/fb4bc9867d65b751872bbf6329438e5ec99c78ff))
* remove yuki smoke artifacts from current baseline ([653a492](https://github.com/Kwensiu/DPIS/commit/653a4926d07e53e316b3b1129efb0bbbe2ba79e2))

## [1.1.0](https://github.com/Kwensiu/DPIS/compare/v1.0.2...v1.1.0) (2026-04-19)


### Features

* **update:** add manifest-based update check and release metadata ([3762e13](https://github.com/Kwensiu/DPIS/commit/3762e13aa6f4775840b9d8c4bf05d44ed511700b))

## [1.0.2](https://github.com/Kwensiu/DPIS/compare/v1.0.1...v1.0.2) (2026-04-19)


### Bug Fixes

* **ci:** ensure gradlew executable in release workflow ([ac4a932](https://github.com/Kwensiu/DPIS/commit/ac4a932c70f3f02f8d043ca773da86881f81cc7e))

## [1.0.1](https://github.com/Kwensiu/DPIS/compare/v1.0.0...v1.0.1) (2026-04-19)


### Bug Fixes

* **ci:** avoid sdkmanager license pipefail failure ([2449e93](https://github.com/Kwensiu/DPIS/commit/2449e938d896d8ee9b9571acdeb414a5f81aa7b7))
* **ci:** make sdkmanager license acceptance pipefail-safe ([2031360](https://github.com/Kwensiu/DPIS/commit/2031360fe72f2ab268e5dccce2c0c30a695ab906))

## 1.0.0 (2026-04-19)


### Features

* add per-app viewport spoofing module ([e6ce14a](https://github.com/Kwensiu/DPIS/commit/e6ce14a48d4df6268db0c3f7a324d6839bcd3d8e))
* add settings other section with about and launcher icon toggle ([ecf2939](https://github.com/Kwensiu/DPIS/commit/ecf2939df6fd6cd8640d90b3165499260499aa5b))
* **core:** refactor hook pipeline and runtime policy ([eb30409](https://github.com/Kwensiu/DPIS/commit/eb3040968675e7ce05874b059a2843bc81b9cf7a))
* **font:** refine hook gating and migrate mirrored config safely ([cd8d756](https://github.com/Kwensiu/DPIS/commit/cd8d75698c1c66fdac066e79035620136b388734))
* redesign app detail actions with per-app DPIS controls ([cfc02d8](https://github.com/Kwensiu/DPIS/commit/cfc02d87914352c1cc5cfbc10cf14f6dd91bdb26))
* **ui:** add paged app list with filter sheet and state restore ([42def8f](https://github.com/Kwensiu/DPIS/commit/42def8f8668bf06f0c02a642286b5bf8669443ac))
* **ui:** rebuild settings page and controls ([e8447d1](https://github.com/Kwensiu/DPIS/commit/e8447d180325b527b26dabd6b4479c4824433b62))


### Bug Fixes

* **build:** suppress BlockedPrivateApi lint for Xposed hook path ([ec657d9](https://github.com/Kwensiu/DPIS/commit/ec657d93328045213953e8e4310dc0d5a42e8dd4))
* **ui:** align settings header and unify icon tint ([1c50157](https://github.com/Kwensiu/DPIS/commit/1c501579b1d12bbbd8f5beb725c6dd8bb5735943))
