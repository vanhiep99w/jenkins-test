// =============================================================================
// Code Quality Stages
// =============================================================================

def call() {
    return {
        stage('Code Quality') {
            when {
                expression { 
                    def ciFlags = load('.jenkins/utils/ci-flags.groovy')
                    return !ciFlags.shouldSkipStage('quality') && env.IS_ROLLBACK != 'true'
                }
            }
            parallel {
                stage('Security Scan') {
                    steps {
                        sh '''
                            echo "Running security scan..."
                            ./mvnw dependency-check:check -B -Dmaven.repo.local=.m2/repository || true
                        '''
                    }
                }
                stage('Code Coverage') {
                    steps {
                        sh '''
                            echo "Generating coverage report..."
                            ./mvnw jacoco:report -B -Dmaven.repo.local=.m2/repository || true
                        '''
                    }
                    post {
                        always {
                            jacoco(
                                execPattern: '**/target/jacoco.exec',
                                classPattern: '**/target/classes',
                                sourcePattern: '**/src/main/java',
                                exclusionPattern: '**/test/**'
                            )
                        }
                    }
                }
            }
        }
    }
}

return this
