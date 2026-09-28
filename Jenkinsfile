@Library('shared-lib') _

pipeline {
    agent none
    parameters {
        choice(
            name: 'TARGET_ENV',
            choices: ['dev', 'qa'],
            description: 'Environment to deploy to'
        )
    }
    stages {
        stage('Greet') {
            agent any
            steps {
                sayHello('Manjot')
            }
        }
        stage('Compute Version') {
            agent any
            steps {
                script {
                    env.APP_VERSION = generateVersion(env.BUILD_NUMBER)
                    echo "App version for this build: ${env.APP_VERSION}"
                }
            }
        }
        stage('Show Deployment Info') {
            agent any
            steps {
                deploymentInfo(
                    environment: params.TARGET_ENV,
                    port: 8005,
                    version: env.APP_VERSION
                )
            }
        }
        stage('Deploy') {
            agent { label 'sast' }
            steps {
                sh 'docker rm -f blue-green-test || true'
                sh 'docker run -d --name blue-green-test -p 8005:80 nginx'
                sh 'sleep 2'
            }
        }
        stage('Validate') {
            agent { label 'sast' }
            steps {
                script {
                    def healthy = healthCheck('http://localhost:8005', 5)
                    if (!healthy) {
                        error("Validation failed for version ${env.APP_VERSION}")
                    }
                    echo "Version ${env.APP_VERSION} validated on ${params.TARGET_ENV}"
                }
            }
        }
    }
}