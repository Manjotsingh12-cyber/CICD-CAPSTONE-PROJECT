def call(String buildNumber) {
    def version = "1.0.${buildNumber}"
    return version
}