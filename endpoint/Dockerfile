FROM pytorch/pytorch:2.3.0-cuda12.1-cudnn8-runtime

WORKDIR /

RUN conda install -c conda-forge 'ffmpeg<7'

RUN pip3 install --no-cache-dir \
    transformers \
    runpod

WORKDIR /workspace

COPY endpoint/preload.py /workspace/.
RUN python3 preload.py

COPY library /workspace/library
COPY endpoint/handler.py /workspace/.

ENV PYTHONPATH "${PYTHONPATH}:/workspace/library"

CMD [ "python", "-u", "handler.py" ]
