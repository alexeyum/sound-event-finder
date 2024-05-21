import torch


def chunk_audio(waveform, sampling_rate, chunk_length_sec):
    
    """Split audiofile into chunks of same length without overlap."""

    chunk_length = chunk_length_sec * sampling_rate
    # Note: c.numpy() is necessary for feature extractor
    chunks = [c.numpy() for c in torch.split(waveform, chunk_length)]
    return chunks


def _continuos_segments(values):

    """Find intervals of continuos `True` values in a boolean array."""

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


class EventFinder:
    def __init__(self, model_config, chunk_length_sec, probability_threshold):
        self.model_config = model_config
        self.chunk_length_sec = chunk_length_sec
        self.probability_threshold = probability_threshold

        # NOTE: this is temporary, will implement antology parsing later
        self.EVENT_CLASSES = [
            74,   # Dog
            137,  # Music
            300,  # Vehicle
            0,    # Speech
            117,  # Crow
            112,  # Bird vocalization, bird call, bird song
        ]

    def __call__(self, probabilities):
        events = []
        for class_ind in self.EVENT_CLASSES:
            class_name = self.model_config.id2label[class_ind]
            class_parts = probabilities[:, class_ind] >= self.probability_threshold
            for begin, end in _continuos_segments(class_parts):
                begin_sec = begin * self.chunk_length_sec
                end_sec = end * self.chunk_length_sec
                events.append((class_name, begin_sec, end_sec))

        # sort by the segment start
        events.sort(key=lambda e: e[1])

        return events
