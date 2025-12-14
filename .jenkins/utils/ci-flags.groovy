// =============================================================================
// CI Control Flags Parser
// =============================================================================

def parseFlags(String commitMsg) {
    def flags = [:]
    
    // Skip Flags
    flags.skipCi = commitMsg.contains('[skip ci]') || commitMsg.contains('[ci skip]')
    flags.skipTests = commitMsg.contains('[skip tests]')
    flags.skipReview = commitMsg.contains('[skip review]')
    flags.skipDeploy = commitMsg.contains('[skip deploy]')
    flags.skipDocker = commitMsg.contains('[skip docker]')
    flags.skipLint = commitMsg.contains('[skip lint]')
    flags.skipRelease = commitMsg.contains('[skip release]')
    
    // Action Flags
    flags.onlyBuild = commitMsg.contains('[only build]')
    flags.isWip = commitMsg.contains('[wip]')
    flags.isHotfix = commitMsg.contains('[hotfix]')
    flags.fullCi = commitMsg.contains('[full ci]')
    flags.isRollback = commitMsg.contains('[rollback]')
    
    // Deploy Flags
    flags.forceDev = commitMsg.contains('[deploy dev]')
    flags.forceUat = commitMsg.contains('[deploy uat]')
    flags.forceProd = commitMsg.contains('[deploy prod]')
    
    // Apply WIP mode
    if (flags.isWip) {
        flags.skipTests = true
        flags.skipDeploy = true
        flags.skipDocker = true
    }
    
    // Apply Hotfix mode
    if (flags.isHotfix) {
        flags.skipTests = true
        flags.skipReview = true
    }
    
    // Apply Full CI mode (override skips)
    if (flags.fullCi) {
        flags.skipTests = false
        flags.skipReview = false
        flags.skipDeploy = false
        flags.skipDocker = false
        flags.skipLint = false
        flags.skipRelease = false
    }
    
    return flags
}

def setEnvironmentFlags(flags) {
    env.SKIP_TESTS = flags.skipTests.toString()
    env.SKIP_REVIEW = flags.skipReview.toString()
    env.SKIP_DEPLOY = flags.skipDeploy.toString()
    env.SKIP_DOCKER = flags.skipDocker.toString()
    env.SKIP_LINT = flags.skipLint.toString()
    env.SKIP_RELEASE = flags.skipRelease.toString()
    env.ONLY_BUILD = flags.onlyBuild.toString()
    env.IS_WIP = flags.isWip.toString()
    env.IS_HOTFIX = flags.isHotfix.toString()
    env.FULL_CI = flags.fullCi.toString()
    env.IS_ROLLBACK = flags.isRollback.toString()
    env.FORCE_DEPLOY_DEV = flags.forceDev.toString()
    env.FORCE_DEPLOY_UAT = flags.forceUat.toString()
    env.FORCE_DEPLOY_PROD = flags.forceProd.toString()
}

def getActiveFlags(flags) {
    def active = []
    if (flags.skipTests) active.add('skip-tests')
    if (flags.skipReview) active.add('skip-review')
    if (flags.skipDeploy) active.add('skip-deploy')
    if (flags.skipDocker) active.add('skip-docker')
    if (flags.skipLint) active.add('skip-lint')
    if (flags.skipRelease) active.add('skip-release')
    if (flags.onlyBuild) active.add('only-build')
    if (flags.isWip) active.add('wip')
    if (flags.isHotfix) active.add('hotfix')
    if (flags.fullCi) active.add('full-ci')
    if (flags.isRollback) active.add('rollback')
    if (flags.forceDev) active.add('deploy-dev')
    if (flags.forceUat) active.add('deploy-uat')
    if (flags.forceProd) active.add('deploy-prod')
    return active
}

def shouldSkipStage(String stage) {
    switch(stage) {
        case 'tests':
            return env.SKIP_TESTS == 'true' || env.ONLY_BUILD == 'true' || env.IS_WIP == 'true'
        case 'lint':
            return env.SKIP_LINT == 'true'
        case 'deploy':
            return env.SKIP_DEPLOY == 'true' || env.ONLY_BUILD == 'true'
        case 'docker':
            return env.SKIP_DOCKER == 'true' || env.SKIP_DEPLOY == 'true' || env.ONLY_BUILD == 'true'
        case 'release':
            return env.SKIP_RELEASE == 'true' || env.ONLY_BUILD == 'true' || env.IS_WIP == 'true'
        case 'quality':
            return env.SKIP_TESTS == 'true' || env.ONLY_BUILD == 'true' || env.IS_WIP == 'true' || env.IS_HOTFIX == 'true'
        default:
            return false
    }
}

return this
