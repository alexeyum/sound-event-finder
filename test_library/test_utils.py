import time
from io import BytesIO
from base64 import b64encode

import torch
import torchaudio
from torio.io import CodecConfig

import pytest

from utils import *


def test_timing_manager():
    tm = TimingManager()
    with tm.timer('sleep_0.01'):
        time.sleep(0.01)
    with tm.timer('noop'):
        pass
    timings = tm.get_timing_dict()
    assert 'sleep_0.01' in timings
    assert 'noop' in timings
    assert timings['sleep_0.01'] >= 0.01
    assert timings['noop'] >= 0


def test_convert_audio():
    torch.manual_seed(5849123)
    waveform = torch.rand(2, 48000)  # Stereo signal
    source_sampling_rate = 48000
    target_sampling_rate = 16000
    converted_waveform = convert_audio(
        waveform, source_sampling_rate, channels="mono", sampling_rate=target_sampling_rate)
    assert converted_waveform.shape[0] == int(
        waveform.shape[1] * target_sampling_rate / source_sampling_rate)


@pytest.fixture
def sine_wave():
    sampling_rate = 16000
    duration = 2  # in seconds
    frequency = 440  # A4 note in Hz
    t = torch.linspace(0, duration, int(sampling_rate * duration), dtype=torch.float32)
    waveform = 0.5 * torch.sin(2 * torch.pi * frequency * t).unsqueeze(0)
    return waveform, sampling_rate


def test_load_audio_from_base64_wav(sine_wave):
    waveform, sampling_rate = sine_wave
    buffered = BytesIO()
    torchaudio.save(buffered, waveform, sampling_rate, format="wav", bits_per_sample=32)
    buffered.seek(0)
    base64_data = b64encode(buffered.read()).decode("ascii")

    loaded_waveform, loaded_sampling_rate = load_audio_from_base64(base64_data, format="wav")
    assert loaded_sampling_rate == sampling_rate
    assert torch.allclose(loaded_waveform, waveform)


def test_chunk_audio():
    torch.manual_seed(5849123)
    waveform = torch.rand(48000)  # 1 second of mono audio at 48kHz
    sampling_rate = 48000
    chunk_length_sec = 1
    chunks = chunk_audio(waveform, sampling_rate, chunk_length_sec)
    assert len(chunks) == 1
    assert chunks[0].shape == (48000,)

def test_chunk_audio_multiple_chunks():
    torch.manual_seed(5849123)
    waveform = torch.rand(96000)  # 2 seconds of mono audio at 48kHz
    sampling_rate = 48000
    chunk_length_sec = 1
    chunks = chunk_audio(waveform, sampling_rate, chunk_length_sec)
    assert len(chunks) == 2
    assert all(chunk.shape == (48000,) for chunk in chunks)


if __name__ == '__main__':
    pytest.main()
