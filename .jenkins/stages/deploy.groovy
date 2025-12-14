// =============================================================================
// Deploy Stages
// =============================================================================

def dockerBuild() {
    def config = load('.jenkins/config.groovy')
    
    return {
        stage('Build Docker Image') {
            when {
                expression { 
                    def ciFlags = load('.jenkins/utils/ci-flags.groovy')
                    return env.AUTO_DEPLOY_TARGET != 'none' && 
                           !ciFlags.shouldSkipStage('docker') &&
                           env.IS_ROLLBACK != 'true'
                }
            }
            steps {
                sh """
                    docker build --build-arg JAR_FILE=target/*.jar \
                        -t ${config.DOCKER_REGISTRY}/${config.GITHUB_USER}/${config.APP_NAME}:${env.APP_VERSION} .
                    docker tag ${config.DOCKER_REGISTRY}/${config.GITHUB_USER}/${config.APP_NAME}:${env.APP_VERSION} \
                        ${config.DOCKER_REGISTRY}/${config.GITHUB_USER}/${config.APP_NAME}:latest
                """
            }
        }
    }
}

def dockerPush() {
    def config = load('.jenkins/config.groovy')
    
    return {
        stage('Push Docker Image') {
            when {
                expression { 
                    def ciFlags = load('.jenkins/utils/ci-flags.groovy')
                    return env.AUTO_DEPLOY_TARGET != 'none' && 
                           !ciFlags.shouldSkipStage('docker') &&
                           env.IS_ROLLBACK != 'true'
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: config.GITHUB_CREDENTIALS, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh """
                        echo \$DOCKER_PASS | docker login ${config.DOCKER_REGISTRY} -u \$DOCKER_USER --password-stdin
                        docker push ${config.DOCKER_REGISTRY}/${config.GITHUB_USER}/${config.APP_NAME}:${env.APP_VERSION}
                        docker push ${config.DOCKER_REGISTRY}/${config.GITHUB_USER}/${config.APP_NAME}:latest
                        docker logout ${config.DOCKER_REGISTRY}
                    """
                }
            }
            post {
                always {
                    sh """
                        docker rmi ${config.DOCKER_REGISTRY}/${config.GITHUB_USER}/${config.APP_NAME}:${env.APP_VERSION} || true
                        docker rmi ${config.DOCKER_REGISTRY}/${config.GITHUB_USER}/${config.APP_NAME}:latest || true
                        docker builder prune --keep-storage=500MB -f || true
                    """
                }
            }
        }
    }
}

def release() {
    def config = load('.jenkins/config.groovy')
    
    return {
        stage('Release') {
            when {
                allOf {
                    anyOf {
                        branch 'main'
                        branch 'master'
                        branch 'dev'
                        branch 'uat'
                    }
                    expression { 
                        def ciFlags = load('.jenkins/utils/ci-flags.groovy')
                        return !ciFlags.shouldSkipStage('release') && env.IS_ROLLBACK != 'true'
                    }
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: config.GITHUB_CREDENTIALS, usernameVariable: 'GH_USER', passwordVariable: 'GITHUB_TOKEN')]) {
                    sh '''
                        echo "Running semantic-release..."
                        bun install --frozen-lockfile
                        HUSKY=0 bun run release
                    '''
                }
            }
        }
    }
}

def deployToEnvironment(String environment, String slot = null) {
    def config = load('.jenkins/config.groovy')
    def helpers = load('.jenkins/utils/helpers.groovy')
    
    def namespace = "app-${environment}"
    def helmRelease = slot ? "${config.APP_NAME}-${slot}" : config.APP_NAME
    
    withCredentials([file(credentialsId: "kubeconfig-${environment}", variable: 'KUBECONFIG')]) {
        sh """
            helm upgrade --install ${helmRelease} ./helm/${config.APP_NAME} \
                --namespace ${namespace} \
                --set image.repository=${config.DOCKER_REGISTRY}/${config.GITHUB_USER}/${config.APP_NAME} \
                --set image.tag=${env.APP_VERSION} \
                --set environment=${environment} \
                --set replicaCount=${helpers.getReplicaCount(environment)} \
                --set resources.requests.memory=${helpers.getMemoryRequest(environment)} \
                --set resources.limits.memory=${helpers.getMemoryLimit(environment)} \
                --wait \
                --timeout ${config.DEPLOY_TIMEOUT}m
        """
    }
}

def runSmokeTests(String environment) {
    def helpers = load('.jenkins/utils/helpers.groovy')
    def baseUrl = helpers.getEnvironmentUrl(environment)
    
    sh """
        echo "Running smoke tests against ${baseUrl}"
        curl -f ${baseUrl}/api/v1/health || exit 1
        curl -f ${baseUrl}/api/v1/info || exit 1
        echo "Smoke tests passed!"
    """
}

def runHealthCheck(String environment, String slot) {
    def config = load('.jenkins/config.groovy')
    def helpers = load('.jenkins/utils/helpers.groovy')
    def baseUrl = helpers.getEnvironmentUrl(environment, slot)
    
    for (int i = 0; i < config.HEALTH_CHECK_RETRIES; i++) {
        def result = sh(script: "curl -sf ${baseUrl}/api/v1/health", returnStatus: true)
        if (result == 0) return true
        sleep(config.HEALTH_CHECK_INTERVAL)
    }
    return false
}

def getCurrentProductionSlot() {
    def config = load('.jenkins/config.groovy')
    return sh(
        script: "kubectl get service ${config.APP_NAME}-prod -o jsonpath='{.spec.selector.slot}' 2>/dev/null || echo 'blue'",
        returnStdout: true
    ).trim()
}

def switchProductionTraffic(String targetSlot) {
    def config = load('.jenkins/config.groovy')
    sh "kubectl patch service ${config.APP_NAME}-prod -p '{\"spec\":{\"selector\":{\"slot\":\"${targetSlot}\"}}}'"
}

def rollback() {
    def currentSlot = getCurrentProductionSlot()
    def previousSlot = currentSlot == 'blue' ? 'green' : 'blue'
    switchProductionTraffic(previousSlot)
    echo "Rolled back to ${previousSlot} slot"
}

return this
