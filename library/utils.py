from base64 import b64decode
from contextlib import contextmanager
from io import BytesIO
import time

import torch
import torchaudio


class TimingManager:

    """A class to manage and record the timing of various operations.

    This class provides a context manager for timing code blocks and stores 
    the elapsed time for each operation in a dictionary.

    Methods:
        timer(name):
            A context manager to time a block of code and store the elapsed time 
            under the given name.
        get_timing_dict():
            Returns the dictionary of recorded timings.
    """
    
    def __init__(self):
        self._timing_dict = {}

    @contextmanager
    def timer(self, name):

        """Context manager to time a block of code.

        Args:
            name (str): The name under which the elapsed time will be stored.

        Usage:
            with timing_manager.timer('operation_name'):
                # code block to time
        """

        start_time = time.time()
        yield
        elapsed_time = time.time() - start_time
        self._timing_dict[name] = elapsed_time

    def get_timing_dict(self):
        """Get the dictionary of recorded timings.

        Returns:
            dict: A dictionary where keys are operation names and values 
                  are the elapsed time for each operation.
        """
        return self._timing_dict


def _downmix_to_mono(waveform):
    return waveform.mean(dim=0)


def convert_audio(waveform, source_sampling_rate, channels="mono", sampling_rate=16000):

    """Convert an audio waveform to the proper format for further processing.

    This function downmixes the waveform to mono if specified and resamples 
    it to the target sampling rate if different from the source sampling rate.

    Args:
        waveform (torch.Tensor): The input audio waveform.
        source_sampling_rate (int): The original sampling rate of the waveform.
        channels (str): The target number of channels ("mono" is the only supported option).
        sampling_rate (int): The target sampling rate.

    Returns:
        torch.Tensor: The converted audio waveform.

    Raises:
        ValueError: If the parameters are incorrect.
    """

    if channels == "mono":
        waveform = _downmix_to_mono(waveform)
    else:
        raise ValueError("Only mono `channels` is implemented")

    if sampling_rate != source_sampling_rate:
        waveform = torchaudio.functional.resample(waveform, source_sampling_rate, sampling_rate)

    return waveform


def load_audio_from_base64(data, format):
    
    """Load an audio waveform from a base64 encoded string.

    Args:
        data (str): The base64 encoded audio data.
        format (str): The format of the audio file (e.g., "mp3", "wav").

    Returns:
        tuple: A tuple containing the audio waveform (torch.Tensor) and the sampling rate (int).
    """

    byte_io = BytesIO(b64decode(data))
    return torchaudio.load(byte_io, format=format)


def chunk_audio(waveform, sampling_rate, chunk_length_sec):
    
    """Split an audio waveform into chunks of a specified length.

    This function divides the audio waveform into non-overlapping chunks of the 
    specified length in seconds.

    Args:
        waveform (torch.Tensor): The input audio waveform.
        sampling_rate (int): The sampling rate of the waveform.
        chunk_length_sec (int): The length of each chunk in seconds.

    Returns:
        list: A list of numpy arrays, each representing a chunk of the waveform.
    """

    chunk_length = int(chunk_length_sec * sampling_rate)
    # Note: c.numpy() is necessary for feature extractor
    chunks = [c.numpy() for c in torch.split(waveform, chunk_length)]
    return chunks
