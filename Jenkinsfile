
pipeline {
    agent any

    options {
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        skipDefaultCheckout(true)
    }

    parameters {
        string(
            name: 'DOCKER_REGISTRY',
            defaultValue: 'docker.io',
            description: 'Docker registry'
        )
        string(
            name: 'DOCKER_IMAGE_NAME',
            defaultValue: 'trading-platform-backend',
            description: 'Docker image repository name'
        )
        string(
            name: 'DOCKER_IMAGE_TAG',
            defaultValue: 'latest',
            description: 'Docker image tag'
        )
    }

    environment {
        IMAGE_REPO = ''
    }

    stages {

        stage('Checkout') {
            steps {
                echo '========== Checking out code =========='
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo '========== Compiling application =========='
                sh 'mvn -B clean compile'
            }
        }

        stage('Test') {
            steps {
                echo '========== Running Maven tests =========='
                sh 'mvn -B test'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo '========== Running SonarQube analysis =========='

                withSonarQubeEnv('SonarQube') {
                    sh '''
                        mvn -B sonar:sonar \
                            -Dsonar.projectKey=ceo-trading-platform-backend
                    '''
                }
            }
        }

        stage('Package') {
            steps {
                echo '========== Packaging application =========='
                sh 'mvn -B package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts(
                        artifacts: 'target/*.jar',
                        fingerprint: true
                    )
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                echo '========== Building Docker image =========='

                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    script {
                        env.IMAGE_REPO = "${params.DOCKER_REGISTRY}/${env.DOCKER_USER}/${params.DOCKER_IMAGE_NAME}"
                    }

                    sh '''
                        docker build \
                            -t "$IMAGE_REPO:$DOCKER_IMAGE_TAG" \
                            -t "$IMAGE_REPO:$BUILD_NUMBER" \
                            -f Dockerfile .
                    '''
                }
            }
        }

        stage('Push to Docker Registry') {
            steps {
                echo '========== Pushing Docker images =========='

                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    sh '''
                        set +x
                        export DOCKER_CONFIG="$(mktemp -d)"
                        trap 'docker logout "$DOCKER_REGISTRY" >/dev/null 2>&1 || true; rm -rf "$DOCKER_CONFIG"' EXIT

                        printf '%s' "$DOCKER_PASS" | docker login \
                            -u "$DOCKER_USER" \
                            --password-stdin "$DOCKER_REGISTRY"

                        docker push "$IMAGE_REPO:$DOCKER_IMAGE_TAG"
                        docker push "$IMAGE_REPO:$BUILD_NUMBER"
                    '''
                }
            }
        }

        stage('Deploy') {
            input {
                message 'Deploy to production?'
                ok 'Deploy'
            }

            steps {
                echo '========== Deploying application =========='

                sh '''
                    test -f .env || {
                        echo "ERROR: .env file is missing"
                        exit 1
                    }

                    docker stop trading-platform-backend || true
                    docker rm trading-platform-backend || true

                    docker run -d \
                        --name trading-platform-backend \
                        --restart unless-stopped \
                        -p 8081:8081 \
                        --env-file .env \
                        "$IMAGE_REPO:$BUILD_NUMBER"

                    sleep 10
                    docker ps --filter name=trading-platform-backend
                    docker logs trading-platform-backend
                '''
            }
        }
    }

    post {
        success {
            echo '========== Pipeline completed successfully =========='
        }

        failure {
            echo '========== Pipeline failed =========='
        }

        always {
            echo "Build number: ${env.BUILD_NUMBER}"
        }
    }
}
