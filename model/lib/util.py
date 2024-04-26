import torch
import torchaudio


def downmix_to_mono(waveform):
    return waveform.mean(dim=0)


def preprocess_audio(waveform, sampling_rate, target_sampling_rate):
    waveform = downmix_to_mono(waveform)
    return torchaudio.functional.resample(waveform, sampling_rate, target_sampling_rate)


def chunk_audio(waveform, sampling_rate, chunk_length_sec):
    
    """Split audiofile into chunks of same length."""

    chunk_length = chunk_length_sec * sampling_rate
    chunks = [c.numpy() for c in torch.split(waveform, chunk_length)]
    return chunks


def continuos_segments(values):

    """Find intervals of `True` values in a boolean array."""

    start = -1
    for i in range(len(values)):
        if values[i]:
            if start == -1:
                start = i
        else:
            if start != -1:
                yield (start, i)
            start = -1
    if start != -1:
        yield (start, len(values))