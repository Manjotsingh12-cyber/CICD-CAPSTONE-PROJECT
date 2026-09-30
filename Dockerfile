FROM python:3.12-slim AS base

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .
COPY templates/ templates/

ARG GIT_SHA=unknown
ARG BUILD_NUMBER=unknown
ARG APP_VERSION=dev
ENV GIT_SHA=${GIT_SHA}
ENV BUILD_NUMBER=${BUILD_NUMBER}
ENV APP_VERSION=${APP_VERSION}
ENV APP_ENV=dev

RUN useradd -m appuser
USER appuser

EXPOSE 8000

CMD ["gunicorn", "--bind", "0.0.0.0:8000", "app:app"]