from base64 import b64decode
from io import BytesIO

import torch
import torchaudio


def _downmix_to_mono(waveform):
    return waveform.mean(dim=0)


def convert_audio(waveform, source_sampling_rate, channels="mono", sampling_rate=16000):

    if channels == "mono":
        waveform = _downmix_to_mono(waveform)
    else:
        raise ValueError("Only mono `channels` is implemented")

    if sampling_rate != source_sampling_rate:
        waveform = torchaudio.functional.resample(waveform, source_sampling_rate, sampling_rate)

    return waveform


def load_audio_from_base64(data, format):
    byte_io = BytesIO(b64decode(data))
    return torchaudio.load(byte_io, format=format)


def chunk_audio(waveform, sampling_rate, chunk_length_sec):
    
    """Split audiofile into chunks of same length without overlap."""

    chunk_length = chunk_length_sec * sampling_rate
    # Note: c.numpy() is necessary for feature extractor
    chunks = [c.numpy() for c in torch.split(waveform, chunk_length)]
    return chunks
