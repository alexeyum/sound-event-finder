from base64 import b64decode
from io import BytesIO
import time

import torch
import torchaudio

from transformers import AutoFeatureExtractor, ASTForAudioClassification

import runpod

import event_finder
import preprocess


def find_sound_events(job):
    job_input = job["input"]

    if "audio" not in job_input:
        return {"error": "No audio found."}
    if "base64" not in job_input["audio"]:
        return {"error": "Only base64 format for audio is available for now."}

    DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")

    load_start = time.time()
    extractor = AutoFeatureExtractor.from_pretrained("bookbot/distil-ast-audioset")
    model = ASTForAudioClassification.from_pretrained("bookbot/distil-ast-audioset").to(DEVICE)
    load_time = time.time() - load_start
    chunk_length_sec = 10
    sampling_rate = extractor.sampling_rate
    finder = event_finder.EventFinder(
        model.config,
        chunk_length_sec=chunk_length_sec,
        probability_threshold=0.2)

    b64_str = job_input["audio"]["base64"]
    byte_io = BytesIO(b64decode(b64_str))
    waveform_raw, source_sampling_rate = torchaudio.load(byte_io, format="mp3")
    waveform = preprocess.convert_audio(waveform_raw, source_sampling_rate,
                                        channels="mono", sampling_rate=sampling_rate)

    chunks = event_finder.chunk_audio(waveform, sampling_rate, chunk_length_sec=chunk_length_sec)

    extr_start = time.time()
    features = extractor(chunks, sampling_rate, return_tensors="pt").to(DEVICE)
    extr_time = time.time() - extr_start

    inf_start = time.time()
    with torch.no_grad():
        probs = torch.sigmoid(model(**features).logits)
    inf_time = time.time() - inf_start

    events = finder(probs)

    return {
            "events": events,
            "time": {
                "load_models": load_time,
                "extract_features": extr_time,
                "inference": inf_time,
            }
        }


runpod.serverless.start({"handler": find_sound_events})