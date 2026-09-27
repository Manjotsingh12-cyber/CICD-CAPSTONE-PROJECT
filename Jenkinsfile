pipeline {
    agent any
    stages {
        stage('Info') {
            steps {
                echo "Branch: ${env.BRANCH_NAME}"
                echo "Build: ${env.BUILD_NUMBER}"
                script {
                    if (env.CHANGE_ID) {
                        echo "Pull Request #${env.CHANGE_ID}: ${env.CHANGE_BRANCH} -> ${env.CHANGE_TARGET}"
                    } else {
                        echo "Direct branch build"
                    }
                }
            }
        }
        stage('Repo Check') {
            steps {
                sh 'python3 check.py'
            }
        }
    }
}