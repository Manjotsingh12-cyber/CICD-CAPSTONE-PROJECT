def call(String url, int attempts = 5) {
    writeFile file: 'http-check.sh', text: libraryResource('scripts/http-check.sh')
    def rc = 0
    withEnv(["CHECK_URL=${url}", "CHECK_ATTEMPTS=${attempts}"]) {
        rc = sh(script: 'sh http-check.sh "$CHECK_URL" "$CHECK_ATTEMPTS"', returnStatus: true)
    }
    return rc == 0
}