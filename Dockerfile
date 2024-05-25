FROM continuumio/anaconda3:2024.02-1

WORKDIR /

RUN conda install -c conda-forge 'ffmpeg<7'

RUN pip3 install --no-cache-dir \
    torch \
    torchaudio \
    transformers

RUN pip3 install --no-cache-dir \
    runpod

ENV PYTHONPATH "${PYTHONPATH}:/workspace/repository/library"

WORKDIR /workspace