pipeline {
    agent any

    environment {
        APP_NAME = 'jenkins-test'
        APP_VERSION = "${env.BUILD_NUMBER}-${env.GIT_COMMIT?.take(7) ?: 'local'}"
        GITHUB_USER = 'vanhiep99w'
        DOCKER_REGISTRY = 'ghcr.io'
        DOCKER_IMAGE = "${DOCKER_REGISTRY}/${GITHUB_USER}/${APP_NAME}"
        DEPLOY_ENV = ''
        IS_PR = "${env.CHANGE_ID ? 'true' : 'false'}"  // true if Pull Request
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5'))
        timeout(time: 60, unit: 'MINUTES')
        timestamps()
        disableConcurrentBuilds(abortPrevious: true)
        skipStagesAfterUnstable()
    }

    triggers {
        githubPush()                 // Trigger on push & PR
    }

    parameters {
        booleanParam(
            name: 'SKIP_TESTS',
            defaultValue: false,
            description: 'Skip test execution (not recommended)'
        )
    }

    stages {
        stage('Initialization') {
            steps {
                script {
                    currentBuild.displayName = "#${BUILD_NUMBER} - ${env.GIT_BRANCH ?: env.BRANCH_NAME ?: 'local'}"
                    currentBuild.description = "Commit: ${env.GIT_COMMIT?.take(7) ?: 'N/A'}"
                    
                    // Auto-determine deploy target based on branch
                    def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: ''
                    branch = branch.replaceAll('origin/', '')
                    
                    if (env.CHANGE_ID) {
                        // Pull Request - test only, no deploy
                        env.AUTO_DEPLOY_TARGET = 'none'
                        echo "Pull Request #${env.CHANGE_ID} - Build & Test only"
                    } else if (branch == 'dev') {
                        env.AUTO_DEPLOY_TARGET = 'dev'
                        echo "Dev branch - Auto deploy to DEV"
                    } else if (branch == 'uat') {
                        env.AUTO_DEPLOY_TARGET = 'uat'
                        echo "UAT branch - Auto deploy to UAT"
                    } else if (branch == 'main' || branch == 'master' || branch == 'production') {
                        env.AUTO_DEPLOY_TARGET = 'production'
                        echo "Production branch - Requires manual approval"
                    } else {
                        env.AUTO_DEPLOY_TARGET = 'none'
                        echo "Feature branch - Build & Test only"
                    }
                }
                sh '''
                    echo "=== Build Environment ==="
                    echo "Java Version: $(java -version 2>&1 | head -1)"
                    echo "Maven Wrapper: ./mvnw"
                    echo "Build Number: ${BUILD_NUMBER}"
                    echo "Branch: ${GIT_BRANCH:-local}"
                    echo "========================="
                '''
            }
        }

        stage('Checkout') {
            steps {
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
                }
            }
        }

        stage('Commitlint') {
            steps {
                sh '''
                    echo "Validating commit message format..."
                    bun install --frozen-lockfile
                    git log -1 --pretty=%B > .commit-msg-temp
                    bun x commitlint --edit .commit-msg-temp
                    rm -f .commit-msg-temp
                '''
            }
        }

        stage('Build') {
            steps {
                sh './mvnw clean compile -DskipTests -B -V'
            }
            post {
                failure {
                    script {
                        currentBuild.result = 'FAILURE'
                    }
                }
            }
        }

//         stage('Unit Tests') {
//             when {
//                 expression { return !params.SKIP_TESTS }
//             }
//             steps {
//                 sh './mvnw test -B'
//             }
//             post {
//                 always {
//                     junit(
//                         testResults: '**/target/surefire-reports/*.xml',
//                         allowEmptyResults: true
//                     )
//                     jacoco(
//                         execPattern: '**/target/jacoco.exec',
//                         classPattern: '**/target/classes',
//                         sourcePattern: '**/src/main/java',
//                         exclusionPattern: '**/test/**'
//                     )
//                 }
//                 failure {
//                     script {
//                         if (!params.FORCE_DEPLOY) {
//                             currentBuild.result = 'FAILURE'
//                         }
//                     }
//                 }
//             }
//         }
//
//         stage('Integration Tests') {
//             when {
//                 expression { return !params.SKIP_TESTS }
//             }
//             steps {
//                 sh './mvnw verify -DskipUnitTests -B'
//             }
//             post {
//                 always {
//                     junit(
//                         testResults: '**/target/failsafe-reports/*.xml',
//                         allowEmptyResults: true
//                     )
//                 }
//             }
//         }

//         stage('Code Quality') {
//             parallel {
//                 stage('SonarQube Analysis') {
//                     when {
//                         expression { return !params.SKIP_SONAR }
//                     }
//                     steps {
//                         withSonarQubeEnv('SonarQube') {
//                             sh '''
//                                 ./mvnw sonar:sonar \
//                                     -Dsonar.projectKey=${APP_NAME} \
//                                     -Dsonar.projectName=${APP_NAME} \
//                                     -Dsonar.projectVersion=${APP_VERSION} \
//                                     -Dsonar.java.coveragePlugin=jacoco \
//                                     -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
//                                     -B
//                             '''
//                         }
//                     }
//                 }
//
//                 stage('Dependency Check') {
//                     steps {
//                         sh './mvnw dependency-check:check -B || true'
//                     }
//                     post {
//                         always {
//                             dependencyCheckPublisher(
//                                 pattern: '**/dependency-check-report.xml',
//                                 failedTotalCritical: 1,
//                                 failedTotalHigh: 5
//                             )
//                         }
//                     }
//                 }
//
//                 stage('License Check') {
//                     steps {
//                         sh './mvnw license:check -B || true'
//                     }
//                 }
//             }
//         }
//
//         stage('Quality Gate') {
//             when {
//                 expression { return !params.SKIP_SONAR }
//             }
//             steps {
//                 timeout(time: 10, unit: 'MINUTES') {
//                     waitForQualityGate abortPipeline: true
//                 }
//             }
//         }
//
//         stage('Package') {
//             steps {
//                 sh './mvnw package -DskipTests -B'
//                 archiveArtifacts(
//                     artifacts: 'target/*.jar',
//                     fingerprint: true,
//                     onlyIfSuccessful: true
//                 )
//             }
//         }
//
//         stage('Security Scan') {
//             parallel {
//                 stage('SAST - Static Analysis') {
//                     steps {
//                         sh '''
//                             ./mvnw spotbugs:check -B || true
//                         '''
//                     }
//                     post {
//                         always {
//                             recordIssues(
//                                 tools: [spotBugs(pattern: '**/spotbugsXml.xml')],
//                                 qualityGates: [[threshold: 10, type: 'TOTAL', unstable: true]]
//                             )
//                         }
//                     }
//                 }
//
//                 stage('Secret Detection') {
//                     steps {
//                         sh '''
//                             echo "Scanning for hardcoded secrets..."
//                             if grep -rn "password\\s*=\\s*['\"][^'\"]*['\"]" src/ --include="*.java" --include="*.properties" --include="*.yml" 2>/dev/null | grep -v "test"; then
//                                 echo "WARNING: Potential hardcoded secrets found!"
//                             fi
//                             echo "Secret scan completed"
//                         '''
//                     }
//                 }
//             }
//         }

        stage('Package') {
            when {
                expression { return env.AUTO_DEPLOY_TARGET != 'none' }
            }
            steps {
                sh './mvnw package -DskipTests -B'
            }
        }

        stage('Build Docker Image') {
            when {
                expression { return env.AUTO_DEPLOY_TARGET != 'none' }
            }
            steps {
                sh """
                    docker build --build-arg JAR_FILE=target/*.jar -t ${DOCKER_IMAGE}:${APP_VERSION} .
                    docker tag ${DOCKER_IMAGE}:${APP_VERSION} ${DOCKER_IMAGE}:latest
                """
            }
        }

//         stage('Container Security Scan') {
//             when {
//                 expression { return env.AUTO_DEPLOY_TARGET != 'none' }
//             }
//             steps {
//                 sh '''
//                     echo "Running Trivy container scan..."
//                     trivy image --severity HIGH,CRITICAL --exit-code 0 ${DOCKER_IMAGE}:${APP_VERSION} || true
//                 '''
//             }
//         }

        stage('Push Docker Image') {
            when {
                expression { return env.AUTO_DEPLOY_TARGET != 'none' }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'github-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh """
                        echo \$DOCKER_PASS | docker login ${DOCKER_REGISTRY} -u \$DOCKER_USER --password-stdin
                        docker push ${DOCKER_IMAGE}:${APP_VERSION}
                        docker push ${DOCKER_IMAGE}:latest
                        docker logout ${DOCKER_REGISTRY}
                    """
                }
            }
            post {
                always {
                    sh """
                        echo "Cleaning up local Docker images..."
                        docker rmi ${DOCKER_IMAGE}:${APP_VERSION} || true
                        docker rmi ${DOCKER_IMAGE}:latest || true
                        
                        echo "Cleaning up build cache (keeping last 500MB)..."
                        docker builder prune --keep-storage=500MB -f || true
                    """
                }
            }
        }

        stage('Release') {
            when {
                anyOf {
                    branch 'main'
                    branch 'master'
                    branch 'dev'
                    branch 'uat'
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'github-credentials', usernameVariable: 'GH_USER', passwordVariable: 'GITHUB_TOKEN')]) {
                    sh '''
                        echo "Running semantic-release..."
                        bun install --frozen-lockfile
                        HUSKY=0 bun run release
                    '''
                }
            }
        }

//         stage('Deploy to Development') {
//             when {
//                 expression { return env.AUTO_DEPLOY_TARGET in ['dev', 'uat', 'production'] }
//             }
//             steps {
//                 script {
//                     env.DEPLOY_ENV = 'dev'
//                     deployToEnvironment('dev')
//                 }
//             }
//             post {
//                 success {
//                     runSmokeTests('dev')
//                 }
//             }
//         }

        stage('Deploy to UAT') {
            when {
                expression { return env.AUTO_DEPLOY_TARGET in ['uat', 'production'] }
            }
            steps {
                script {
                    env.DEPLOY_ENV = 'uat'
                    deployToEnvironment('uat')
                }
            }
            post {
                success {
                    runSmokeTests('uat')
                    runPerformanceTests('uat')
                }
            }
        }

        stage('Production Approval') {
            when {
                expression { return env.AUTO_DEPLOY_TARGET == 'production' }
            }
            steps {
                script {
                    def approvers = 'devops-team,tech-leads'
                    emailext(
                        subject: "Production Deployment Approval Required - ${APP_NAME}",
                        body: """
                            Production deployment approval required for ${APP_NAME}.
                            
                            Build: #${BUILD_NUMBER}
                            Version: ${APP_VERSION}
                            Branch: ${env.GIT_BRANCH}
                            Commit: ${env.GIT_COMMIT}
                            Author: ${env.GIT_AUTHOR}
                            Message: ${env.GIT_COMMIT_MSG}
                            
                            Please review and approve at: ${BUILD_URL}input
                        """,
                        to: approvers,
                        recipientProviders: [requestor()]
                    )
                    
                    timeout(time: 24, unit: 'HOURS') {
                        input(
                            message: 'Deploy to Production?',
                            ok: 'Deploy',
                            submitter: approvers,
                            parameters: [
                                booleanParam(
                                    name: 'CONFIRM_DEPLOY',
                                    defaultValue: false,
                                    description: 'I confirm this deployment has been reviewed'
                                )
                            ]
                        )
                    }
                }
            }
        }

        stage('Deploy to Production') {
            when {
                expression { return env.AUTO_DEPLOY_TARGET == 'production' }
            }
            steps {
                script {
                    env.DEPLOY_ENV = 'production'
                    
                    // Blue-Green Deployment
                    def currentSlot = getCurrentProductionSlot()
                    def targetSlot = currentSlot == 'blue' ? 'green' : 'blue'
                    
                    echo "Deploying to ${targetSlot} slot (current: ${currentSlot})"
                    deployToEnvironment('production', targetSlot)
                    
                    // Health check on new deployment
                    def healthCheck = runHealthCheck('production', targetSlot)
                    if (healthCheck) {
                        switchProductionTraffic(targetSlot)
                        echo "Traffic switched to ${targetSlot} slot"
                    } else {
                        error "Health check failed on ${targetSlot} slot"
                    }
                }
            }
            post {
                success {
                    runSmokeTests('production')
                }
                failure {
                    script {
                        echo "Production deployment failed, initiating rollback..."
                        rollbackProduction()
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs(
                cleanWhenAborted: true,
                cleanWhenFailure: true,
                cleanWhenNotBuilt: true,
                cleanWhenSuccess: true,
                cleanWhenUnstable: true,
                deleteDirs: true
            )
            
            script {
                def buildStatus = currentBuild.currentResult
                def color = buildStatus == 'SUCCESS' ? 'good' : 'danger'
                
//                 // Slack notification
//                 slackSend(
//                     channel: '#ci-cd-notifications',
//                     color: color,
//                     message: """
//                         *${APP_NAME}* - Build #${BUILD_NUMBER}
//                         *Status:* ${buildStatus}
//                         *Branch:* ${env.GIT_BRANCH ?: 'N/A'}
//                         *Commit:* ${env.GIT_COMMIT?.take(7) ?: 'N/A'}
//                         *Author:* ${env.GIT_AUTHOR ?: 'N/A'}
//                         *Duration:* ${currentBuild.durationString}
//                         *Deploy Target:* ${params.DEPLOY_TARGET}
//                         <${BUILD_URL}|View Build>
//                     """
//                 )
            }
        }
        
        success {
            echo "Pipeline completed successfully!"
            script {
                if (env.DEPLOY_ENV == 'production') {
                    createGitTag("v${APP_VERSION}")
                }
            }
        }
        
        failure {
            echo "Pipeline failed!"
//             emailext(
//                 subject: "FAILED: ${APP_NAME} Build #${BUILD_NUMBER}",
//                 body: """
//                     Build failed for ${APP_NAME}.
//
//                     Build: #${BUILD_NUMBER}
//                     Branch: ${env.GIT_BRANCH}
//                     Commit: ${env.GIT_COMMIT}
//
//                     Check console output at: ${BUILD_URL}console
//                 """,
//                 to: '${DEFAULT_RECIPIENTS}',
//                 recipientProviders: [culprits(), requestor()]
//             )
        }
        
        unstable {
            echo "Pipeline is unstable!"
        }
    }
}

