// =============================================================================
// Notification Utilities
// =============================================================================

def send(String status, String message = null) {
    def config = load('.jenkins/config.groovy')
    def duration = getBuildDuration()
    def emoji = getEmoji(status)
    def color = getColor(status)
    
    def summary = """
*${config.APP_NAME}* - Build #${BUILD_NUMBER}
*Status:* ${status} ${emoji}
*Branch:* ${env.GIT_BRANCH ?: 'N/A'}
*Commit:* ${env.GIT_COMMIT?.take(7) ?: 'N/A'}
*Author:* ${env.GIT_AUTHOR ?: 'N/A'}
*Duration:* ${duration}
*Deploy Target:* ${env.AUTO_DEPLOY_TARGET ?: 'none'}
<${BUILD_URL}|View Build>
"""
    
    // Log notification
    echo "Notification [${status}]: ${message ?: 'Build completed'}"
    
    // Send to Slack (uncomment to enable)
    // sendSlack(config.SLACK_CHANNEL, color, message ? "${message}\n${summary}" : summary)
    
    // Send to Discord (uncomment to enable)
    // sendDiscord(config.DISCORD_CREDENTIAL_ID, status, message, duration)
}

def sendSlack(String channel, String color, String message) {
    try {
        slackSend(
            channel: channel,
            color: color,
            message: message
        )
    } catch (Exception e) {
        echo "Slack notification failed: ${e.message}"
    }
}

def sendDiscord(String credentialId, String status, String message, String duration) {
    try {
        withCredentials([string(credentialsId: credentialId, variable: 'WEBHOOK_URL')]) {
            def color = status == 'SUCCESS' ? 3066993 : (status == 'FAILURE' ? 15158332 : 16776960)
            def payload = """{"embeds":[{"title":"${env.APP_NAME} - Build #${BUILD_NUMBER}","description":"${status}: ${message ?: 'Build completed'}","color":${color},"fields":[{"name":"Branch","value":"${env.GIT_BRANCH ?: 'N/A'}","inline":true},{"name":"Duration","value":"${duration}","inline":true}],"url":"${BUILD_URL}"}]}"""
            sh "curl -s -H 'Content-Type: application/json' -d '${payload}' \${WEBHOOK_URL}"
        }
    } catch (Exception e) {
        echo "Discord notification failed: ${e.message}"
    }
}

def getEmoji(String status) {
    switch(status) {
        case 'SUCCESS': return ':white_check_mark:'
        case 'FAILURE': return ':x:'
        case 'UNSTABLE': return ':warning:'
        case 'ABORTED': return ':no_entry:'
        default: return ':question:'
    }
}

def getColor(String status) {
    switch(status) {
        case 'SUCCESS': return 'good'
        case 'FAILURE': return 'danger'
        default: return 'warning'
    }
}

def getBuildDuration() {
    def duration = System.currentTimeMillis() - env.BUILD_START_TIME.toLong()
    def seconds = (duration / 1000).toInteger()
    def minutes = (seconds / 60).toInteger()
    seconds = seconds % 60
    return "${minutes}m ${seconds}s"
}

return this
