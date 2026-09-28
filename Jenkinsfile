@Library('shared-lib') _

pipeline {
    agent none
    parameters {
        choice(name: 'TARGET_ENV', choices: ['dev', 'qa'], description: 'Environment to deploy to')
    }
    stages {
        stage('Banner') {
            agent any
            steps {
                buildBanner()
                sayHello('Manjot')
            }
        }
        stage('Compute Version') {
            agent any
            steps {
                script {
                    env.APP_VERSION = generateVersion(env.BUILD_NUMBER)
                    echo "App version: ${env.APP_VERSION}"
                }
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
                    if (!httpCheck('http://localhost:8005', 5)) {
                        error("Validation failed for ${env.APP_VERSION}")
                    }
                    echo "Version ${env.APP_VERSION} validated on ${params.TARGET_ENV}"
                }
            }
        }
    }
}