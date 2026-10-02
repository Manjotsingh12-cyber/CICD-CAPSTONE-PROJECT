@Library('shared-lib') _

pipeline {

    agent none

    environment {
        APP_NAME = 'billing-payment'
        AWS_REGION = 'ap-south-1'

        // Existing ECR repository created by Terraform
        ECR_REPOSITORY = 'billing-payment'

        // Existing Terraform directory on node-2
        TERRAFORM_DIR = '/home/mbrar/terraform'
    }

    stages {

        stage('Checkout') {
            agent { label 'ci' }

            steps {
                checkout scm

                script {
                    env.GIT_SHA = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()

                    env.IMAGE_TAG = env.GIT_SHA
                    env.APP_VERSION = "0.1.${env.BUILD_NUMBER}"

                    echo "Git SHA: ${env.GIT_SHA}"
                    echo "Image tag: ${env.IMAGE_TAG}"
                    echo "App version: ${env.APP_VERSION}"
                }
            }
        }

        stage('Test') {
            agent { label 'ci' }

            steps {
                sh '''
                    python3 -m pytest -q
                '''
            }
        }

        stage('Security Scans') {
            parallel {

                stage('Bandit') {
                    agent { label 'sast' }

                    steps {
                        banditScan()
                    }
                }

                stage('Gitleaks') {
                    agent { label 'sast' }

                    steps {
                        gitleaksScan()
                    }
                }

                stage('SonarQube') {
                    agent { label 'sast' }

                    steps {
                        sonarScan()
                    }
                }

                stage('OWASP ZAP') {
                    agent { label 'sast' }

                    steps {
                        owaspScan()
                    }
                }
            }
        }

        stage('Docker Build') {
            agent { label 'ci' }

            steps {
                dockerBuild(
                    image: "${APP_NAME}:${IMAGE_TAG}",
                    gitSha: "${GIT_SHA}",
                    buildNumber: "${BUILD_NUMBER}",
                    appVersion: "${APP_VERSION}"
                )
            }
        }

        stage('Container Security') {
            agent { label 'ci' }

            parallel {

                stage('Trivy') {
                    steps {
                        trivyScan(
                            image: "${APP_NAME}:${IMAGE_TAG}"
                        )
                    }
                }

                stage('SBOM') {
                    steps {
                        sbomGenerate(
                            image: "${APP_NAME}:${IMAGE_TAG}"
                        )
                    }
                }
            }
        }

        stage('Push Image to ECR') {
            agent { label 'ci' }

            steps {
                dockerPush(
                    image: "${APP_NAME}:${IMAGE_TAG}",
                    repository: "${ECR_REPOSITORY}",
                    region: "${AWS_REGION}"
                )
            }
        }

        stage('Terraform Plan') {
            agent { label 'node-2' }

            steps {
                vaultAwsCreds {
                    terraformPlan(
                        terraformDir: "${TERRAFORM_DIR}",
                        imageTag: "${IMAGE_TAG}"
                    )
                }
            }
        }

        stage('Terraform Apply / Deploy ECS') {
            agent { label 'node-2' }

            steps {
                vaultAwsCreds {
                    terraformApply(
                        terraformDir: "${TERRAFORM_DIR}",
                        imageTag: "${IMAGE_TAG}"
                    )
                }
            }
        }

        stage('Validate ECS Deployment') {
            agent { label 'node-2' }

            steps {
                ecsHealthCheck(
                    terraformDir: "${TERRAFORM_DIR}",
                    expectedVersion: "${GIT_SHA}",
                    region: "${AWS_REGION}"
                )
            }
        }
    }

    post {

        success {
            echo """
            ========================================
            BUILD SUCCESSFUL
            Application : ${APP_NAME}
            Git SHA     : ${GIT_SHA}
            Image Tag   : ${IMAGE_TAG}
            Version     : ${APP_VERSION}
            ========================================
            """
        }

        failure {
            echo """
            ========================================
            BUILD FAILED
            Application : ${APP_NAME}
            Git SHA     : ${GIT_SHA}
            ========================================
            """
        }

        always {
            archiveArtifacts(
                artifacts: '**/sbom*.json, **/trivy*.json',
                allowEmptyArchive: true
            )
        }
    }
}
