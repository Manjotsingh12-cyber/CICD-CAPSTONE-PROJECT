import os
from flask import Flask, jsonify, render_template

app = Flask(__name__)

VERSION = os.environ.get("APP_VERSION", "dev")
GIT_SHA = os.environ.get("GIT_SHA", "unknown")
BUILD_NUMBER = os.environ.get("BUILD_NUMBER", "unknown")
ENVIRONMENT = os.environ.get("APP_ENV", "local")

@app.route("/health")
def health():
    return jsonify(status="ok"), 200

@app.route("/ready")
def ready():
    return jsonify(status="ready"), 200

@app.route("/version")
def version():
    return jsonify(
        version=VERSION,
        git_sha=GIT_SHA,
        build_number=BUILD_NUMBER,
        environment=ENVIRONMENT,
    ), 200

@app.route("/")
def dashboard():
    return render_template(
        "dashboard.html",
        version=VERSION,
        git_sha=GIT_SHA,
        build_number=BUILD_NUMBER,
        environment=ENVIRONMENT,
    )

@app.after_request
def add_security_headers(response):
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["Content-Security-Policy"] = "default-src 'self'"
    response.headers["Permissions-Policy"] = "geolocation=(), microphone=(), camera=()"
    response.headers["Cache-Control"] = "no-store"
    return response

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8000)