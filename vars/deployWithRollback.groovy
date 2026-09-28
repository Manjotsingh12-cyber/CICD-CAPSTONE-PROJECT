// Deploys a container, verifies it, records last-known-good, rolls back on failure.
// Required: name, image, hostPort, containerPort
// Optional: healthPath (default '/'), attempts (default 5)
def call(Map cfg) {
    ['name', 'image', 'hostPort', 'containerPort'].each { k ->
        if (!cfg[k]) { error("deployWithRollback: missing '${k}'") }
    }
    String healthPath = cfg.get('healthPath', '/')
    int attempts = cfg.get('attempts', 5)
    String url = "http://localhost:${cfg.hostPort}${healthPath}"

    withEnv([
        "D_NAME=${cfg.name}",
        "D_IMAGE=${cfg.image}",
        "D_HP=${cfg.hostPort}",
        "D_CP=${cfg.containerPort}",
        "D_STATE=${env.HOME}/.deploy-state"
    ]) {
        int rc = sh(script: '''
            mkdir -p "$D_STATE"
            docker rm -f "$D_NAME" >/dev/null 2>&1 || true
            docker run -d --name "$D_NAME" -p "$D_HP:$D_CP" "$D_IMAGE"
        ''', returnStatus: true)

        boolean healthy = (rc == 0) && httpCheck(url, attempts)

        if (!healthy) {
            echo "Verification FAILED for ${cfg.image} - starting rollback"
            String lastGood = sh(script: 'cat "$D_STATE/$D_NAME.lastgood" 2>/dev/null || true',
                                 returnStdout: true).trim()
            sh 'docker rm -f "$D_NAME" >/dev/null 2>&1 || true'

            if (lastGood) {
                withEnv(["D_LAST=${lastGood}"]) {
                    sh 'docker run -d --name "$D_NAME" -p "$D_HP:$D_CP" "$D_LAST"'
                }
                boolean ok = httpCheck(url, attempts)
                echo(ok ? "Rolled back to ${lastGood} (verified healthy)"
                        : "CRITICAL: rollback target ${lastGood} is also unhealthy")
            } else {
                echo "No last-known-good image recorded - nothing to roll back to"
            }
            error("Deployment of ${cfg.image} failed verification")
        }

        sh 'echo "$D_IMAGE" > "$D_STATE/$D_NAME.lastgood"'
        echo "Deployed ${cfg.image}, verified, recorded as last known good"
    }
}