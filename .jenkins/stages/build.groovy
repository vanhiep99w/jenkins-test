// =============================================================================
// Build Stages
// =============================================================================

def call() {
    return {
        stage('Build') {
            steps {
                sh '''
                    echo "Building with cache..."
                    ./mvnw clean compile -DskipTests -B -V \
                        -Dmaven.repo.local=.m2/repository
                '''
            }
            post {
                failure {
                    script {
                        def notifications = load('.jenkins/utils/notifications.groovy')
                        notifications.send('FAILURE', 'Build failed!')
                    }
                }
            }
        }
    }
}

def commitlint() {
    return {
        stage('Commitlint') {
            when {
                expression { 
                    def ciFlags = load('.jenkins/utils/ci-flags.groovy')
                    return !ciFlags.shouldSkipStage('lint') && env.IS_ROLLBACK != 'true'
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
    }
}

def package_app() {
    return {
        stage('Package') {
            when {
                expression { 
                    def ciFlags = load('.jenkins/utils/ci-flags.groovy')
                    return env.AUTO_DEPLOY_TARGET != 'none' && 
                           !ciFlags.shouldSkipStage('deploy') &&
                           env.IS_ROLLBACK != 'true'
                }
            }
            steps {
                sh './mvnw package -DskipTests -B -Dmaven.repo.local=.m2/repository'
            }
        }
    }
}

return this
