// =============================================================================
// Jenkins CI/CD Pipeline
// =============================================================================
// Documentation: See COMMITLINT.md for CI control flags
// Structure: See .jenkins/ for modular components
// =============================================================================

pipeline {
    agent any

    environment {
        APP_VERSION = "${env.BUILD_NUMBER}-${env.GIT_COMMIT?.take(7) ?: 'local'}"
        BUILD_START_TIME = "${System.currentTimeMillis()}"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5'))
        timeout(time: 60, unit: 'MINUTES')
        timestamps()
        disableConcurrentBuilds(abortPrevious: true)
        skipStagesAfterUnstable()
    }

    triggers {
        githubPush()
    }

    stages {
        // =====================================================================
        // Phase 1: Initialization
        // =====================================================================
        stage('CI Control') {
            steps {
                script {
                    def ciFlags = load('.jenkins/utils/ci-flags.groovy')
                    def helpers = load('.jenkins/utils/helpers.groovy')
                    def config = load('.jenkins/config.groovy')
                    
                    // Get commit message and parse flags
                    def commitMsg = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                    def flags = ciFlags.parseFlags(commitMsg)
                    
                    // Check for skip ci
                    if (flags.skipCi) {
                        currentBuild.result = 'NOT_BUILT'
                        currentBuild.description = 'Skipped: [skip ci]'
                        error('Skipping build - [skip ci] detected')
                    }
                    
                    // Set environment variables
                    ciFlags.setEnvironmentFlags(flags)
                    env.APP_NAME = config.APP_NAME
                    env.DOCKER_IMAGE = "${config.DOCKER_REGISTRY}/${config.GITHUB_USER}/${config.APP_NAME}"
                    
                    // Log active flags
                    def activeFlags = ciFlags.getActiveFlags(flags)
                    if (activeFlags.size() > 0) {
                        echo "CI Flags: ${activeFlags.join(', ')}"
                        currentBuild.description = "Flags: ${activeFlags.join(', ')}"
                    }
                    
                    // Determine deploy target
                    def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: ''
                    env.AUTO_DEPLOY_TARGET = helpers.determineDeployTarget(branch, flags)
                    
                    // Set build display
                    currentBuild.displayName = "#${BUILD_NUMBER} - ${branch.replaceAll('origin/', '')}"
                }
            }
        }

        stage('Rollback') {
            when { expression { return env.IS_ROLLBACK == 'true' } }
            steps {
                script {
                    def deploy = load('.jenkins/stages/deploy.groovy')
                    def notifications = load('.jenkins/utils/notifications.groovy')
                    
                    echo "Initiating rollback..."
                    deploy.rollback()
                    currentBuild.description = "Rollback completed"
                    notifications.send('SUCCESS', 'Rollback completed')
                }
            }
        }

        // =====================================================================
        // Phase 2: Build & Validate
        // =====================================================================
        stage('Checkout') {
            when { expression { return env.IS_ROLLBACK != 'true' } }
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_MSG = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                    env.GIT_AUTHOR = sh(script: 'git log -1 --pretty=%an', returnStdout: true).trim()
                }
            }
        }

        stage('Commitlint') {
            when {
                expression { 
                    return env.IS_ROLLBACK != 'true' && env.SKIP_LINT != 'true'
                }
            }
            steps {
                sh '''
                    echo "Validating commit message..."
                    bun install --frozen-lockfile
                    git log -1 --pretty=%B > .commit-msg-temp
                    bun x commitlint --edit .commit-msg-temp
                    rm -f .commit-msg-temp
                '''
            }
        }

        stage('Build') {
            when { expression { return env.IS_ROLLBACK != 'true' } }
            steps {
                sh '''
                    echo "Building..."
                    ./mvnw clean compile -DskipTests -B -V -Dmaven.repo.local=.m2/repository
                '''
            }
        }

        // =====================================================================
        // Phase 3: Test & Quality
        // =====================================================================
        stage('Tests') {
            when {
                expression { 
                    return env.IS_ROLLBACK != 'true' && 
                           env.SKIP_TESTS != 'true' && 
                           env.ONLY_BUILD != 'true' &&
                           env.IS_WIP != 'true'
                }
            }
            parallel {
                stage('Unit Tests') {
                    steps {
                        retry(2) {
                            sh './mvnw test -B -Dmaven.repo.local=.m2/repository'
                        }
                    }
                    post {
                        always {
                            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                        }
                    }
                }
                stage('Integration Tests') {
                    steps {
                        retry(2) {
                            sh './mvnw verify -DskipUnitTests -B -Dmaven.repo.local=.m2/repository'
                        }
                    }
                    post {
                        always {
                            junit testResults: '**/target/failsafe-reports/*.xml', allowEmptyResults: true
                        }
                    }
                }
            }
        }

        stage('Code Quality') {
            when {
                expression { 
                    return env.IS_ROLLBACK != 'true' && 
                           env.SKIP_TESTS != 'true' && 
                           env.ONLY_BUILD != 'true' &&
                           env.IS_WIP != 'true' &&
                           env.IS_HOTFIX != 'true'
                }
            }
            parallel {
                stage('Security Scan') {
                    steps {
                        sh './mvnw dependency-check:check -B -Dmaven.repo.local=.m2/repository || true'
                    }
                }
                stage('Coverage') {
                    steps {
                        sh './mvnw jacoco:report -B -Dmaven.repo.local=.m2/repository || true'
                    }
                }
            }
        }

        // =====================================================================
        // Phase 4: Package & Deploy
        // =====================================================================
        stage('Package') {
            when {
                expression { 
                    return env.AUTO_DEPLOY_TARGET != 'none' && 
                           env.SKIP_DEPLOY != 'true' && 
                           env.ONLY_BUILD != 'true' &&
                           env.IS_ROLLBACK != 'true'
                }
            }
            steps {
                sh './mvnw package -DskipTests -B -Dmaven.repo.local=.m2/repository'
            }
        }

        stage('Docker') {
            when {
                expression { 
                    return env.AUTO_DEPLOY_TARGET != 'none' && 
                           env.SKIP_DOCKER != 'true' && 
                           env.SKIP_DEPLOY != 'true' && 
                           env.ONLY_BUILD != 'true' &&
                           env.IS_ROLLBACK != 'true'
                }
            }
            stages {
                stage('Build Image') {
                    steps {
                        sh """
                            docker build --build-arg JAR_FILE=target/*.jar -t ${env.DOCKER_IMAGE}:${APP_VERSION} .
                            docker tag ${env.DOCKER_IMAGE}:${APP_VERSION} ${env.DOCKER_IMAGE}:latest
                        """
                    }
                }
                stage('Push Image') {
                    steps {
                        script {
                            def config = load('.jenkins/config.groovy')
                            withCredentials([usernamePassword(credentialsId: config.GITHUB_CREDENTIALS, usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                                sh """
                                    echo \$PASS | docker login ${config.DOCKER_REGISTRY} -u \$USER --password-stdin
                                    docker push ${env.DOCKER_IMAGE}:${APP_VERSION}
                                    docker push ${env.DOCKER_IMAGE}:latest
                                    docker logout ${config.DOCKER_REGISTRY}
                                """
                            }
                        }
                    }
                    post {
                        always {
                            sh """
                                docker rmi ${env.DOCKER_IMAGE}:${APP_VERSION} || true
                                docker rmi ${env.DOCKER_IMAGE}:latest || true
                            """
                        }
                    }
                }
            }
        }

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
                        return env.IS_ROLLBACK != 'true' && 
                               env.SKIP_RELEASE != 'true' &&
                               env.ONLY_BUILD != 'true' &&
                               env.IS_WIP != 'true'
                    }
                }
            }
            steps {
                script {
                    def config = load('.jenkins/config.groovy')
                    withCredentials([usernamePassword(credentialsId: config.GITHUB_CREDENTIALS, usernameVariable: 'GH_USER', passwordVariable: 'GITHUB_TOKEN')]) {
                        sh 'HUSKY=0 bun run release'
                    }
                }
            }
        }

        // =====================================================================
        // Phase 5: Deploy to Environments
        // =====================================================================
        stage('Deploy UAT') {
            when {
                expression { 
                    return env.AUTO_DEPLOY_TARGET in ['uat', 'production'] && 
                           env.SKIP_DEPLOY != 'true' && 
                           env.ONLY_BUILD != 'true' &&
                           env.IS_ROLLBACK != 'true'
                }
            }
            steps {
                script {
                    def deploy = load('.jenkins/stages/deploy.groovy')
                    deploy.deployToEnvironment('uat')
                }
            }
            post {
                success {
                    script {
                        def deploy = load('.jenkins/stages/deploy.groovy')
                        deploy.runSmokeTests('uat')
                    }
                }
            }
        }

        stage('Production') {
            when {
                expression { 
                    return env.AUTO_DEPLOY_TARGET == 'production' && 
                           env.SKIP_DEPLOY != 'true' && 
                           env.ONLY_BUILD != 'true' &&
                           env.IS_ROLLBACK != 'true'
                }
            }
            stages {
                stage('Approval') {
                    steps {
                        script {
                            def config = load('.jenkins/config.groovy')
                            timeout(time: config.APPROVAL_TIMEOUT_HOURS, unit: 'HOURS') {
                                input(
                                    message: 'Deploy to Production?',
                                    ok: 'Deploy',
                                    submitter: config.PROD_APPROVERS
                                )
                            }
                        }
                    }
                }
                stage('Deploy') {
                    steps {
                        script {
                            def deploy = load('.jenkins/stages/deploy.groovy')
                            def currentSlot = deploy.getCurrentProductionSlot()
                            def targetSlot = currentSlot == 'blue' ? 'green' : 'blue'
                            
                            deploy.deployToEnvironment('production', targetSlot)
                            
                            if (deploy.runHealthCheck('production', targetSlot)) {
                                deploy.switchProductionTraffic(targetSlot)
                                echo "Deployed to ${targetSlot} slot"
                            } else {
                                error "Health check failed"
                            }
                        }
                    }
                    post {
                        success {
                            script {
                                def deploy = load('.jenkins/stages/deploy.groovy')
                                deploy.runSmokeTests('production')
                            }
                        }
                        failure {
                            script {
                                def deploy = load('.jenkins/stages/deploy.groovy')
                                deploy.rollback()
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // Post Actions
    // =========================================================================
    post {
        always {
            script {
                def helpers = load('.jenkins/utils/helpers.groovy')
                helpers.recordMetrics()
            }
            cleanWs(
                deleteDirs: true,
                patterns: [[pattern: '.m2/repository/**', type: 'EXCLUDE']]
            )
        }
        success {
            script {
                def notifications = load('.jenkins/utils/notifications.groovy')
                def helpers = load('.jenkins/utils/helpers.groovy')
                notifications.send('SUCCESS')
                if (env.AUTO_DEPLOY_TARGET == 'production') {
                    helpers.createGitTag("v${APP_VERSION}")
                }
            }
        }
        failure {
            script {
                def notifications = load('.jenkins/utils/notifications.groovy')
                notifications.send('FAILURE', 'Pipeline failed!')
            }
        }
        unstable {
            script {
                def notifications = load('.jenkins/utils/notifications.groovy')
                notifications.send('UNSTABLE')
            }
        }
        aborted {
            script {
                def notifications = load('.jenkins/utils/notifications.groovy')
                notifications.send('ABORTED')
            }
        }
    }
}
