FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

RUN groupadd --system simulator && useradd --system --gid simulator simulator
WORKDIR /app
COPY pyproject.toml .
COPY src src
RUN pip install --no-cache-dir .
USER simulator
EXPOSE 8000
HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8000/metrics', timeout=2)"
ENTRYPOINT ["robot-simulator"]

