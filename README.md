# iDex

[简体中文](#简体中文) · [English](#english)

关都 151 的口袋图鉴，做给可折叠 Android 设备。对准、扫描，假装研究经费已经批下来了。

Kotlin · Jetpack Compose · Material 3 · English / 简体中文 / 日本語 · 完全离线

这是 [iDex](https://github.com/zihang-huang/iDex)（MIT，Zihang Huang）的独立 Android 移植：图鉴数据、像素图、叫声和交互沿用上游，界面用 Kotlin 重写。非官方粉丝作品，与任天堂、Creatures、GAME FREAK、The Pokémon Company 无关，也未获其认可。

## 简体中文

### 用 Android Studio 打开

1. 安装 Android Studio（能使用 Android Gradle Plugin 8.13 的版本）和 JDK 17 或 21。
2. 用 SDK Manager 安装 **Android SDK Platform 36** 和 **Build-Tools 36**。本工程 `compileSdk` / `targetSdk` 为 36，`minSdk` 为 26。
3. 用 Android Studio 打开本目录，等 Gradle Sync 完成。
4. 选一台手机、平板，或折叠屏模拟器（例如 Pixel Fold），运行 `app`。

不需要账号、API key、服务器或运行时下载。应用没有 `INTERNET` 权限。

模拟器或还没允许相机时，取景器里是标注好的样本：**蚊香君 #061**。点 **扫描** 会把这张像素图送进设备上的模型。真机上可以点相机，或从相册导入一张照片。照片只留在内存里。

### 命令行构建

在仓库根目录：

```sh
export ANDROID_HOME="$HOME/Android/Sdk"   # Android Studio 的 SDK 路径
./gradlew :app:assembleDebug
```

单元测试（图鉴检索、识别门槛、宽屏判断）：

```sh
./gradlew :app:testDebugUnitTest
```

本次在此环境（JDK 21，`ANDROID_HOME` 指向已安装 `platforms;android-36` 与 Build-Tools 36 的 SDK）执行：

```text
$ ./gradlew :app:assembleDebug :app:testDebugUnitTest
BUILD SUCCESSFUL in 2s
43 actionable tasks: 7 executed, 36 up-to-date
```

依赖已经在本机缓存时，上面就是这次命令的原文。`testDebugUnitTest` 里 `CatalogAndScoringTest` 为 4 个用例、0 失败。调试 APK 在 `app/build/outputs/apk/debug/app-debug.apk`。第一次拉取 Gradle 和依赖时会更久。

### 折叠屏和宽屏

单手竖屏是一条流程：扫描器 → **打开记录** → 图鉴。返回键按设置、图鉴、记录的顺序退回。

下面任一条件成立时，左边（或桌面姿态的上半）是图鉴记录或浏览，右边（或下半）是扫描器，中间有一条铰链：

- 窗口宽度达到 Material 3 的 **Expanded**（约 840dp）且高度至少 480dp。
- 没有折叠传感器，但宽度至少 680dp、高度至少 500dp（平板、可调整尺寸的模拟器）。
- `FoldingFeature` 为竖直方向，并且处于展开、半折或 `isSeparating`，同时宽度至少 600dp。

竖直铰链如果落在窗口内部，分割线跟铰链边界走，避免内容压在折痕上；否则左右各半，中间画一条 18dp 的铰链。水平且 `isSeparating` 的桌面姿态（高度至少 600dp）改成上下两栏。普通手机横过来通常不够高，仍保持单栏，避免把两边挤扁。

Android Studio 里可以用 Pixel Fold，或把可调整尺寸的设备拉到大约 900×700dp，预览这套双栏。这里没有折叠屏模拟器，双栏规则由 `PaneLayout` 的单元测试覆盖；真机铰链仍需在设备上确认。

### 识别：在设备上跑，以及它不保证什么

iOS 版附带的是本地训练的 **EfficientNet-B0 FP32 Core ML**。贡献者对那份训练权重的授权是 **CC BY-NC-SA 4.0**（见 `NOTICE.md` 和 `app/src/main/assets/legal/UPSTREAM_MODEL_NOTICE.md`）。PyTorch checkpoint 没有放进 iDex 仓库，Core ML 包也不能在 Android 上执行，所以 **本应用不附带那份 Core ML 模型**。

Android 使用 iDex 自己的 `scripts/model/convert_model.py` 所指向的同一套 151 类权重，已经是 ONNX：

- 文件：`app/src/main/assets/model/kanto-classifier.onnx`
- 来源：[BiernyVR/pokemon-classifier-mobilenetv3](https://huggingface.co/BiernyVR/pokemon-classifier-mobilenetv3) 的 `pokemon_classifier.onnx`
- SHA-256：`ede506ccbded53eb7867f081417d721dbd32e3aa0f12f73683307f3f6b81d69f`
- 模型卡声明 **MIT**。这是上传者给出的软件许可，不是 iDex EfficientNet 的 CC BY-NC-SA，也不等于宝可梦形象本身可以商用。
- 运行时：[ONNX Runtime Android](https://onnxruntime.ai/)。输入为 224×224 RGB，应用内做 ImageNet 归一化（mean `0.485, 0.456, 0.406`，std `0.229, 0.224, 0.225`）。输出是 151 维原始 logits，顺序与 `pokemon_labels.json` 一致。
- 接受规则与 iDex 相同：温度 1 的 softmax ≥ 0.25，且第一名与第二名的 logit 差 ≥ 0.5。只有被接受的结果才会写入发现记录并播放叫声、朗读。低置信度仍显示最可能的精灵和分数。

`scripts/verify_classifier.py` 在本机用同一份 ONNX 跑过全部 151 张像素图：top-1 为 151/151，全部被接受，没有认错后仍被接受的情况。纯白和纯黑图不会被接受（白图最高约 2.5%，margin 0.03）。这是精灵图冒烟测试，不是相机准确率。

限制：

- 没有“不是宝可梦”这一类。无关物体、其他世代的宝可梦仍可能被认成关都宝可梦。
- 分数不是校准过的概率。
- 和 iOS 上的 EfficientNet-B0 不是同一套权重，实拍准确率没有在真机上测量。
- 系统朗读依赖设备上已安装的 TTS 语音；没有对应语言时，Android 可能改用默认语音或保持沉默。

### 语言和本地进度

首次启动跟随系统语言（中文、日文，否则英文）。设置里可以改成 English、简体中文或日本語，选择会记住。界面、图鉴正文、属性名和朗读都跟着走。按名字搜索时三种语言都算；按属性搜索只用当前语言。也可以搜编号（`61`、`061`、`#061`）。

发现记录、上次查看的编号、语言和音效开关写在本机 `SharedPreferences`（`idex`）。没有账号。

### 许可

原创 Kotlin / Gradle / 校验脚本使用 [MIT](LICENSE)。精灵图、图鉴文本、叫声、字体和两个模型的范围见 [NOTICE.md](NOTICE.md)。字体的 OFL 全文在 `app/src/main/assets/fonts/`。

## English

A pocket Pokédex for foldable Android phones. Point, scan, and pretend the research grant cleared.

This is an independent Kotlin port of [iDex](https://github.com/zihang-huang/iDex) (MIT, Zihang Huang). Catalog data, sprites, cries, and the field-unit behavior follow that app. The UI is new Compose code. Unofficial fan work. Not affiliated with or endorsed by Nintendo, Creatures, GAME FREAK, or The Pokémon Company.

### Open it in Android Studio

1. Install Android Studio (a release that supports Android Gradle Plugin 8.13) and JDK 17 or 21.
2. Install **Android SDK Platform 36** and **Build-Tools 36**. This project sets `compileSdk` / `targetSdk` to 36 and `minSdk` to 26.
3. Open this directory and let Gradle sync.
4. Run `app` on a phone, a tablet, or a foldable emulator such as Pixel Fold.

No account, API key, server, or runtime download. The manifest does not request `INTERNET`.

Until the camera is available, the viewfinder shows a labelled sample: **Poliwhirl #061**. **Scan** runs that sprite through the on-device model. On a device, use the camera button or import a photo. Photos stay in memory.

### Command-line build

From the repository root:

```sh
export ANDROID_HOME="$HOME/Android/Sdk"   # the SDK path Android Studio uses
./gradlew :app:assembleDebug
```

Unit tests for catalog search, the acceptance rule, and the wide-layout predicate:

```sh
./gradlew :app:testDebugUnitTest
```

Ran in this workspace (JDK 21, `ANDROID_HOME` pointing at an SDK with `platforms;android-36` and Build-Tools 36):

```text
$ ./gradlew :app:assembleDebug :app:testDebugUnitTest
BUILD SUCCESSFUL in 2s
43 actionable tasks: 7 executed, 36 up-to-date
```

That is the verbatim result with Gradle and Maven already cached. `CatalogAndScoringTest` reported 4 tests and 0 failures. The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. A first run also downloads the Gradle distribution and dependencies.

### Foldables and wide screens

A phone in portrait is one pane: scanner, then **Open record**, then the archive. Back closes settings, then the catalog, then the record.

The archive (or the browser) sits beside the scanner when any of these is true:

- Material 3 width is **Expanded** (about 840dp) and the window is at least 480dp tall.
- There is no fold sensor, but the window is at least 680dp wide and 500dp tall.
- A vertical `FoldingFeature` is flat, half-opened, or `isSeparating`, and the width is at least 600dp.

A vertical hinge whose bounds sit inside the window becomes the split, so text is not drawn across the crease. Otherwise the panes share the width around an 18dp spine. A horizontal separating fold (tabletop, height at least 600dp) stacks the archive above the scanner. A normal phone in landscape is usually too short and stays on one pane.

In Android Studio, use a Pixel Fold emulator or stretch a resizable device to about 900×700dp. This workspace has no foldable emulator. `PaneLayout` unit tests cover the size rules; a physical hinge still needs a device.

### Recognition, on device, and what it does not promise

The iOS app ships a locally trained **EfficientNet-B0 FP32 Core ML** model. Contributor rights in that trained model are offered under **CC BY-NC-SA 4.0** (see `NOTICE.md` and `app/src/main/assets/legal/UPSTREAM_MODEL_NOTICE.md`). The PyTorch checkpoint is not in the iDex git repo, and the Core ML package does not run on Android. **This app does not redistribute that Core ML model.**

Android bundles the ONNX file from the checkpoint family iDex's own `scripts/model/convert_model.py` already points at:

- File: `app/src/main/assets/model/kanto-classifier.onnx`
- Source: `pokemon_classifier.onnx` from [BiernyVR/pokemon-classifier-mobilenetv3](https://huggingface.co/BiernyVR/pokemon-classifier-mobilenetv3)
- SHA-256: `ede506ccbded53eb7867f081417d721dbd32e3aa0f12f73683307f3f6b81d69f`
- The model card declares **MIT**. That is the uploader's software license. It is not the CC BY-NC-SA offer on iDex's EfficientNet, and it does not clear Pokémon character art for commercial use.
- Runtime: ONNX Runtime for Android. Input is 224×224 RGB. The app applies ImageNet normalization (mean `0.485, 0.456, 0.406`, std `0.229, 0.224, 0.225`). Output is 151 raw logits in `pokemon_labels.json` order.
- Acceptance matches iDex: temperature-1 softmax at least 0.25 and a logit margin of at least 0.5. Only an accepted match is stored as a discovery and followed by the cry and spoken entry. A weaker match still shows the best sprite and its score.

`scripts/verify_classifier.py` ran this ONNX file on all 151 bundled sprites: top-1 was 151/151, every sprite was accepted, and none was accepted under the wrong id. Plain white and black images were not accepted (white peaked near 2.5% with margin 0.03). That is a sprite smoke test, not a camera benchmark.

Limits:

- There is no trained “not a Pokémon” class. Unrelated objects and non-Kanto species can still match.
- Scores are not calibrated probabilities.
- These are not the iOS EfficientNet-B0 weights. Camera accuracy was not measured on a physical device here.
- Spoken entries use the on-device TTS engine. A missing language pack can fall back or stay silent.

### Language and the local log

The first launch follows the system language (Chinese, Japanese, otherwise English). Settings can switch to English, 简体中文, or 日本語, and the choice is kept. Names are searchable in all three languages; type search uses the selected language. Numbers match `61`, `061`, and `#061`.

Discoveries, the last viewed species, language, and the sound toggle live in on-device `SharedPreferences` (`idex`). No account.

### License

Original Kotlin, Gradle, and the checker script are [MIT](LICENSE). Sprites, catalog text, cries, fonts, and both models are scoped in [NOTICE.md](NOTICE.md). The OFL texts sit beside the fonts in `app/src/main/assets/fonts/`.
