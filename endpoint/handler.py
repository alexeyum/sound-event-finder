import time

import torch
import torchaudio
from transformers import AutoFeatureExtractor, ASTForAudioClassification
import runpod

import event_finder
import preprocess


DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")
CHUNK_LENGTH_SEC = 10
PROBABILITY_THRESHOLD = 0.2
MODEL_PATH = "MIT/ast-finetuned-audioset-10-10-0.4593"
FORMAT = "mp3"


def _load_model():
    load_start = time.time()
    extractor = AutoFeatureExtractor.from_pretrained(MODEL_PATH)
    model = ASTForAudioClassification.from_pretrained(MODEL_PATH).to(DEVICE)
    load_time = time.time() - load_start

    return model, extractor, load_time


def _make_features(extractor, waveform, sampling_rate):
    chunks = event_finder.chunk_audio(waveform, sampling_rate, chunk_length_sec=CHUNK_LENGTH_SEC)

    extract_start = time.time()
    features = extractor(chunks, sampling_rate, return_tensors="pt").to(DEVICE)
    extract_time = time.time() - extract_start

    return features, extract_time


def _inference(features):
    inference_start = time.time()
    with torch.no_grad():
        probs = torch.sigmoid(model(**features).logits)
    inference_time = time.time() - inference_start
    return probs, inference_time


def find_sound_events(job):
    job_input = job["input"]
    if "audio" not in job_input:
        return {"error": "No audio found."}
    if "base64" not in job_input["audio"]:
        return {"error": "Only base64 format for audio is available for now."}

    model, extractor, load_time = _load_model()
    sampling_rate = extractor.sampling_rate

    finder = event_finder.EventFinder(
        model.config,
        chunk_length_sec=CHUNK_LENGTH_SEC,
        probability_threshold=PROBABILITY_THRESHOLD)

    waveform_raw, source_sampling_rate = preprocess.load_audio_from_base64(
        job_input["audio"]["base64"], format=FORMAT)
    waveform = preprocess.convert_audio(waveform_raw, source_sampling_rate,
                                        channels="mono", sampling_rate=sampling_rate)

    features, extract_time = _make_features(extractor, waveform, sampling_rate)

    probs, inference_time = _inference(features)

    events = finder(probs)

    result = {
            "events": events,
            "time": {
                "load_model": load_time,
                "extract_features": extract_time,
                "inference": inference_time,
            }
        }
    return result


if __name__ == "__main__":
    runpod.serverless.start({"handler": find_sound_events})
