import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.Project
import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.authentication.http.HttpHeaderAuthentication

/**
 * Adds GitLab Maven repository to the specified RepositoryHandler
 */
fun RepositoryHandler.mavenGitlab(project: Project) {
    maven {
        name = "mavenGitLab"
        url = project.uri("https://gitlab.allette.com.au/api/v4/projects/276/packages/maven")
        credentials(HttpHeaderCredentials::class.java) {
            name = "Private-Token"
            value = project.findProperty("gitlabPrivateToken") as String?
                ?: throw org.gradle.api.GradleException("gitlabPrivateToken property must be set in gradle.properties")
        }
        authentication {
            register("header", HttpHeaderAuthentication::class.java)
        }
    }
}
