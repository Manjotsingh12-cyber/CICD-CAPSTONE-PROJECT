pipeline {

    agent none

    environment {
        APP_NAME       = 'billing-payment'
        AWS_REGION     = 'ap-south-1'
        ECR_REPOSITORY = 'billing-payment'
        TERRAFORM_DIR  = '/home/mbrar/terraform'
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
                    echo "Image Tag: ${env.IMAGE_TAG}"
                    echo "App Version: ${env.APP_VERSION}"
                }
            }
        }

        stage('Test') {
            agent { label 'ci' }

            steps {
                sh '''
                    set -e

                    echo "Running tests..."

                    python3 -m pytest -q
                '''
            }
        }

        stage('Security Scans') {
            parallel {

                stage('Bandit') {
                    agent { label 'sast' }

                    steps {
                        sh '''
                            bandit -r . -f json -o bandit-report.json || true
                        '''
                    }
                }

                stage('Gitleaks') {
                    agent { label 'sast' }

                    steps {
                        sh '''
                            gitleaks detect \
                              --source . \
                              --no-banner \
                              --report-format json \
                              --report-path gitleaks-report.json \
                              || true
                        '''
                    }
                }

                stage('SonarQube') {
                    agent { label 'sast' }

                    steps {
                        withSonarQubeEnv('sonarqube') {
                            sh '''
                                sonar-scanner \
                                  -Dsonar.projectKey=billing-payment \
                                  -Dsonar.sources=.
                            '''
                        }
                    }
                }

                stage('OWASP ZAP') {
                    agent { label 'sast' }

                    steps {
                        sh '''
                            echo "ZAP scan will be configured after basic pipeline works"
                        '''
                    }
                }
            }
        }

        stage('Docker Build') {
            agent { label 'ci' }

            steps {
                sh '''
                    set -e

                    docker build \
                      --build-arg GIT_SHA="${GIT_SHA}" \
                      --build-arg BUILD_NUMBER="${BUILD_NUMBER}" \
                      --build-arg APP_VERSION="${APP_VERSION}" \
                      -t "${APP_NAME}:${IMAGE_TAG}" .
                '''
            }
        }

        stage('Container Security') {
            parallel {

                stage('Trivy') {
                    agent { label 'ci' }

                    steps {
                        sh '''
                            trivy image \
                              --format json \
                              --output trivy-report.json \
                              "${APP_NAME}:${IMAGE_TAG}" \
                              || true
                        '''
                    }
                }

                stage('SBOM') {
                    agent { label 'ci' }

                    steps {
                        sh '''
                            syft "${APP_NAME}:${IMAGE_TAG}" \
                              -o json > sbom-report.json
                        '''
                    }
                }
            }
        }

        stage('Push Image to ECR') {
            agent { label 'ci' }

            steps {
                sh '''
                    set -e

                    AWS_ACCOUNT_ID=$(aws sts get-caller-identity \
                      --query Account \
                      --output text)

                    ECR_URL="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPOSITORY}"

                    aws ecr get-login-password \
                      --region "${AWS_REGION}" | \
                    docker login \
                      --username AWS \
                      --password-stdin "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

                    docker tag \
                      "${APP_NAME}:${IMAGE_TAG}" \
                      "${ECR_URL}:${IMAGE_TAG}"

                    docker push \
                      "${ECR_URL}:${IMAGE_TAG}"
                '''
            }
        }

        stage('Terraform Plan') {
            agent { label 'node-2' }

            steps {
                sh '''
                    set -e

                    cd "${TERRAFORM_DIR}"

                    terraform init

                    terraform plan \
                      -var="image_tag=${IMAGE_TAG}"
                '''
            }
        }

        stage('Terraform Apply / Deploy ECS') {
            agent { label 'node-2' }

            steps {
                sh '''
                    set -e

                    cd "${TERRAFORM_DIR}"

                    terraform apply \
                      -auto-approve \
                      -var="image_tag=${IMAGE_TAG}"
                '''
            }
        }

        stage('Validate ECS Deployment') {
            agent { label 'node-2' }

            steps {
                sh '''
                    set -e

                    echo "Getting ALB DNS..."

                    ALB_DNS=$(terraform -chdir="${TERRAFORM_DIR}" output -raw alb_dns_name)

                    echo "ALB: ${ALB_DNS}"

                    echo "Checking application..."

                    curl -f "http://${ALB_DNS}/health"

                    echo ""
                    echo "Checking version..."

                    curl -f "http://${ALB_DNS}/version"

                    echo ""
                    echo "Expected Git SHA: ${GIT_SHA}"
                '''
            }
        }
    }

    post {

        success {
            echo """
            ========================================
                    PIPELINE SUCCESSFUL
            ========================================
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
                    PIPELINE FAILED
            ========================================
            Application : ${APP_NAME}
            Git SHA     : ${GIT_SHA}
            Image Tag   : ${IMAGE_TAG}
            ========================================
            """
        }
    }
}
