#!/usr/bin/env python3
"""On-device classifier smoke check. Runs the bundled ONNX model on the 151 sprites.

No network. Requires onnxruntime, pillow, and numpy.
"""

import json
from pathlib import Path

import numpy as np
from PIL import Image
import onnxruntime as ort

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT / "app/src/main/assets/model/kanto-classifier.onnx"
LABELS = json.loads((ROOT / "app/src/main/assets/catalog/pokemon_labels.json").read_text())
SPRITES = ROOT / "app/src/main/assets/sprites"
MEAN = np.array([0.485, 0.456, 0.406], np.float32)
STD = np.array([0.229, 0.224, 0.225], np.float32)


def prepare(image: Image.Image) -> np.ndarray:
    image = image.convert("RGBA")
    background = Image.new("RGBA", image.size, (255, 255, 255, 255))
    image = Image.alpha_composite(background, image).convert("RGB")
    side = min(image.size)
    left = (image.width - side) // 2
    top = (image.height - side) // 2
    image = image.crop((left, top, left + side, top + side)).resize((224, 224), Image.Resampling.BILINEAR)
    array = np.asarray(image).astype(np.float32) / 255.0
    array = (array - MEAN) / STD
    return np.transpose(array, (2, 0, 1))[None]


def decide(logits: np.ndarray):
    logits = logits.reshape(-1)
    order = np.argsort(logits)[::-1]
    shifted = np.exp(logits - logits.max())
    probabilities = shifted / shifted.sum()
    margin = float(logits[order[0]] - logits[order[1]])
    confidence = float(probabilities[order[0]])
    accepted = confidence >= 0.25 and margin >= 0.5
    return int(LABELS[int(order[0])]["id"]), confidence, margin, accepted


def main():
    session = ort.InferenceSession(str(MODEL), providers=["CPUExecutionProvider"])
    name = session.get_inputs()[0].name
    correct = accepted_right = accepted_wrong = 0
    for dex_id in range(1, 152):
        logits = session.run(None, {name: prepare(Image.open(SPRITES / f"{dex_id:03d}.png"))})[0]
        top, _confidence, _margin, accepted = decide(logits)
        correct += top == dex_id
        accepted_right += accepted and top == dex_id
        accepted_wrong += accepted and top != dex_id
    for blank, color in (("white", (255, 255, 255)), ("black", (0, 0, 0))):
        logits = session.run(None, {name: prepare(Image.new("RGB", (224, 224), color))})[0]
        top, confidence, margin, accepted = decide(logits)
        print(f"{blank}: top={top:03d} confidence={confidence:.3f} margin={margin:.3f} accepted={accepted}")
    print(f"sprites top1 {correct}/151 accepted_right {accepted_right} accepted_wrong {accepted_wrong}")


if __name__ == "__main__":
    main()
