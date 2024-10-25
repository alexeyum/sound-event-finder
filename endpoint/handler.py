import json

import torch
import torchaudio
from transformers import AutoFeatureExtractor, ASTForAudioClassification
import runpod

import event_finder
import utils


DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")
MODEL_PATH = "MIT/ast-finetuned-audioset-10-10-0.4593"
ONTOLOGY_PATH = "ontology.json"

DEFAULT_CHUNK_LENGTH_SEC = 10
DEFAULT_PROBABILITY_THRESHOLD = 0.2


def make_config(job_input):

    """Create a configuration dictionary based on job input."""

    return {
        "chunk_length_sec": job_input.get("chunk_length_sec", 
                                          DEFAULT_CHUNK_LENGTH_SEC),
        "probability_threshold": job_input.get("probability_threshold", 
                                               DEFAULT_PROBABILITY_THRESHOLD),
    }


def process_audio_input(job_input, config):

    """Process the audio input from the job input."""

    if "audio" not in job_input:
        raise ValueError("No audio found.")
    if "base64" not in job_input["audio"]:
        raise ValueError("Only base64 encoding for audio is supported for now.")
    if "format" not in job_input["audio"]:
        raise ValueError("Audio format not specified.")
    if job_input["audio"]["format"] != "mp3":
        raise ValueError("Only mp3 format for audio is supported for now.")

    waveform_raw, source_sampling_rate = utils.load_audio_from_base64(
        job_input["audio"]["base64"], 
        format=job_input["audio"]["format"])

    waveform = utils.convert_audio(waveform_raw, source_sampling_rate,
                                   channels="mono", sampling_rate=config["sampling_rate"])

    return waveform


def load_model():    
    """Load the model and feature extractor."""
    extractor = AutoFeatureExtractor.from_pretrained(MODEL_PATH, local_files_only=True)
    model = ASTForAudioClassification.from_pretrained(MODEL_PATH, local_files_only=True).to(DEVICE)
    return model, extractor


def find_events(config, probabilities):

    """Create and return an EventFinder instance."""

    with open(ONTOLOGY_PATH, "r") as f:
        events_graph = json.load(f)

    finder = event_finder.EventFinder(
        config["model_config"].id2label,
        events_graph,
        chunk_length_sec=config["chunk_length_sec"],
        probability_threshold=config["probability_threshold"])

    return finder.find(probabilities)


def make_features(extractor, waveform, config):

    """Extract features from the audio waveform."""

    chunks = utils.chunk_audio(waveform, config["sampling_rate"], 
                               chunk_length_sec=config["chunk_length_sec"])

    # remove extra small chunks for the extractor
    chunks = [c for c in chunks if len(c) >= 2]

    features = extractor(chunks, config["sampling_rate"], return_tensors="pt").to(DEVICE)
    return features


def auto_chunk_length(config, waveform):

    """Automatically set up chunk length if specified in config."""

    MIN_CHUNK_LENGTH_SEC = 1.0
    MAX_CHUNK_LENGTH_SEC = 10.0
    CHUNKS_PER_AUDIO = 4.0

    if config["chunk_length_sec"] == 'auto':
        audio_length_sec = waveform.shape[0] / config["sampling_rate"]
        chunk_length_sec = audio_length_sec / CHUNKS_PER_AUDIO
        chunk_length_sec = min(chunk_length_sec, MAX_CHUNK_LENGTH_SEC)
        chunk_length_sec = max(chunk_length_sec, MIN_CHUNK_LENGTH_SEC)
        config["chunk_length_sec"] = chunk_length_sec


def inference(model, features):
    """Perform inference on the extracted features."""
    with torch.no_grad():
        probs = torch.sigmoid(model(**features).logits)
    return probs


def find_sound_events(job):
    try:
        timing_manager = utils.TimingManager()

        with timing_manager.timer("load_model"):
            model, extractor = load_model()

        config = make_config(job["input"])
        config["sampling_rate"] = extractor.sampling_rate
        config["model_config"] = model.config

        waveform = process_audio_input(job["input"], config)

        auto_chunk_length(config, waveform)

        with timing_manager.timer("extract_features"):
            features = make_features(extractor, waveform, config)

        with timing_manager.timer("inference"):
            probs = inference(model, features)

        with timing_manager.timer("find_events"):
            events = find_events(config, probs)

        result = {"events": events,
                  "time": timing_manager.get_timing_dict()}
        return result

    except ValueError as e:
        return {"error": str(e)}
    except Exception as e:
        return {"error": f"An unexpected error occurred: {str(e)}"}

if __name__ == "__main__":
    runpod.serverless.start({"handler": find_sound_events})