// Helper Functions
def deployToEnvironment(String environment, String slot = null) {
    def namespace = "app-${environment}"
    def helmRelease = slot ? "${APP_NAME}-${slot}" : APP_NAME
    
    withCredentials([file(credentialsId: "kubeconfig-${environment}", variable: 'KUBECONFIG')]) {
        sh """
            helm upgrade --install ${helmRelease} ./helm/${APP_NAME} \
                --namespace ${namespace} \
                --set image.repository=${DOCKER_IMAGE} \
                --set image.tag=${APP_VERSION} \
                --set environment=${environment} \
                --set replicaCount=${getReplicaCount(environment)} \
                --set resources.requests.memory=${getMemoryRequest(environment)} \
                --set resources.limits.memory=${getMemoryLimit(environment)} \
                --wait \
                --timeout 10m
        """
    }
}

def runSmokeTests(String environment) {
    def baseUrl = getEnvironmentUrl(environment)
    sh """
        echo "Running smoke tests against ${baseUrl}"
        curl -f ${baseUrl}/api/v1/health || exit 1
        curl -f ${baseUrl}/api/v1/info || exit 1
        echo "Smoke tests passed!"
    """
}

def runPerformanceTests(String environment) {
    def baseUrl = getEnvironmentUrl(environment)
    sh """
        echo "Running performance tests against ${baseUrl}"
        # Add k6, JMeter, or other performance testing tool commands here
        echo "Performance tests completed"
    """
}

