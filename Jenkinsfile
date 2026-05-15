pipeline {
    agent any

    environment {
        // Define the list of microservices
        SERVICES = "admin-server analytics-service api-gateway auth-service budget-service category-service eureka-server expense-service income-service notification-service payment-service recurring-service subscription-service"
        // The name of the SonarQube server configuration in Jenkins (Manage Jenkins -> System)
        SONAR_SERVER = "SonarQube" 
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                script {
                    def serviceList = env.SERVICES.split(' ')
                    for (service in serviceList) {
                        dir(service) {
                            echo "========================================"
                            echo " Building and Testing: ${service}"
                            echo "========================================"
                            if (isUnix()) {
                                sh 'chmod +x mvnw && ./mvnw clean test -B'
                            } else {
                                bat 'mvnw.cmd clean test -B'
                            }
                        }
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    def serviceList = env.SERVICES.split(' ')
                    // This block requires the "SonarQube Scanner for Jenkins" plugin.
                    // It automatically injects the SonarQube URL and authentication token.
                    withSonarQubeEnv(env.SONAR_SERVER) {
                        for (service in serviceList) {
                            dir(service) {
                                echo "========================================"
                                echo " SonarQube Analysis: ${service}"
                                echo "========================================"
                                if (isUnix()) {
                                    sh "./mvnw sonar:sonar -Dsonar.projectKey=${service} -Dsonar.projectName=${service} -B"
                                } else {
                                    bat "mvnw.cmd sonar:sonar -Dsonar.projectKey=${service} -Dsonar.projectName=${service} -B"
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('Docker Build') {
            steps {
                echo "========================================"
                echo " Building Docker Images"
                echo "========================================"
                if (isUnix()) {
                    sh 'docker-compose build'
                } else {
                    bat 'docker-compose build'
                }
            }
        }

        stage('Deploy') {
            steps {
                echo "========================================"
                echo " Deploying Stack via Docker Compose"
                echo "========================================"
                if (isUnix()) {
                    sh 'docker-compose down'
                    sh 'docker-compose up -d'
                } else {
                    bat 'docker-compose down'
                    bat 'docker-compose up -d'
                }
            }
        }
    }

    post {
        always {
            echo "========================================"
            echo " Pipeline Execution Completed"
            echo "========================================"
        }
        success {
            echo "Pipeline succeeded! Application is deployed."
        }
        failure {
            echo "Pipeline failed! Please check the logs."
        }
    }
}
