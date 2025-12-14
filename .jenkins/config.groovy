// =============================================================================
// CI/CD Configuration
// =============================================================================

class Config {
    // Application Settings
    static String APP_NAME = 'jenkins-test'
    static String GITHUB_USER = 'vanhiep99w'
    static String DOCKER_REGISTRY = 'ghcr.io'
    
    // Notification Channels
    static String SLACK_CHANNEL = '#ci-cd-notifications'
    static String DISCORD_CREDENTIAL_ID = 'discord-webhook-url'
    
    // Credentials IDs
    static String GITHUB_CREDENTIALS = 'github-credentials'
    static String ZAI_API_KEY = 'zai-api-key'
    
    // Timeouts (minutes)
    static int PIPELINE_TIMEOUT = 60
    static int DEPLOY_TIMEOUT = 10
    static int APPROVAL_TIMEOUT_HOURS = 24
    
    // Retry Settings
    static int TEST_RETRY_COUNT = 2
    static int HEALTH_CHECK_RETRIES = 10
    static int HEALTH_CHECK_INTERVAL = 15
    
    // Environment URLs
    static Map ENV_URLS = [
        dev: 'https://dev.app.company.com',
        uat: 'https://uat.app.company.com',
        production: 'https://app.company.com'
    ]
    
    // Resource Settings per Environment
    static Map REPLICA_COUNTS = [dev: 1, uat: 2, production: 3]
    static Map MEMORY_REQUESTS = [dev: '256Mi', uat: '512Mi', production: '1Gi']
    static Map MEMORY_LIMITS = [dev: '512Mi', uat: '1Gi', production: '2Gi']
    
    // Branch to Environment Mapping
    static Map BRANCH_ENV_MAP = [
        'dev': 'dev',
        'uat': 'uat',
        'main': 'production',
        'master': 'production',
        'production': 'production'
    ]
    
    // Approvers for Production
    static String PROD_APPROVERS = 'devops-team,tech-leads'
}

return Config
