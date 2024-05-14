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