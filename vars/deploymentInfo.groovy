def call(Map config) {
    echo "=== Deployment Info ==="
    echo "Environment: ${config.environment}"
    echo "Port: ${config.port}"
    echo "Version: ${config.version}"
}