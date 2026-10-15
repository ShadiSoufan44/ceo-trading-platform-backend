
pipeline {
    agent any

    tools {
        jdk 'JDK21'
    }

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
        IMAGE_REPO = 'docker.io/slimshadi4444/trading-platform-backend'
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
                echo '========== Building with Java 21 =========='
                sh '''
                    java -version
                    mvn -version
                    mvn -B clean compile
                '''
            }
        }

        stage('Test') {
            steps {
                echo '========== Running Maven tests =========='

                withCredentials([file(
                    credentialsId: 'backend-env',
                    variable: 'ENV_FILE'
                )]) {
                    sh '''
                        set +x

                        CLEAN_ENV=$(mktemp)
                        chmod 600 "$CLEAN_ENV"

                        trap 'rm -f "$CLEAN_ENV"' EXIT

                        # Convert Windows line endings to Unix
                        tr -d '\\r' < "$ENV_FILE" > "$CLEAN_ENV"

                        # Load environment variables
                        set -a
                        . "$CLEAN_ENV"
                        set +a

                        # Run Maven tests
                        mvn -B test
                    '''
                }
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
                        set +x

                        export SONAR_TOKEN="$SONAR_AUTH_TOKEN"

                        mvn -B \
                            org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar \
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

                sh '''
                    docker build \
                        -t "$IMAGE_REPO:$DOCKER_IMAGE_TAG" \
                        -t "$IMAGE_REPO:$BUILD_NUMBER" \
                        -f Dockerfile .
                '''
            }
        }

        stage('Push to Docker Registry') {
            steps {
                echo '========== Pushing Docker images =========='

                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                        set -eu
                        set +x

                        export DOCKER_CONFIG="$(mktemp -d)"
                        trap 'rm -rf "$DOCKER_CONFIG"' EXIT

                        printf '%s' "$DOCKER_PASS" | docker login \
                            --username "$DOCKER_USER" \
                            --password-stdin

                        docker push "$IMAGE_REPO:$DOCKER_IMAGE_TAG"
                        docker push "$IMAGE_REPO:$BUILD_NUMBER"
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                echo '========== Deploying application =========='

                withCredentials([file(credentialsId: 'backend-env', variable: 'ENV_FILE')]) {
                    sh '''
                        set -eu
                        set +x

                        CLEAN_ENV=$(mktemp)
                        chmod 600 "$CLEAN_ENV"
                        trap 'rm -f "$CLEAN_ENV"' EXIT

                        # Remove Windows CRLF line endings
                        tr -d '\\r' < "$ENV_FILE" > "$CLEAN_ENV"

                        docker stop trading-platform-backend || true
                        docker rm trading-platform-backend || true

                        docker run -d \
                            --name trading-platform-backend \
                            --restart unless-stopped \
                            -p 8081:8081 \
                            --env-file "$CLEAN_ENV" \
                            "$IMAGE_REPO:$BUILD_NUMBER"

                        sleep 10

                        docker ps --filter name=trading-platform-backend
                        docker logs --tail 50 trading-platform-backend
                    '''
                }
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
