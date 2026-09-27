def call(String url, int maxAttempts = 5) {
    for (int i = 1; i <= maxAttempts; i++) {
        def status = sh(script: "curl -sf ${url}", returnStatus: true)
        if (status == 0) {
            echo "Health check passed on attempt ${i}"
            return true
        }
        echo "Health check attempt ${i} failed, retrying..."
        sleep 2
    }
    echo "Health check failed after ${maxAttempts} attempts"
    return false
}