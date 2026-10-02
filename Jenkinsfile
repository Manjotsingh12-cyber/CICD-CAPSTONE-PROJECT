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
                    archiveArtifacts artifacts: 'bandit-report.txt',
                                     allowEmptyArchive: true
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
                    archiveArtifacts artifacts: 'gitleaks-report.json',
                                     allowEmptyArchive: true
                }
            }
        }

        stage('SonarQube Scan') {
            agent { label 'ci' }
            steps {
                withSonarQubeEnv('sonar') {
                    sh '''
                        sonar-scanner \
                          -Dsonar.projectKey=billing-payment \
                          -Dsonar.sources=.
                    '''
                }
            }
        }

        stage('Docker Build') {
            agent { label 'sast' }

            steps {
                script {
                    def gitSha = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()

                    env.GIT_SHA = gitSha
                    env.IMAGE_TAG = gitSha
                    env.APP_VERSION = "0.1.${env.BUILD_NUMBER}"
                }

                sh """
                    docker build \
                      --build-arg GIT_SHA=${env.GIT_SHA} \
                      --build-arg BUILD_NUMBER=${env.BUILD_NUMBER} \
                      --build-arg APP_VERSION=${env.APP_VERSION} \
                      -t billing-payment:${env.IMAGE_TAG} .
                """
            }
        }

        stage('Trivy Scan') {
            agent { label 'sast' }

            steps {
                sh """
                    trivy image \
                      --exit-code 0 \
                      --severity HIGH,CRITICAL \
                      billing-payment:${env.IMAGE_TAG} || true
                """
            }
        }

        stage('SBOM') {
            agent { label 'sast' }

            steps {
                sh """
                    syft billing-payment:${env.IMAGE_TAG} \
                      -o json > sbom.json
                """
            }

            post {
                always {
                    archiveArtifacts artifacts: 'sbom.json',
                                     allowEmptyArchive: true
                }
            }
        }

        stage('Terraform Infrastructure') {
            agent { label 'node-2' }

            steps {
                dir('/home/mbrar/terraform') {
                    sh '''
                        terraform init
                        terraform apply -auto-approve
                    '''
                }
            }
        }

        stage('Push Image to ECR') {
            agent { label 'node-2' }

            steps {
                dir('/home/mbrar/terraform') {
                    sh '''
                        ECR_REPO=$(terraform output -raw ecr_repository_url)

                        echo "ECR Repository: $ECR_REPO"
                        echo "Image Tag: $IMAGE_TAG"

                        aws ecr get-login-password \
                          --region ap-south-1 | \
                          docker login \
                          --username AWS \
                          --password-stdin "$ECR_REPO"

                        docker tag \
                          "billing-payment:${IMAGE_TAG}" \
                          "$ECR_REPO:${IMAGE_TAG}"

                        docker push \
                          "$ECR_REPO:${IMAGE_TAG}"
                    '''
                }
            }
        }

        stage('Deploy to ECS') {
            agent { label 'node-2' }

            steps {
                dir('/home/mbrar/terraform') {
                    sh '''
                        terraform apply \
                          -auto-approve \
                          -var="image_tag=${IMAGE_TAG}"
                    '''
                }
            }
        }

        stage('Wait for ECS') {
            agent { label 'node-2' }

            steps {
                dir('/home/mbrar/terraform') {
                    sh '''
                        CLUSTER=$(terraform output -raw ecs_cluster_name)
                        SERVICE=$(terraform output -raw ecs_service_name)

                        echo "ECS Cluster: $CLUSTER"
                        echo "ECS Service: $SERVICE"
                        echo "Waiting for ECS service to stabilize..."

                        aws ecs wait services-stable \
                          --cluster "$CLUSTER" \
                          --services "$SERVICE" \
                          --region ap-south-1

                        echo "ECS service is stable."
                    '''
                }
            }
        }

        stage('Validate') {
            agent { label 'node-2' }

            steps {
                dir('/home/mbrar/terraform') {
                    sh '''
                        ALB_DNS=$(terraform output -raw alb_dns_name)

                        echo "ALB: http://$ALB_DNS"

                        echo "Checking /health..."
                        curl -sf "http://$ALB_DNS/health"

                        echo ""
                        echo "Checking /version..."
                        curl -sf "http://$ALB_DNS/version"

                        echo ""
                        echo "ECS deployment validated successfully."
                    '''
                }

                echo "Deployed and validated: ${env.APP_VERSION} (${env.GIT_SHA})"
            }
        }

        stage('OWASP ZAP Scan') {
            agent { label 'node-2' }

            steps {
                sh '''
                    sudo rm -rf /mnt/trivy/zap-work
                    sudo mkdir -p /mnt/trivy/zap-work
                    sudo chmod 0777 /mnt/trivy/zap-work

                    ALB_DNS=$(cd /home/mbrar/terraform && \
                              terraform output -raw alb_dns_name)

                    echo "Running OWASP ZAP against:"
                    echo "http://$ALB_DNS"

                    docker run --rm \
                      --network host \
                      -v /mnt/trivy/zap-work:/zap/wrk/:rw \
                      ghcr.io/zaproxy/zaproxy:stable \
                      zap-baseline.py \
                      -t "http://$ALB_DNS" \
                      -r zap-report.html \
                      -J zap-report.json \
                      || true

                    echo "Copying ZAP reports..."

                    cp /mnt/trivy/zap-work/zap-report.html \
                       "$WORKSPACE/"

                    cp /mnt/trivy/zap-work/zap-report.json \
                       "$WORKSPACE/"
                '''

                archiveArtifacts \
                    artifacts: 'zap-report.html,zap-report.json', \
                    allowEmptyArchive: false
            }
        }
    }
}