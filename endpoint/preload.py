from transformers import AutoFeatureExtractor, ASTForAudioClassification


extractor = AutoFeatureExtractor.from_pretrained("bookbot/distil-ast-audioset")
model = ASTForAudioClassification.from_pretrained("bookbot/distil-ast-audioset")