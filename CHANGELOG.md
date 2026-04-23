# Changelog

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
