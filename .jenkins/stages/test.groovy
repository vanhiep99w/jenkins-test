// =============================================================================
// Test Stages
// =============================================================================

def call() {
    def config = load('.jenkins/config.groovy')
    
    return {
        stage('Tests') {
            when {
                expression { 
                    def ciFlags = load('.jenkins/utils/ci-flags.groovy')
                    return !ciFlags.shouldSkipStage('tests') && env.IS_ROLLBACK != 'true'
                }
            }
            parallel {
                stage('Unit Tests') {
                    steps {
                        retry(config.TEST_RETRY_COUNT) {
                            sh './mvnw test -B -Dmaven.repo.local=.m2/repository'
                        }
                    }
                    post {
                        always {
                            junit(
                                testResults: '**/target/surefire-reports/*.xml',
                                allowEmptyResults: true
                            )
                        }
                    }
                }
                stage('Integration Tests') {
                    steps {
                        retry(config.TEST_RETRY_COUNT) {
                            sh './mvnw verify -DskipUnitTests -B -Dmaven.repo.local=.m2/repository'
                        }
                    }
                    post {
                        always {
                            junit(
                                testResults: '**/target/failsafe-reports/*.xml',
                                allowEmptyResults: true
                            )
                        }
                    }
                }
            }
        }
    }
}

return this
