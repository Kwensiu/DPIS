# Changelog

## [2.1.0](https://github.com/Kwensiu/DPIS/compare/v2.0.0...v2.1.0) (2026-09-06)


### Features

* **home:** customize workspace layout ([ad06893](https://github.com/Kwensiu/DPIS/commit/ad06893c366f03a76a76bc237e7f0f6b1e70b79b))
* **settings:** customize workspace navigation ([c7ac4e6](https://github.com/Kwensiu/DPIS/commit/c7ac4e61a7a134d372be10c6432772690ee5591f))


### Bug Fixes

* **ci:** skip release please Sonar analysis ([26f4ad1](https://github.com/Kwensiu/DPIS/commit/26f4ad181f893c408573336f709fa8fb8a6b7366))
* **ci:** use available Android 37 SDK package ([d039506](https://github.com/Kwensiu/DPIS/commit/d039506969743cb34be6c0d1a597310ee8f1d392))
* **deps:** update activity to v1.13.0 ([#119](https://github.com/Kwensiu/DPIS/issues/119)) ([f886c7b](https://github.com/Kwensiu/DPIS/commit/f886c7b04cbdecfbdeecfed20c43724c25052c59))
* **deps:** update dependency androidx.compose.material3:material3 to v1.5.0-alpha27 ([#118](https://github.com/Kwensiu/DPIS/issues/118)) ([8a5d5ec](https://github.com/Kwensiu/DPIS/commit/8a5d5ec22dea202f75ba0cc8bb5c45bc8d6f8061))
* **deps:** update dependency com.materialkolor:material-kolor to v5.0.1 ([#109](https://github.com/Kwensiu/DPIS/issues/109)) ([58d8366](https://github.com/Kwensiu/DPIS/commit/58d836666d211bdd799b2119889ae1e443203fb3))
* **deps:** update routine non-major dependencies ([#111](https://github.com/Kwensiu/DPIS/issues/111)) ([947b1a6](https://github.com/Kwensiu/DPIS/commit/947b1a67b6c6bffdfe51911c1730605aaa2bee8c))
* normalize WeChat bottom tab icons ([cd97137](https://github.com/Kwensiu/DPIS/commit/cd971371e1fe447870153a5ec9b08006036a365e))
* preserve quick config sheet behavior ([1acedec](https://github.com/Kwensiu/DPIS/commit/1acedecff6567f3aaacd6f6418fa1805c3aa64e1))
* prevent startup stalls from invalid viewport config ([4b3a006](https://github.com/Kwensiu/DPIS/commit/4b3a00690c951608806e05908a42a2b753f47951))
* resolve compose lint error [skip preview] ([e5aa411](https://github.com/Kwensiu/DPIS/commit/e5aa411b0ceb3e11d27861efe036bceb237fdcec))
* serialize LSPosed scope requests ([9757f1e](https://github.com/Kwensiu/DPIS/commit/9757f1edd1f20ce67237c67875b21beec6bca3ed))
* soften editor sheet resize motion ([2530b4c](https://github.com/Kwensiu/DPIS/commit/2530b4c41ee99a5afd19be95062bf39f82d3a18e))
* stabilize dialogs across appearance changes and rotation ([c2021eb](https://github.com/Kwensiu/DPIS/commit/c2021eb89b03f0ad360d6e3194bb79eaafcba5f7))
* unify adaptive Compose surfaces and interaction feedback ([cba0d70](https://github.com/Kwensiu/DPIS/commit/cba0d70add87a5fc24411ac0565f11c260903eb6))
* unify template filter sheet presentation ([277abfe](https://github.com/Kwensiu/DPIS/commit/277abfe4b5fbd10902d3d82fadb615e3f24aa3e5))


### Refactoring

* consolidate settings workspace ownership ([ebfdea4](https://github.com/Kwensiu/DPIS/commit/ebfdea4ccee42f9e44cb5a5cbb0908b455876414))
* **dialog:** unify Compose dialog ownership ([1674cf8](https://github.com/Kwensiu/DPIS/commit/1674cf89180b4870f2250bafbb2e3cfcb7445986))
* extract app and tools workspace ownership ([1982eed](https://github.com/Kwensiu/DPIS/commit/1982eed32af075f3c921095b8ed54851e4fc6f0c))
* **home:** remove legacy workspace implementation ([c7cc818](https://github.com/Kwensiu/DPIS/commit/c7cc818b791a660aec92bb3408d17d543a0091ed))
* isolate classic preferences in legacy flavor ([f3f5042](https://github.com/Kwensiu/DPIS/commit/f3f5042d2d188ff6e9f9df2747351c810bf45d5e))
* **layout:** organize presentation code and tests ([0552622](https://github.com/Kwensiu/DPIS/commit/05526223184a41f7315173d23afd9187af3cc791))
* migrate settings controller to Kotlin session ownership ([20113c9](https://github.com/Kwensiu/DPIS/commit/20113c94fa13c11ed1c8da5b72071fe755559c5f))
* organize compose presentation modules ([c3e4d92](https://github.com/Kwensiu/DPIS/commit/c3e4d9233f881d0b3b4fe83675dc827f3a2b6cbe))
* polish template Compose migration ([#108](https://github.com/Kwensiu/DPIS/issues/108)) ([e64c1a4](https://github.com/Kwensiu/DPIS/commit/e64c1a4913f3113a97e92425fddac5619edb8045))
* unify compose editor presentation ([c6d7a04](https://github.com/Kwensiu/DPIS/commit/c6d7a0489d16262eabbc49e39484c21d00f29388))
* unify compose interaction feedback ([f352cf6](https://github.com/Kwensiu/DPIS/commit/f352cf6b32afba0d391e0c1700ac4cf5c0e4789e))

## [2.0.0](https://github.com/Kwensiu/DPIS/compare/v1.15.0...v2.0.0) (2026-08-30)


### Features

* adapt workspace UI for compact watches ([bcacc39](https://github.com/Kwensiu/DPIS/commit/bcacc39c24f0827944e1f94925d704920ae39dab))
* add compose theme settings ([d51a0e1](https://github.com/Kwensiu/DPIS/commit/d51a0e1f671839da5677f0d94960898798ed35b3))
* improve language settings ([e75d52d](https://github.com/Kwensiu/DPIS/commit/e75d52d5850d92a988565ba44a64d3ddbd3dc815))
* promote TTC font collections ([e07143f](https://github.com/Kwensiu/DPIS/commit/e07143fe44107dee9c9b10e688379af240eb1cba))
* refine template target filters ([57d25e3](https://github.com/Kwensiu/DPIS/commit/57d25e3d560ae69837c4ec7f0a7f795002fd5c5a))
* **ui:** establish Compose design system and adaptive app shell ([#100](https://github.com/Kwensiu/DPIS/issues/100)) ([36c1aa1](https://github.com/Kwensiu/DPIS/commit/36c1aa16616b0e42e4e228bd3dab812d33683e66))


### Bug Fixes

* align diagnostic log gate ([aeb0725](https://github.com/Kwensiu/DPIS/commit/aeb0725493f887da6883e83a98762ed4720de603))
* **api:** support modern 101 baseline with 102 capabilities ([26ac481](https://github.com/Kwensiu/DPIS/commit/26ac481e3d6fbdc1704f5b329af98bc7fa878b64))
* **app-config:** align saved state and configured app lists ([0b0ef82](https://github.com/Kwensiu/DPIS/commit/0b0ef82a34c0ab85b3614a94df8ed20d29c5773b))
* **app-list:** hide system scope from configured apps ([5ea227f](https://github.com/Kwensiu/DPIS/commit/5ea227fc11d54937be04c7881e1199d03378bf45))
* **applist:** restore launcher catalog fallback ([9d4e557](https://github.com/Kwensiu/DPIS/commit/9d4e557e56257b781b040d6ef21c3a534b7f142f))
* Flutter Typeface Routing ([#102](https://github.com/Kwensiu/DPIS/issues/102)) ([e9d745e](https://github.com/Kwensiu/DPIS/commit/e9d745eaf9da278536eb4a02831109627584149e))
* harden imported font library ([bfbc82e](https://github.com/Kwensiu/DPIS/commit/bfbc82e29bd61b82378c005859211941b740fd9e))
* harden independent WeChat DPI route diagnostics ([4bdaab5](https://github.com/Kwensiu/DPIS/commit/4bdaab58c5298718cbeeaef38f074ae8546e28df))
* preserve hook chain domains across editor sessions ([90b2873](https://github.com/Kwensiu/DPIS/commit/90b287337ed122ae04b9f28f90fa2e7328d70b2b))
* preserve imported package target types ([82cd152](https://github.com/Kwensiu/DPIS/commit/82cd152ad7f39b4b6ba4157682bf346108307a1b))
* prioritize active config editor page interactions [skip preview] ([3b5d910](https://github.com/Kwensiu/DPIS/commit/3b5d91016a186a30d5c0ddd94adc28ee09a8716a))
* provide fallback round dialog dimensions ([8a4d3a0](https://github.com/Kwensiu/DPIS/commit/8a4d3a05768693a147b58669142afe074f2a0733))
* render theme swatches with seed colors ([8b61a95](https://github.com/Kwensiu/DPIS/commit/8b61a9525e27fedb9887f8ea3cb1b009bd3f3100))
* restore startup update prompts [skip preview] ([eb24dfc](https://github.com/Kwensiu/DPIS/commit/eb24dfc7754cc8ca164644d9289557da9eecc10c))
* show reload notice once per install ([df4cbf6](https://github.com/Kwensiu/DPIS/commit/df4cbf619edf5efe5e86c9b6beb9a11b6362b4f5))
* **ui:** align WeChat DPI editor spacing ([c4df5b5](https://github.com/Kwensiu/DPIS/commit/c4df5b5fa14b140a59fd1ca1ea4f7ffebfcaf1a4))
* **ui:** refine compose edge-to-edge surfaces ([2775d3c](https://github.com/Kwensiu/DPIS/commit/2775d3c4f109fed4405ecb7cb391cf20fc14e1c9))
* **ui:** refine configuration editor surfaces ([aca087e](https://github.com/Kwensiu/DPIS/commit/aca087e08bea90390df3bbefc577dde39fe62be0))
* **ui:** refine quick template target picker ([ab8a5cd](https://github.com/Kwensiu/DPIS/commit/ab8a5cd215186b84d2f91b53f05d1989a7acf1ee))
* **ui:** stabilize compose workspace surfaces ([90136e5](https://github.com/Kwensiu/DPIS/commit/90136e59d1e13a6010b1c8a5e2590d416517fd75))
* **ui:** TypefacePicker page ([8fc2817](https://github.com/Kwensiu/DPIS/commit/8fc28172ba55270f8e6a4d13b65d4275b49963d8))
* unify workspace sheet semantics ([15e78d9](https://github.com/Kwensiu/DPIS/commit/15e78d99ccacdd69e389a3e724bb618db7917859))


### Refactoring

* clarify app filter and configuration state semantics ([a620006](https://github.com/Kwensiu/DPIS/commit/a620006a10db7a29b88abfda7fbc40877893e4bd))
* consolidate template workspace architecture ([0e32930](https://github.com/Kwensiu/DPIS/commit/0e32930ad5514c85011b0abc50b8c889a997fbcb))
* harden config backup lifecycle ([#105](https://github.com/Kwensiu/DPIS/issues/105)) ([c87faeb](https://github.com/Kwensiu/DPIS/commit/c87faeb5b3cff60eb3866f73707e5db4787dea26))
* migrate workspace code and tests to Kotlin ([#106](https://github.com/Kwensiu/DPIS/issues/106)) ([5c39fc1](https://github.com/Kwensiu/DPIS/commit/5c39fc145fc22545308ef100ebce7560ebca11fa))
* redesign diagnostics & runtime evidence collection ([#104](https://github.com/Kwensiu/DPIS/issues/104)) ([fce7fd5](https://github.com/Kwensiu/DPIS/commit/fce7fd584f7d6053d08c266a7481d136da04fd5c))
* **ui:** consolidate md3 surface migration ([5c8316a](https://github.com/Kwensiu/DPIS/commit/5c8316a898f55df160b81f16de0d3bb2b2072c5f))
* **ui:** reorganize log entry ([7c57573](https://github.com/Kwensiu/DPIS/commit/7c57573888db212de1c5fe6623bca232ce57ea99))
* **ui:** streamline app editor ownership ([150cd6f](https://github.com/Kwensiu/DPIS/commit/150cd6f4ebbfa0824da898226f734e1d55d72255))
* **ui:** Wear OS UI/UX ([586ba30](https://github.com/Kwensiu/DPIS/commit/586ba30efd057badf189cfc98fc8a09fcdbdeb62))


### Chores

* prepare 2.0.0 release ([7446c52](https://github.com/Kwensiu/DPIS/commit/7446c52aaa196a5c0d5f3c510c7d3f730583c629))

## [1.15.0](https://github.com/Kwensiu/DPIS/compare/v1.14.0...v1.15.0) (2026-07-07)


### Features

* add quick config tile ([158cb62](https://github.com/Kwensiu/DPIS/commit/158cb62645ecf2f82d227f2542fe66f6b7b2a255))
* **api:** support libxposed API 102 ([#94](https://github.com/Kwensiu/DPIS/issues/94)) ([d63a68d](https://github.com/Kwensiu/DPIS/commit/d63a68d518a4dff82e61bfd81700176f1a713ede))
* support owner-scoped WebAPK scaling in Chrome ([eba4901](https://github.com/Kwensiu/DPIS/commit/eba49012fc242d01a9283c08f683a2dca959815f))


### Bug Fixes

* harden system window relayout routing ([e9e1ded](https://github.com/Kwensiu/DPIS/commit/e9e1ded3a4a00f6c3e76844115116e8b731835fd))
* keep local UI state out of remote config ([eed862b](https://github.com/Kwensiu/DPIS/commit/eed862b7bebd455840ee549ad64f57ebbba3abdb))
* polish main UI search state and scale setting ([b7075c6](https://github.com/Kwensiu/DPIS/commit/b7075c6075f6dffab350dd1d8a833982be9153e7))
* stabilize modern package-loaded app hooks ([122ca0c](https://github.com/Kwensiu/DPIS/commit/122ca0ce0da258cc82750f16ec43e0f8c3fb3f2e))
* stabilize viewport route fallback and Chrome launch-item scaling ([645cd91](https://github.com/Kwensiu/DPIS/commit/645cd91f9455ac6d4c28f6cc9c1826f19791277f))
* unify auto viewport fallback to compat across legacy and modern runtime paths ([0b15608](https://github.com/Kwensiu/DPIS/commit/0b156080ed75a10570099e13fffe14ed39ccdad9))


### Performance

* cache viewport resolution on resources read hot path ([d9e2bb7](https://github.com/Kwensiu/DPIS/commit/d9e2bb7945c6bccc5630a1ae4295062acf9034ec))
* reduce font hook hot-path allocations ([ad1b7bc](https://github.com/Kwensiu/DPIS/commit/ad1b7bc07d00dcfb3ff613071515c6057470713d))


### Refactoring

* **config:** support three-decimal viewport scale precision ([9e1cb93](https://github.com/Kwensiu/DPIS/commit/9e1cb93f64ccc2555e550fb8c3dede102910f430))
* rename DpiConfigStore to DpisConfigStore ([072ee0e](https://github.com/Kwensiu/DPIS/commit/072ee0e2f6e703472605c504e1d5de5684712d6d))
* split local ui preference stores ([1021005](https://github.com/Kwensiu/DPIS/commit/102100577e82ae2f820385511c44149d950b507d))

## [1.14.0](https://github.com/Kwensiu/DPIS/compare/v1.13.1...v1.14.0) (2026-06-23)


### Bug Fixes

* count known injected apps as configured ([a7877e6](https://github.com/Kwensiu/DPIS/commit/a7877e690e49330fa92cb05d9358e88fbaac77c9))
* enrich WeChat DPI feedback diagnostics ([c907428](https://github.com/Kwensiu/DPIS/commit/c9074285596d04227163b09e253bd1c95c527ac0))
* simplify WeChat 8.0.74 route and retire g/k/l path ([45a62ea](https://github.com/Kwensiu/DPIS/commit/45a62eae091928508ed5595260036693db1dcb9e))
* stabilize WeChat DPI static route semantics ([d353dfb](https://github.com/Kwensiu/DPIS/commit/d353dfb4279240d96aa0bc06ba0da6129bfac56f))
* streamline Paint font fallback dispatch ([17a462b](https://github.com/Kwensiu/DPIS/commit/17a462bd0278cd0dfa1f35a7dd58327ee9c9c1fd))
* tighten feedback diagnostic skip sampling ([2e26c0a](https://github.com/Kwensiu/DPIS/commit/2e26c0a7b0efccb3417fa9deb7a28aca2e27623e))

## [1.13.1](https://github.com/Kwensiu/DPIS/compare/v1.13.0...v1.13.1) (2026-06-21)


### Features

* add DPIS diagnostic log viewer ([0da5de7](https://github.com/Kwensiu/DPIS/commit/0da5de792077aab4bc36ed31df8aed348adfe784))
* add log export sharing ([9030d14](https://github.com/Kwensiu/DPIS/commit/9030d149e16804adf922a96b2724f22eb43475eb))
* expand feedback diagnostics for WeChat hook routes ([1b22831](https://github.com/Kwensiu/DPIS/commit/1b22831ded96e31512a0447731d79e5951ad38cc))
* feedback diagnostic function ([#92](https://github.com/Kwensiu/DPIS/issues/92)) ([6eeaff5](https://github.com/Kwensiu/DPIS/commit/6eeaff5cb94f59073b811a17cd6045b38d316bd8))


### Bug Fixes

* clarify per-app configuration semantics ([815e206](https://github.com/Kwensiu/DPIS/commit/815e2062c5a95fdafb1f111ca1a186b697df6975))
* explicitly link c++_static for native library ([05c0016](https://github.com/Kwensiu/DPIS/commit/05c001669042c9b3cd01f801148b12073d389346))
* normalize app config dirty state by final draft semantics ([39a597d](https://github.com/Kwensiu/DPIS/commit/39a597d99174f42b00b3c3a0a65966d42a71f9d8))
* restore draft hook-chain state in editor ([4e2b438](https://github.com/Kwensiu/DPIS/commit/4e2b438646a194fe4421874ff194800211f746ef))
* tighten feedback diagnostic log windowing ([ea014c2](https://github.com/Kwensiu/DPIS/commit/ea014c2d9683b3301dc0b5c0ae4fa79698efe35d))


### Refactoring

* package config aggregation ([#91](https://github.com/Kwensiu/DPIS/issues/91)) ([18d58c6](https://github.com/Kwensiu/DPIS/commit/18d58c66845b043cbb3987281d6682341f9b4a49))
* simplify configured apps home status card ([a713359](https://github.com/Kwensiu/DPIS/commit/a713359bf9ab18a331ec085e37f67a68d770e700))

## [1.13.0](https://github.com/Kwensiu/DPIS/compare/v1.12.3...v1.13.0) (2026-06-17)


### Features

* combine template summary chips ([7cf837a](https://github.com/Kwensiu/DPIS/commit/7cf837a323957727da397ac030702e54b48484ee))
* refine font hook guidance and defaults ([224aae1](https://github.com/Kwensiu/DPIS/commit/224aae14cf5749a2aa6d5864eddeb28a45c8774b))
* unify font hook domain presentation ([6aa7c06](https://github.com/Kwensiu/DPIS/commit/6aa7c0688c1614bdac2d052e04c180e8bef2f83e))


### Bug Fixes

* avoid boot-time runtime mirror false positives ([c2ea712](https://github.com/Kwensiu/DPIS/commit/c2ea71212685793618cbc97e3abccc1301c9a245))
* clarify font hook domain normalization ([b2fc790](https://github.com/Kwensiu/DPIS/commit/b2fc790451c0e2c44077e714609b03effe65be6e))
* harden VVeChat DPI runtime route handling ([b10bdeb](https://github.com/Kwensiu/DPIS/commit/b10bdeb1b59fa0d0b9d9ed911ffaa2ac9f963127))
* install modern system_server hooks during system startup ([c8f73a6](https://github.com/Kwensiu/DPIS/commit/c8f73a6c80a6bc3c9c3bb2b851da1aadca06720d))
* stabilize system font metrics scaling in resources read path ([#88](https://github.com/Kwensiu/DPIS/issues/88)) ([1574ffa](https://github.com/Kwensiu/DPIS/commit/1574ffa2a71a2441f8b9accce4a0c9fe6d84c230))


### UI

* help page adjust ([0f1106f](https://github.com/Kwensiu/DPIS/commit/0f1106f8ad0ebdab1b69fca931c406be842c7f46))

## [1.12.3](https://github.com/Kwensiu/DPIS/compare/v1.12.2...v1.12.3) (2026-06-14)


### ci

* update workflows ([ad711de](https://github.com/Kwensiu/DPIS/commit/ad711de8dd545738eb8eff5b3e210bd1c42f6c6f))


### Features

* polish donate page and secondary screen headers ([6c9a596](https://github.com/Kwensiu/DPIS/commit/6c9a596dbf2f613cfff6c5237cad78c7cf53ab2e))

## [1.12.2](https://github.com/Kwensiu/DPIS/compare/v1.12.1...v1.12.2) (2026-06-12)


### Bug Fixes

* make local config authoritative for runtime delivery ([8939422](https://github.com/Kwensiu/DPIS/commit/89394224a4137c06f8ec213c3502a26d7b1d0101))


### Refactoring

* rename Xposed build tracks to modern and legacy ([d22608a](https://github.com/Kwensiu/DPIS/commit/d22608ae1162ecb3df3aba5e86542d510f57bdbc))

## [1.12.1](https://github.com/Kwensiu/DPIS/compare/v1.12.0...v1.12.1) (2026-06-12)


### Features

* add donation page ([0adb479](https://github.com/Kwensiu/DPIS/commit/0adb4796026664b9f2f2cce606ca2c577908ee0f))
* add independent WeChat DisplayMetrics DPI route ([97cb839](https://github.com/Kwensiu/DPIS/commit/97cb8393847ff6372f960f97cc1545b5ac5da8ed))
* add system font size tool ([#78](https://github.com/Kwensiu/DPIS/issues/78)) ([5d594b6](https://github.com/Kwensiu/DPIS/commit/5d594b6150e8581e1bf1bbdaf10dc1958521bf6a))
* polish main workspace navigation and config UI ([97d5e96](https://github.com/Kwensiu/DPIS/commit/97d5e96df61ab6e6ae17dfe7759d26b6db64a753))


### Bug Fixes

* preserve draft mode intent without runtime values ([6ff6185](https://github.com/Kwensiu/DPIS/commit/6ff6185a19c8ef160bbc9c6ffae84ac05d970f83))
* refine app list spacing semantics ([d453f73](https://github.com/Kwensiu/DPIS/commit/d453f73891c6a58a2d94c5f156aad6126881db95))
* resolve home activation from xposed load state ([162be98](https://github.com/Kwensiu/DPIS/commit/162be987915e9e9e3a5ffc709697d2b3666d224c))


### Refactoring

* make app config local authoritative ([#81](https://github.com/Kwensiu/DPIS/issues/81)) ([7d7b33a](https://github.com/Kwensiu/DPIS/commit/7d7b33a11e1d6d4b15a4d63e423d321b0a2d0a3a))

## [1.12.0](https://github.com/Kwensiu/DPIS/compare/v1.11.1...v1.12.0) (2026-06-07)


### Features

* add template workspace and reusable app config templates ([#74](https://github.com/Kwensiu/DPIS/issues/74)) ([2e1abef](https://github.com/Kwensiu/DPIS/commit/2e1abef99b01a77c93487be96ff4c15e9ede830b))
* add VVeChat target field route ([#75](https://github.com/Kwensiu/DPIS/issues/75)) ([9d0df63](https://github.com/Kwensiu/DPIS/commit/9d0df63f6688f3aa6ea10b9eebe1341755cc226e))


### Bug Fixes

* adapt quick template target selection ([081dde4](https://github.com/Kwensiu/DPIS/commit/081dde4c03cb5cbbb84d75bbb0ddc8a75db7ebd9))
* avoid API 34 Stream toList in font domains ([b37618d](https://github.com/Kwensiu/DPIS/commit/b37618d853ea7034c07bd1f012cb0bd04b14791b))
* improve quick template target selection ([e9c09ba](https://github.com/Kwensiu/DPIS/commit/e9c09bab7b1bbb48238cf092eca88d2997cc56ec))
* preserve viewport scaling in browser small windows ([d4ac40b](https://github.com/Kwensiu/DPIS/commit/d4ac40b7506dc8cd39c00b26251e7073ed83ad00))
* stabilize system font mutation scheduling ([#77](https://github.com/Kwensiu/DPIS/issues/77)) ([3ce71e1](https://github.com/Kwensiu/DPIS/commit/3ce71e1bf828d8ce01d6dd2b54c07e720e3d2026))

## [1.11.1](https://github.com/Kwensiu/DPIS/compare/v1.11.0...v1.11.1) (2026-05-31)


### Bug Fixes

* restore launch activity viewport system route ([#72](https://github.com/Kwensiu/DPIS/issues/72)) ([1aec464](https://github.com/Kwensiu/DPIS/commit/1aec46463fb76b29bca9969e2b4fce78559698c5))

## [1.11.0](https://github.com/Kwensiu/DPIS/compare/v1.10.0...v1.11.0) (2026-05-27)


### Features

* add app sheet advanced action hint ([d8b05c2](https://github.com/Kwensiu/DPIS/commit/d8b05c2fa88553bfc1972229c9cbc046966c2569))
* add viewport relative scale route ([#66](https://github.com/Kwensiu/DPIS/issues/66)) ([85a261e](https://github.com/Kwensiu/DPIS/commit/85a261e3478d4ed8da5b4d08ec683d5f39be1b92))


### Bug Fixes

* align app config sheet defaults ([e7ee271](https://github.com/Kwensiu/DPIS/commit/e7ee271be3b863bea089e7901fa40cfee1d60344))
* harden viewport scale config and hook routing ([#68](https://github.com/Kwensiu/DPIS/issues/68)) ([b10ed3f](https://github.com/Kwensiu/DPIS/commit/b10ed3f424089f802cd2747c1b76d233275c9a5e))
* persist list filters and trim backups ([b1ead23](https://github.com/Kwensiu/DPIS/commit/b1ead231bdb370863e4a08e033fc28fee7d795f5))


### UI

* refine shared page chrome and dimension naming ([c38ba83](https://github.com/Kwensiu/DPIS/commit/c38ba83590bfff728f7de7c894edc9421b924078))

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
