@Library('shared-lib') _

pipeline {
    agent any
    parameters {
        choice(
            name: 'BUILD_MODE',
            choices: ['With Docker', 'Without Docker'],
            description: 'Choose whether to build a Docker image'
        )
        choice(
            name: 'TARGET_ENV',
            choices: ['node-2-dev', 'node-2-qa'],
            description: 'Choose which environment to deploy to'
        )
        booleanParam(
            name: 'RUN_TESTS',
            defaultValue: true,
            description: 'Run test suite before deploying?'
        )
        string(
            name: 'RELEASE_NOTES',
            defaultValue: '',
            description: 'Optional release notes for this build'
        )
    }
    stages {
        stage('Greet') {
            steps {
                sayHello('Manjot')
            }
        }
        stage('Info') {
            steps {
                echo "Branch: ${env.BRANCH_NAME}"
                echo "Build mode: ${params.BUILD_MODE}"
                echo "Target environment: ${params.TARGET_ENV}"
            }
        }
        stage('Test') {
            when {
                expression { return params.RUN_TESTS == true }
            }
            steps {
                sh 'python3 check.py'
            }
        }
    }
}