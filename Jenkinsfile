@Library('shared-lib') _

pipeline {
    agent any
    parameters {
        choice(
            name: 'TARGET_ENV',
            choices: ['dev', 'qa'],
            description: 'Environment to deploy to'
        )
    }
    stages {
        stage('Greet') {
            steps {
                sayHello('Manjot')
            }
        }
        stage('Compute Version') {
            steps {
                script {
                    env.APP_VERSION = generateVersion(env.BUILD_NUMBER)
                    echo "App version for this build: ${env.APP_VERSION}"
                }
            }
        }
        stage('Show Deployment Info') {
            steps {
                deploymentInfo(
                    environment: params.TARGET_ENV,
                    port: 8000,
                    version: env.APP_VERSION
                )
            }
        }
        stage('Deploy') {
            steps {
                echo "Deploying version ${env.APP_VERSION} to ${params.TARGET_ENV}..."
                sh 'docker rm -f blue-green-test || true'
                sh 'docker run -d --name blue-green-test -p 8005:80 nginx'
                sh 'sleep 2'
            }
        }
        stage('Validate') {
            steps {
                script {
                    def healthy = healthCheck('http://localhost:8005', 5)
                    if (!healthy) {
                        error("Deployment validation failed for version ${env.APP_VERSION}")
                    }
                    echo "Deployment of version ${env.APP_VERSION} to ${params.TARGET_ENV} validated successfully"
                }
            }
        }
    }
}