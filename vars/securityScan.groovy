// Runs SAST (Bandit) and secret scanning (Gitleaks) on the current workspace.
// Options: blockOnHighSeverity (default false), gitleaksConfig (path, default none)
def call(Map cfg = [:]) {
    boolean blockOnHigh = cfg.get('blockOnHighSeverity', false)
    String glConfig = cfg.get('gitleaksConfig', '')

    try {
        // banditArgs comes from a boolean, never from caller-supplied text
        String banditArgs = blockOnHigh ? '-lll' : '--exit-zero'
        sh "bandit -r . -f txt -o bandit-report.txt ${banditArgs}"

        withEnv(["GL_CFG=${glConfig}"]) {
            sh '''
                if [ -n "$GL_CFG" ]; then
                    gitleaks detect --source . --config "$GL_CFG" --report-path gitleaks-report.json
                else
                    gitleaks detect --source . --report-path gitleaks-report.json
                fi
            '''
        }
    } finally {
        // reports are archived whether the scans passed or failed
        archiveArtifacts artifacts: 'bandit-report.txt,gitleaks-report.json', allowEmptyArchive: true
    }
}