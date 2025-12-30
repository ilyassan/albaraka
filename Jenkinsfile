pipeline {
    agent any

    environment {
        // Docker image configuration
        IMAGE_NAME = "ilyassanida/albaraka-banking"
        IMAGE_TAG = "${BUILD_NUMBER}"

        // Deployment configuration
        DEPLOY_HOST = "root@165.232.122.222"
        DEPLOY_PATH = "/opt/albaraka"

        // Application configuration
        APP_PORT = "8080"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "=========================================="
                    echo "Stage: Checkout Code"
                    echo "Branch: ${env.BRANCH_NAME ?: 'main'}"
                    echo "Build Number: ${BUILD_NUMBER}"
                    echo "=========================================="
                }

                checkout scm

                script {
                    env.GIT_COMMIT_MSG = sh(
                        script: 'git log -1 --pretty=%B',
                        returnStdout: true
                    ).trim()
                    env.GIT_AUTHOR = sh(
                        script: 'git log -1 --pretty=%an',
                        returnStdout: true
                    ).trim()

                    echo "Commit: ${env.GIT_COMMIT_MSG}"
                    echo "Author: ${env.GIT_AUTHOR}"
                }
            }
        }

        stage('Environment Check') {
            steps {
                script {
                    echo "=========================================="
                    echo "Stage: Environment Check"
                    echo "=========================================="
                }

                sh '''
                    echo "Java Version:"
                    java -version

                    echo "\nMaven Version:"
                    mvn -version

                    echo "\nDocker Version:"
                    docker --version
                '''
            }
        }

        stage('Run Tests') {
            steps {
                script {
                    echo "=========================================="
                    echo "Stage: Running Unit Tests"
                    echo "=========================================="
                }

                sh 'mvn clean test -B'
            }

            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }

                failure {
                    script {
                        echo "❌ Tests failed! Aborting pipeline."
                        error("Tests failed")
                    }
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    echo "=========================================="
                    echo "Stage: Building Docker Image"
                    echo "Image: ${IMAGE_NAME}:${IMAGE_TAG}"
                    echo "=========================================="
                }

                sh """
                    docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
                    docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${IMAGE_NAME}:latest
                    docker images | grep ${IMAGE_NAME}
                """
            }
        }

        stage('Push Docker Image') {
            steps {
                script {
                    echo "=========================================="
                    echo "Stage: Pushing Docker Image to Registry"
                    echo "=========================================="
                }

                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    sh """
                        echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                        docker push ${IMAGE_NAME}:${IMAGE_TAG}
                        docker push ${IMAGE_NAME}:latest
                        docker logout
                        echo "✅ Images pushed successfully"
                    """
                }
            }

            post {
                always {
                    sh """
                        docker rmi ${IMAGE_NAME}:${IMAGE_TAG} || true
                        docker rmi ${IMAGE_NAME}:latest || true
                    """
                }
            }
        }

        stage('Backup Current Version') {
            steps {
                script {
                    echo "=========================================="
                    echo "Stage: Backing Up Current Version"
                    echo "=========================================="
                }

                sh """
                    ssh -o StrictHostKeyChecking=no ${DEPLOY_HOST} '
                        cd ${DEPLOY_PATH}
                        mkdir -p backups

                        if [ -f docker-compose.prod.yml ]; then
                            BACKUP_NAME=backup_\$(date +%Y%m%d_%H%M%S)_build_${BUILD_NUMBER}
                            mkdir -p backups/\$BACKUP_NAME

                            cp docker-compose.prod.yml backups/\$BACKUP_NAME/ 2>/dev/null || true
                            cp .env backups/\$BACKUP_NAME/ 2>/dev/null || true

                            echo "✅ Backup created: \$BACKUP_NAME"

                            # Keep only last 3 backups
                            ls -t backups | tail -n +4 | xargs -I {} rm -rf backups/{} || true
                        else
                            echo "No existing deployment to backup"
                        fi
                    '
                """
            }
        }

        stage('Deploy to Production') {
            steps {
                script {
                    echo "=========================================="
                    echo "Stage: Deploying to Production VPS"
                    echo "=========================================="
                }

                sh """
                    ssh -o StrictHostKeyChecking=no ${DEPLOY_HOST} '
                        cd ${DEPLOY_PATH}

                        # Pull the latest image
                        docker pull ${IMAGE_NAME}:${IMAGE_TAG}
                        docker pull ${IMAGE_NAME}:latest

                        # Stop and remove old containers
                        docker-compose -f docker-compose.prod.yml down || true

                        # Start new containers
                        export IMAGE_VERSION=${IMAGE_TAG}
                        docker-compose -f docker-compose.prod.yml up -d

                        # Wait for application to start
                        sleep 15

                        # Health check
                        RETRY_COUNT=0
                        MAX_RETRIES=12

                        while [ \$RETRY_COUNT -lt \$MAX_RETRIES ]; do
                            if docker-compose -f docker-compose.prod.yml ps | grep -q "Up"; then
                                echo "✅ Application is running!"
                                exit 0
                            fi

                            RETRY_COUNT=\$((RETRY_COUNT + 1))
                            echo "Health check attempt \$RETRY_COUNT/\$MAX_RETRIES..."
                            sleep 5
                        done

                        echo "❌ Application health check failed"
                        exit 1
                    '
                """
            }

            post {
                success {
                    echo "✅ Deployment successful! Version: ${IMAGE_TAG}"
                }

                failure {
                    echo "❌ Deployment failed!"
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                sh """
                    ssh -o StrictHostKeyChecking=no ${DEPLOY_HOST} '
                        cd ${DEPLOY_PATH}

                        echo "Running containers:"
                        docker-compose -f docker-compose.prod.yml ps

                        echo "\nContainer logs (last 20 lines):"
                        docker-compose -f docker-compose.prod.yml logs --tail=20 app
                    '
                """
            }
        }

        stage('Cleanup') {
            steps {
                sh """
                    ssh -o StrictHostKeyChecking=no ${DEPLOY_HOST} '
                        docker image prune -f
                        docker images ${IMAGE_NAME} --format "{{.Tag}}" | grep -E "^[0-9]+\$" | sort -rn | tail -n +4 | xargs -I {} docker rmi ${IMAGE_NAME}:{} || true
                        echo "✅ Cleanup completed"
                    '
                """
            }
        }
    }

    post {
        success {
            script {
                echo "=========================================="
                echo "✅ CI/CD PIPELINE COMPLETED SUCCESSFULLY"
                echo "=========================================="
                echo "Build: #${BUILD_NUMBER}"
                echo "Branch: ${env.BRANCH_NAME ?: 'main'}"
                echo "Commit: ${env.GIT_COMMIT_MSG}"
                echo "Author: ${env.GIT_AUTHOR}"

                if (env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'master') {
                    echo "Deployed Version: ${IMAGE_TAG}"
                }
                echo "=========================================="
            }
        }

        failure {
            script {
                echo "=========================================="
                echo "❌ CI/CD PIPELINE FAILED"
                echo "=========================================="
                echo "Build: #${BUILD_NUMBER}"
                echo "Branch: ${env.BRANCH_NAME ?: 'main'}"
                echo "Commit: ${env.GIT_COMMIT_MSG}"
                echo "=========================================="
            }
        }

        always {
            cleanWs()
        }
    }
}