def runHealthCheck(String environment, String slot) {
    def baseUrl = getEnvironmentUrl(environment, slot)
    def maxRetries = 10
    def retryInterval = 15
    
    for (int i = 0; i < maxRetries; i++) {
        def result = sh(
            script: "curl -sf ${baseUrl}/api/v1/health",
            returnStatus: true
        )
        if (result == 0) {
            return true
        }
        sleep(retryInterval)
    }
    return false
}

def getCurrentProductionSlot() {
    // Query current active slot from load balancer or service mesh
    return sh(
        script: "kubectl get service ${APP_NAME}-prod -o jsonpath='{.spec.selector.slot}' 2>/dev/null || echo 'blue'",
        returnStdout: true
    ).trim()
}

def switchProductionTraffic(String targetSlot) {
    sh """
        kubectl patch service ${APP_NAME}-prod -p '{"spec":{"selector":{"slot":"${targetSlot}"}}}'
    """
}

def rollbackProduction() {
    def currentSlot = getCurrentProductionSlot()
    def previousSlot = currentSlot == 'blue' ? 'green' : 'blue'
    switchProductionTraffic(previousSlot)
    echo "Rolled back to ${previousSlot} slot"
}

def createGitTag(String tag) {
    withCredentials([usernamePassword(credentialsId: 'git-credentials', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
        sh """
            git config user.email "jenkins@company.com"
            git config user.name "Jenkins CI"
            git tag -a ${tag} -m "Release ${tag}"
            git push https://${GIT_USER}:${GIT_TOKEN}@\${GIT_URL#https://} ${tag}
        """
    }
}

def getEnvironmentUrl(String environment, String slot = null) {
    def urls = [
        'dev': 'https://dev.app.company.com',
        'uat': 'https://uat.app.company.com',
        'production': slot ? "https://${slot}.prod.app.company.com" : 'https://app.company.com'
    ]
    return urls[environment]
}

def getReplicaCount(String environment) {
    def counts = ['dev': 1, 'uat': 2, 'production': 3]
    return counts[environment] ?: 1
}

def getMemoryRequest(String environment) {
    def memory = ['dev': '256Mi', 'uat': '512Mi', 'production': '1Gi']
    return memory[environment] ?: '256Mi'
}

def getMemoryLimit(String environment) {
    def memory = ['dev': '512Mi', 'uat': '1Gi', 'production': '2Gi']
    return memory[environment] ?: '512Mi'
}
