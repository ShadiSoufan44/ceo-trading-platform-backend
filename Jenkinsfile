pipeline {
    agent any

    options {
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    parameters {
        string(name: 'DOCKER_REGISTRY', defaultValue: 'docker.io', description: 'Docker registry URL')
        string(name: 'DOCKER_IMAGE_NAME', defaultValue: 'trading-platform-backend', description: 'Docker image name')
        string(name: 'DOCKER_IMAGE_TAG', defaultValue: 'latest', description: 'Docker image tag')
    }

    environment {
        JAVA_HOME = tool 'JDK17'
        MAVEN_HOME = tool 'Maven-3.8.1'
        PATH = "${MAVEN_HOME}/bin:${JAVA_HOME}/bin:${PATH}"
        SONAR_HOST_URL = credentials('sonar-host-url')
        SONAR_LOGIN = credentials('sonar-login-token')
        DOCKER_CREDENTIALS = credentials('docker-credentials')
    }

    stages {
        stage('Checkout') {
            steps {
                echo '========== Checking out code from main branch =========='
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    userRemoteConfigs: [[url: 'https://github.com/your-org/ceo-trading-platform-backend.git']]
                ])
            }
        }

        stage('Build') {
            steps {
                echo '========== Running Maven clean compile =========='
                sh 'mvn clean compile'
            }
        }

        stage('Test') {
            steps {
                echo '========== Running Maven tests =========='
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    publishHTML([
                        reportDir: 'target/surefire-reports',
                        reportFiles: 'index.html',
                        reportName: 'Test Report'
                    ])
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo '========== Running SonarQube code quality analysis =========='
                sh '''
                    mvn sonar:sonar \
                        -Dsonar.projectKey=ceo-trading-platform-backend \
                        -Dsonar.sources=src/main/java \
                        -Dsonar.tests=src/test/java \
                        -Dsonar.host.url=${SONAR_HOST_URL} \
                        -Dsonar.login=${SONAR_LOGIN}
                '''
            }
        }

        stage('Package') {
            steps {
                echo '========== Packaging application =========='
                sh 'mvn package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Build Docker Image') {
            when {
                branch 'main'
            }
            steps {
                echo '========== Building Docker image =========='
                script {
                    sh '''
                        docker build \
                            -t ${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG} \
                            -t ${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:${BUILD_NUMBER} \
                            -f Dockerfile .
                    '''
                }
            }
        }

        stage('Push to Docker Registry') {
            when {
                branch 'main'
            }
            steps {
                echo '========== Pushing Docker image to registry =========='
                script {
                    sh '''
                        echo $DOCKER_CREDENTIALS_PSW | docker login -u $DOCKER_CREDENTIALS_USR --password-stdin ${DOCKER_REGISTRY}
                        docker push ${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}
                        docker push ${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:${BUILD_NUMBER}
                        docker logout ${DOCKER_REGISTRY}
                    '''
                }
            }
        }

        stage('Deploy') {
            when {
                branch 'main'
            }
            input {
                message "Deploy to production?"
                ok "Deploy"
            }
            steps {
                echo '========== Deploying Docker container =========='
                script {
                    sh '''
                        # Stop and remove existing container
                        docker stop trading-platform-backend || true
                        docker rm trading-platform-backend || true

                        # Run new container
                        docker run -d \
                            --name trading-platform-backend \
                            -p 8081:8081 \
                            --env-file .env \
                            ${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}

                        # Wait for container to start
                        sleep 10
                        docker logs trading-platform-backend
                    '''
                }
            }
        }
    }

    post {
        always {
            echo '========== Pipeline finished =========='
            cleanWs()
        }
        success {
            emailext(
                subject: "Build SUCCESS: ${env.JOB_NAME} - #${env.BUILD_NUMBER}",
                body: """
                    Build successful!

                    Job: ${env.JOB_NAME}
                    Build Number: ${env.BUILD_NUMBER}
                    Build URL: ${env.BUILD_URL}
                    
                    Docker Image: ${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}
                """,
                to: '${DEFAULT_RECIPIENTS}',
                mimeType: 'text/html'
            )
        }
        failure {
            emailext(
                subject: "Build FAILED: ${env.JOB_NAME} - #${env.BUILD_NUMBER}",
                body: """
                    Build failed!

                    Job: ${env.JOB_NAME}
                    Build Number: ${env.BUILD_NUMBER}
                    Build URL: ${env.BUILD_URL}
                    
                    Check the logs for details.
                """,
                to: '${DEFAULT_RECIPIENTS}',
                mimeType: 'text/html',
                attachLog: true
            )
        }
    }
}
