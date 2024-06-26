import org.slf4j.LoggerFactory

apply<AllowedRepositoryPlugin>()

class AllowedRepositoryPlugin : Plugin<Gradle> {
    val logger = LoggerFactory.getLogger("allowed-repository-plugin")

    companion object {

        // Also see org.gradle.api.internal.artifacts.BaseRepositoryFactory.PLUGIN_PORTAL_OVERRIDE_URL_PROPERTY
        // (val PLUGIN_PORTAL_OVERRIDE_URL_PROPERTY: String? = "org.gradle.internal.plugins.portal.url.override")
        val allowedRepositories =
            listOf<String>("https://repo.gradle.org/gradle/m2?id=repo1", "https://repo.gradle.org/gradle/m2?id=repo2")
        val allowedPluginRepositories =
            listOf<String>("https://plugins.gradle.org/m2?id=repo1", "https://plugins.gradle.org/m2?id=repo2")
    }

    override fun apply(gradle: Gradle) {
        gradle.settingsEvaluated {
            settings.pluginManagement {
                repositories {
                    var added = false
                    all {
                        if (this !is MavenArtifactRepository || url.toString() !in allowedPluginRepositories) {
                            logger.warn("[settings] Plugin Repository ${(this as? MavenArtifactRepository)?.url ?: name} removed. Only ${allowedPluginRepositories} is allowed")
                            remove(this)
                            if (!added) {
                                var idx = 0
                                allowedPluginRepositories.forEach {
                                    add(maven {
                                        name = "PLUGIN_REPOSITORY_URL_$idx"
                                        url = java.net.URI(it)
                                    })
                                    idx++
                                }
                            }
                        }
                    }
                }
            }

            settings.dependencyResolutionManagement {
                repositories {
                    var added = false
                    all {
                        if (this !is MavenArtifactRepository || url.toString() !in allowedRepositories) {
                            logger.warn("[settings] Repository ${(this as? MavenArtifactRepository)?.url ?: name} removed. Only $allowedRepositories allowed")
                            remove(this)
                            if (!added) {
                                var idx = 0
                                allowedRepositories.forEach {
                                    add(maven {
                                        name = "ALLOWED_REPOSITORY_URL_$idx"
                                        url = java.net.URI(it)
                                    })
                                    idx++
                                    added = true
                                }
                            }
                        }
                    }
                }
            }
        }

        // ONLY USE ALLOWED REPOSITORIES FOR DEPENDENCIES
        gradle.allprojects {
            repositories {
                // Remove all repositories not pointing to the allowed repository url
                all {
                    project.logger.lifecycle("[${project.name}] Checking Repository ${this}")
                    if (this !is MavenArtifactRepository || url.toString() !in allowedRepositories) {
                        project.logger.lifecycle("[${project.name}] Repository ${(this as? MavenArtifactRepository)?.url ?: name} removed. Only $allowedRepositories is allowed")
                        remove(this)
                    }
                }

                // Add the allowed repositories
                var idx = 0
                allowedRepositories.forEach {
                    add(maven {
                        name = "ALLOWED_REPOSITORY_URL_$idx"
                        url = java.net.URI(it)
                    })
                    idx++
                }
            }
        }
    }
}