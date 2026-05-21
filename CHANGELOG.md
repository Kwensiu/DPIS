# Changelog

## [1.10.0](https://github.com/Kwensiu/DPIS/compare/v1.9.1...v1.10.0) (2026-05-21)


### Features

* add .ttc font collections ([#63](https://github.com/Kwensiu/DPIS/issues/63)) ([fe0a529](https://github.com/Kwensiu/DPIS/commit/fe0a5291e77a95e49f37911eea081b2858c9327e))
* font file replacement ([#60](https://github.com/Kwensiu/DPIS/issues/60)) ([2b7de99](https://github.com/Kwensiu/DPIS/commit/2b7de9960d4cf604c0393b48e8e648dd1e3e43af))


### UI

* align experimental settings with main settings style ([29bce9f](https://github.com/Kwensiu/DPIS/commit/29bce9f515d57cd728c0ac5b1f9a425ff460debb))

## [1.9.1](https://github.com/Kwensiu/DPIS/compare/v1.9.0...v1.9.1) (2026-05-20)


### Refactoring

* unify per-app font and DPI hook scheduling ([#57](https://github.com/Kwensiu/DPIS/issues/57)) ([c54b454](https://github.com/Kwensiu/DPIS/commit/c54b454a0c08e3f073259141731de7cfd7de1335))

## [1.9.0](https://github.com/Kwensiu/DPIS/compare/v1.8.2...v1.9.0) (2026-05-17)


### Features

* Unify Font Hook Domains and Stabilize Viewport Scaling ([#52](https://github.com/Kwensiu/DPIS/issues/52)) ([ace8658](https://github.com/Kwensiu/DPIS/commit/ace8658e16a0b29ca1bf5d167b21eae3fab64a8f))


### Bug Fixes

* restore post-squash font cleanup commits ([#55](https://github.com/Kwensiu/DPIS/issues/55)) ([df17016](https://github.com/Kwensiu/DPIS/commit/df1701621a6b0dc922536bc972ccf0e738604ae9))


### UI

* rename UI terminology to smallest width, system, and compat ([ec1030e](https://github.com/Kwensiu/DPIS/commit/ec1030ec25446a2b61e76bdec2ed11b22421323c))

## [1.8.2](https://github.com/Kwensiu/DPIS/compare/v1.8.1...v1.8.2) (2026-05-15)


### Bug Fixes

* complete viewport override path on API 101/100 ([#48](https://github.com/Kwensiu/DPIS/issues/48)) ([2287cbd](https://github.com/Kwensiu/DPIS/commit/2287cbde49ef3e2f924d112b33829ffda958cb1e))

## [1.8.1](https://github.com/Kwensiu/DPIS/compare/v1.8.0...v1.8.1) (2026-05-13)


### Features

* add safe cache cleanup entry ([8211c1e](https://github.com/Kwensiu/DPIS/commit/8211c1e963306aa8f058c0dcc2c954fcb53610f2))


### Bug Fixes

* align legacy scope recommendation and version naming ([d2b6f39](https://github.com/Kwensiu/DPIS/commit/d2b6f39ccb27f1c94433efa28e323997f96ad1e1))
* improve release notes markdown rendering ([994d8e9](https://github.com/Kwensiu/DPIS/commit/994d8e96a661654ee11b17e5ee85a40964260e7a))

## [1.8.0](https://github.com/Kwensiu/DPIS/compare/v1.7.1...v1.8.0) (2026-05-12)


### Features

* publish dual APKs (modern + legacy) in release workflows ([ee9f3e7](https://github.com/Kwensiu/DPIS/commit/ee9f3e7ad5ee2c95284dcc3ab14111a69afd5bfc))
* split modern101 and compat100 Xposed builds ([fa24caf](https://github.com/Kwensiu/DPIS/commit/fa24cafc9d90b961ef181590ca291fa4c269a46f))


### Bug Fixes

* cleanup dead tests ([390e4bd](https://github.com/Kwensiu/DPIS/commit/390e4bd8e0f257c4202e72d6200edb488866e746))
* guard compat100 text size rewrite ([af86482](https://github.com/Kwensiu/DPIS/commit/af8648204c9c9897c23a40dc41d901e1ed6bfb50))
* improve startup update release notes dialog ([a6002bf](https://github.com/Kwensiu/DPIS/commit/a6002bfc9824bb65099d8f3810cabd7d2d80c2c4))
* mark compat100 builds as legacy ([b89e66a](https://github.com/Kwensiu/DPIS/commit/b89e66ab9c0223aee96d9e1828c96aa4181a413b))
* migrate font debug stats before legacy cleanup ([a815893](https://github.com/Kwensiu/DPIS/commit/a81589362c9a2b85a3f818499d043ff8c3b890cf))
* persist compat font recovery paths ([dc2f8a5](https://github.com/Kwensiu/DPIS/commit/dc2f8a5b47dad47260a292883eb89757c1d82e30))
* refine compat100 scope fallback UI and remove unused manual-scope strings ([ac99904](https://github.com/Kwensiu/DPIS/commit/ac999040319d4dde1a85292112a183595a3bdc7e))
* restore compat100 HyperOS runtime sync ([414a3d2](https://github.com/Kwensiu/DPIS/commit/414a3d2fcb01433790feb9c5ee8f70b60a2dad9d))
* restore release notes toggle in startup update dialog ([ff10dc5](https://github.com/Kwensiu/DPIS/commit/ff10dc598a15183e74c2cd0adaf943da62631104))
* stabilize compat100 hook configuration ([5e73c79](https://github.com/Kwensiu/DPIS/commit/5e73c7938d13d7057f603940d436191637e4ec40))


### Refactoring

* cache system server package uid lookups ([41f71f8](https://github.com/Kwensiu/DPIS/commit/41f71f847c376bc034c771259f48ebcf3d39deb2))
* cache system server reflection probes ([7c6f086](https://github.com/Kwensiu/DPIS/commit/7c6f0869b218fc38f93e8d9d8c670a52167d839a))
* centralize runtime property recovery ([35e7ade](https://github.com/Kwensiu/DPIS/commit/35e7ade7a183767bad561a2c748160a8719b7c4b))
* deduplicate preference fallback reads ([efea3f9](https://github.com/Kwensiu/DPIS/commit/efea3f9ed55a0c253bc2a998499fb571e06baaae))
* introduce config snapshot read model ([01d6593](https://github.com/Kwensiu/DPIS/commit/01d659336833aeecd9c265eabc5d74391aee7bbe))
* refresh system server config snapshots ([37186af](https://github.com/Kwensiu/DPIS/commit/37186af5450897f7cfdba916ae4c13a1b98a09f6))

## [1.7.1](https://github.com/Kwensiu/DPIS/compare/v1.7.0...v1.7.1) (2026-05-10)


### Features

* add confirmation before disabling safe mode ([8ec4405](https://github.com/Kwensiu/DPIS/commit/8ec440545aa514f0267b6af66a3fd338f1e3b1f2))
* show cached release notes in update dialog ([c95abf9](https://github.com/Kwensiu/DPIS/commit/c95abf9c2260ac2d4e2573674b1c0ceca1177fc3))


### Bug Fixes

* correct launcher icon hiding logic ([fa98411](https://github.com/Kwensiu/DPIS/commit/fa98411117e8742310b18bb773c129406dbab81d))
* enforce release system-hook default and lint config ([1d0d1f2](https://github.com/Kwensiu/DPIS/commit/1d0d1f211789404679a9ac77dbff3f4c4cdc51f7))


### Refactoring

* harden release notes update flow ([08419bd](https://github.com/Kwensiu/DPIS/commit/08419bdbc4d4c5fd6416e5e5f4c721658fa5dcef))

## [1.7.0](https://github.com/Kwensiu/DPIS/compare/v1.6.3...v1.7.0) (2026-05-09)


### Features

* add HyperOS Rust/Flutter dp/font scaling support ([#37](https://github.com/Kwensiu/DPIS/issues/37)) ([8233eac](https://github.com/Kwensiu/DPIS/commit/8233eac37cdf6c57ec529207e283a3175244a183))
* simplify reload advice dialog and remove HyperOS sheet warning ([8cc7091](https://github.com/Kwensiu/DPIS/commit/8cc7091df84ceb15f991e92628f4b816589e59af))


### Bug Fixes

* harden HyperOS native proxy font path ([71f80ae](https://github.com/Kwensiu/DPIS/commit/71f80aed6b4511d419878fd0e44c0074c1ae3cc6))
* request HyperOS installed apps permission ([3ac8b6d](https://github.com/Kwensiu/DPIS/commit/3ac8b6dfcbcd9db48846362de42b7c3a801ea8cf))

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
