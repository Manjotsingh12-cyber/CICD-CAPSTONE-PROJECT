import os

os.environ["APP_VERSION"] = "test-version"
os.environ["GIT_SHA"] = "test-sha"
os.environ["BUILD_NUMBER"] = "123"
os.environ["APP_ENV"] = "test"

from app import app


def test_health():
    client = app.test_client()

    response = client.get("/health")

    assert response.status_code == 200
    assert response.get_json() == {"status": "ok"}


def test_ready():
    client = app.test_client()

    response = client.get("/ready")

    assert response.status_code == 200
    assert response.get_json() == {"status": "ready"}


def test_version():
    client = app.test_client()

    response = client.get("/version")

    assert response.status_code == 200

    data = response.get_json()

    assert data["version"] == "test-version"
    assert data["git_sha"] == "test-sha"
    assert data["build_number"] == "123"
    assert data["environment"] == "test"


def test_dashboard():
    client = app.test_client()

    response = client.get("/")

    assert response.status_code == 200