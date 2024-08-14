from transformers import AutoFeatureExtractor, ASTForAudioClassification


MODEL_PATH = "MIT/ast-finetuned-audioset-10-10-0.4593"


extractor = AutoFeatureExtractor.from_pretrained(MODEL_PATH)
model = ASTForAudioClassification.from_pretrained(MODEL_PATH)
