pipeline {
    agent { label 'devops' }

    environment {
        AWS_PROFILE = 'devops-iac'
        AWS_CREDENTIALS_ID = 'aws-credentials-id' // Replace with your Jenkins AWS credentials ID
        REPO_URL = 'https://your-repo-url.git' // Replace with your repository URL
        REPO_NAME = 'terraform_iac'
        REPO_DIR = 'devops_sandbox'
    }

    stages {
        stage('Clean Workspace') {
            steps {
                script {
                    cleanWs()
                }
            }
        }

        stage('Switch AWS Profile') {
            steps {
                script {
                    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: "${AWS_CREDENTIALS_ID}"]]) {
                        sh '''
                            aws configure set profile.${AWS_PROFILE}.aws_access_key_id $AWS_ACCESS_KEY_ID
                            aws configure set profile.${AWS_PROFILE}.aws_secret_access_key $AWS_SECRET_ACCESS_KEY
                        '''
                    }
                }
            }
        }

        stage('Clone Repository') {
            steps {
                script {
                    dir("${WORKSPACE}/${REPO_NAME}") {
                        checkout([$class: 'GitSCM', branches: [[name: '*/main']], userRemoteConfigs: [[url: "${REPO_URL}"]], extensions: [[$class: 'SparseCheckoutPaths', sparseCheckoutPaths: [[path: "${REPO_DIR}"]]]]])
                    }
                }
            }
        }

        stage('Terraform Init') {
            steps {
                script {
                    dir("${WORKSPACE}/${REPO_NAME}/${REPO_DIR}") {
                        sh 'terraform init'
                    }
                }
            }
        }

        stage('Terraform Apply') {
            steps {
                script {
                    dir("${WORKSPACE}/${REPO_NAME}/${REPO_DIR}") {
                        input message: 'Approve Terraform Apply?', ok: 'Apply'
                        sh 'terraform apply -auto-approve'
                    }
                }
            }
        }
    }
}
