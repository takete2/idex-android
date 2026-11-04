# Licensing notice

This Android app is an independent Kotlin / Jetpack Compose reimplementation of the fan-made iDex experience. It is not a copy of the Swift sources.

The original iDex application source and scripts are MIT, copyright (c) 2026 Zihang Huang: https://github.com/zihang-huang/iDex. That MIT grant does not cover the third-party material listed there, and it does not cover the material listed here.

## This repository's own code

Kotlin, Gradle, and the Python checker under `scripts/verify_classifier.py` are MIT, copyright (c) 2026 iDex Android contributors. See [LICENSE](LICENSE).

## Not covered by that MIT grant

- **Pokémon catalog text.** `app/src/main/assets/catalog/pokemon-kanto.json` is the Kanto 151 extract bundled by iDex, fetched from PokéAPI on 24 October 2026 (names, genera, English / 简体中文 / 日本語 descriptions, types, height, weight, base stats). See `app/src/main/assets/legal/UPSTREAM_ATTRIBUTION.md`.
- **Sprites.** `app/src/main/assets/sprites/` are the Black & White pixel sprites iDex downloaded from the Pokémon Database. `app/src/main/assets/legal/sprite-sources.json` records each URL. https://pokemondb.net/sprites
- **Cries.** `app/src/main/assets/cries/` are the PokéAPI cries iDex converted to mono 22,050 Hz PCM WAV, from [PokeAPI/cries](https://github.com/PokeAPI/cries) at commit `b3a9044bb839b5e3152aaa536c7b463a0fcf0140`. The upstream notice is preserved at `app/src/main/assets/legal/PokeAPI-Cries-LICENSE.txt` and the per-file manifest at `app/src/main/assets/legal/cry-sources.json`. That notice says the audio contents are copyright The Pokémon Company and that the cries repository is distributed under CC0 1.0, which does not transfer the underlying audio rights.
- **Fonts.** Press Start 2P and Fusion Pixel (12 px proportional, Simplified Chinese and Japanese) stay under SIL OFL 1.1. License texts sit beside the files in `app/src/main/assets/fonts/`.
- **Labels.** `app/src/main/assets/catalog/pokemon_labels.json` is the 151-id order shared by iDex and the classifier (ids 1…151).

## Recognition model

iDex ships a locally trained **EfficientNet-B0 FP32 Core ML** package (`KantoClassifier.mlpackage`). Contributor rights in that trained model are offered under **CC BY-NC-SA 4.0**, as described in iDex's model notice (copied at `app/src/main/assets/legal/UPSTREAM_MODEL_NOTICE.md`). The PyTorch checkpoint is not in the iDex git repository, and the Core ML package does not run on Android. **This app does not redistribute that Core ML model or its weights.**

The Android build instead bundles `app/src/main/assets/model/kanto-classifier.onnx`:

- Source: [BiernyVR/pokemon-classifier-mobilenetv3](https://huggingface.co/BiernyVR/pokemon-classifier-mobilenetv3) `pokemon_classifier.onnx`
- SHA-256: `ede506ccbded53eb7867f081417d721dbd32e3aa0f12f73683307f3f6b81d69f`
- The model card declares **MIT**. That declaration is the uploader's software license. It does not clear Pokémon character artwork, and it is not the CC BY-NC-SA offer attached to iDex's EfficientNet.
- Architecture: DenseNet121, 151 Kanto classes, the same label order as iDex. iDex's own `scripts/model/convert_model.py` points at this repository's checkpoint family.
- ImageNet mean/std normalization is applied in the app. The network returns raw logits. Acceptance matches iDex: temperature-1 softmax ≥ 0.25 and logit margin ≥ 0.5.
- TorchVision's BSD notice is kept at `app/src/main/assets/legal/TorchVision-LICENSE.txt` because DenseNet's usual implementation lineage runs through TorchVision / timm. This app does not ship TorchVision code.

## Disclaimer

Unofficial fan work. Pokémon, the names, characters, sprites, and cries are trademarks and copyrights of Nintendo, Creatures, GAME FREAK, and The Pokémon Company. This project is not affiliated with or endorsed by them. These assets need a rights review before any public commercial distribution.
