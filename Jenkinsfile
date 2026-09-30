pipeline {
    agent none
    stages {
        stage('Test') {
            agent { label 'ci' }
            steps {
                sh 'pip3 install -r requirements.txt --break-system-packages || true'
                echo "No unit tests yet - placeholder stage"
            }
        }
        stage('SAST') {
            agent { label 'ci' }
            steps {
                sh 'bandit -r . -f txt -o bandit-report.txt --exit-zero'
                sh 'cat bandit-report.txt'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'bandit-report.txt', allowEmptyArchive: true
                }
            }
        }
        stage('Secret Scan') {
            agent { label 'ci' }
            steps {
                sh 'gitleaks detect --source . --report-path gitleaks-report.json'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'gitleaks-report.json', allowEmptyArchive: true
                }
            }
        }
        stage('SonarQube Scan') {
            agent { label 'ci' }
            steps {
                withSonarQubeEnv('sonar') {
                    sh 'sonar-scanner -Dsonar.projectKey=billing-payment -Dsonar.sources=.'
                }
            }
        }
        stage('Docker Build') {
            agent { label 'sast' }
            steps {
                script {
                    def gitSha = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.GIT_SHA = gitSha
                    env.APP_VERSION = "0.1.${env.BUILD_NUMBER}"
                }
                sh """
                    docker build \
                      --build-arg GIT_SHA=${env.GIT_SHA} \
                      --build-arg BUILD_NUMBER=${env.BUILD_NUMBER} \
                      --build-arg APP_VERSION=${env.APP_VERSION} \
                      -t billing-payment:${env.GIT_SHA} .
                """
            }
        }
        stage('Trivy Scan') {
    agent { label 'sast' }
    steps {
        sh "trivy --cache-dir /mnt/trivy image \
            --exit-code 0 \
            --severity HIGH,CRITICAL \
            billing-payment:${env.GIT_SHA}"
    }
}
        stage('SBOM') {
            agent { label 'sast' }
            steps {
                sh "syft billing-payment:${env.GIT_SHA} -o json > sbom.json"
            }
            post {
                always {
                    archiveArtifacts artifacts: 'sbom.json', allowEmptyArchive: true
                }
            }
        }
        stage('Deploy') {
            agent { label 'sast' }
            steps {
                sh 'docker rm -f billing-payment-dev || true'
                sh "docker run -d --name billing-payment-dev -p 8006:8000 billing-payment:${env.GIT_SHA}"
                sh 'sleep 3'
            }
        }
        stage('Validate') {
            agent { label 'sast' }
            steps {
                sh 'curl -sf http://localhost:8006/health'
                sh 'curl -sf http://localhost:8006/version'
                echo "Deployed and validated: ${env.APP_VERSION} (${env.GIT_SHA})"
            }
        }
    }
}