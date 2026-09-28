@Library('shared-lib') _

pipeline {
    agent none
    parameters {
        choice(name: 'IMAGE',
               choices: ['nginx:1.27', 'nginx:1.26', 'busybox:1.36'],
               description: 'busybox = a deliberately broken release')
    }
    stages {
        stage('Banner') {
            agent any
            steps { buildBanner() }
        }
        stage('Security') {
            agent { label 'sast' }
            steps { securityScan(blockOnHighSeverity: true) }
        }
        stage('Deploy') {
            agent { label 'sast' }
            steps {
                deployWithRollback(
                    name: 'blue-green-test',
                    image: params.IMAGE,
                    hostPort: 8005,
                    containerPort: 80
                )
            }
        }
    }
}