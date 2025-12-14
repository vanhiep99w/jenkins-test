// =============================================================================
// Helper Functions
// =============================================================================

def getConfig() {
    return load('.jenkins/config.groovy')
}

def determineDeployTarget(String branch, flags) {
    def config = getConfig()
    
    // Check force deploy flags first
    if (flags.forceProd) return 'production'
    if (flags.forceUat) return 'uat'
    if (flags.forceDev) return 'dev'
    
    // Check if PR
    if (env.CHANGE_ID) return 'none'
    
    // Map branch to environment
    def cleanBranch = branch.replaceAll('origin/', '')
    def target = config.BRANCH_ENV_MAP[cleanBranch] ?: 'none'
    
    // Hotfix mode forces deploy if on feature branch
    if (flags.isHotfix && target == 'none') {
        return 'dev'
    }
    
    return target
}

def getEnvironmentUrl(String environment, String slot = null) {
    def config = getConfig()
    if (environment == 'production' && slot) {
        return "https://${slot}.prod.app.company.com"
    }
    return config.ENV_URLS[environment] ?: config.ENV_URLS.dev
}

def getReplicaCount(String environment) {
    def config = getConfig()
    return config.REPLICA_COUNTS[environment] ?: 1
}

def getMemoryRequest(String environment) {
    def config = getConfig()
    return config.MEMORY_REQUESTS[environment] ?: '256Mi'
}

def getMemoryLimit(String environment) {
    def config = getConfig()
    return config.MEMORY_LIMITS[environment] ?: '512Mi'
}

def recordMetrics() {
    def notifications = load('.jenkins/utils/notifications.groovy')
    def metrics = [
        buildNumber: BUILD_NUMBER,
        duration: notifications.getBuildDuration(),
        status: currentBuild.currentResult,
        branch: env.GIT_BRANCH ?: 'unknown',
        deployTarget: env.AUTO_DEPLOY_TARGET ?: 'none',
        timestamp: new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'"),
        flags: [
            skipTests: env.SKIP_TESTS,
            skipDeploy: env.SKIP_DEPLOY,
            hotfix: env.IS_HOTFIX,
            wip: env.IS_WIP
        ]
    ]
    echo "Build Metrics: ${metrics}"
}

def createGitTag(String tag) {
    def config = getConfig()
    withCredentials([usernamePassword(credentialsId: config.GITHUB_CREDENTIALS, usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
        sh """
            git config user.email "jenkins@company.com"
            git config user.name "Jenkins CI"
            git tag -a ${tag} -m "Release ${tag}"
            git push https://\${GIT_USER}:\${GIT_TOKEN}@\${GIT_URL#https://} ${tag}
        """
    }
}

return this
