import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class StableVersionsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Apply the versions plugin
        project.plugins.apply("com.github.ben-manes.versions")

        project.tasks.withType(DependencyUpdatesTask::class.java).configureEach {
            rejectVersionIf {
                candidate.version.isNonStable()
            }
        }
    }

    /**
     * Filter out non-stable version when checking for dependency updates
     */
    private fun String.isNonStable(): Boolean {
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { uppercase().contains(it) }
        val regex = "^[0-9,.v-]+(-r)?$".toRegex()
        val isStable = stableKeyword || regex.matches(this)
        return isStable.not()
    }
}
