# Sound Event Finder

A mobile app that finds various sounds with timecodes in an audio file using AI. 
It can detect sounds from over 500 classes, including vehicle sounds (e.g. car engine,
train horn, helicopter), animal sounds (e.g. dog bark, pig oink, crow caw), 
human sounds (e.g. speech, clapping, baby crying), and many more. The app assigns time
intervals for each sound event, and allow overlapping intervals for different events.


## Features

- **Predictions using AI:** sound classificiation using [Audio Spectogram Transformer](https://huggingface.co/MIT/ast-finetuned-audioset-10-10-0.4593) model trained on [AudioSet](https://research.google.com/audioset/) data
- **API for core functionality:** all main features accessible through a RESTful API endpoint
- **Android app:** user-friendly interface for API as a mobile application


## Demo

https://github.com/user-attachments/assets/dd58a630-3c2a-43dd-9a9f-858ea18b1c2d


## Tech Stack

- **HuggingFace transformers** for sound classification
- **Python** and **PyTorch** for backend sound processing
- **Docker** and [**runpod**](rundpod.io) for creating API endpoint
- **Java** and **gradle** for mobile app
- **pytest** and **junit** for testing


## Usage

TODO

### Repository Structure

### API

### Mobile App

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.


## Acknowledgements

This project uses several open source components, please see [Credits.md](Credits.md) file for details.

## Future Work

Here are some plans for the future versions:

- Analyse sound during its recording, send notifications depending on specific sound events
- ...