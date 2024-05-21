from base64 import b64decode
from io import BytesIO

import torch
import torchaudio

import event_finder
import preprocess

import runpod


def find_sound_events(job):
    job_input = job["input"]

    if "audio" not in job_input:
        return {"error": "No audio found."}
    if "base64" not in job_input["audio"]:
        return {"error": "Only base64 format for audio is available for now."}

    b64_str = job_input["audio"]["base64"]
    byte_io = BytesIO(b64decode(b64_str))

    waveform_raw, source_sampling_rate = torchaudio.load(byte_io, format="wav")

    return {"Result": "OK"}


runpod.serverless.start({"handler": find_sound_events})