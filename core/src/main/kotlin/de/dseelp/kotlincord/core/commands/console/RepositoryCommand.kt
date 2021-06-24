/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package de.dseelp.kotlincord.core.commands.console

import de.dseelp.kommon.command.CommandBuilder
import de.dseelp.kommon.command.CommandContext
import de.dseelp.kommon.command.CommandNode
import de.dseelp.kommon.command.arguments.StringArgument
import de.dseelp.kotlincord.api.InternalKotlinCordApi
import de.dseelp.kotlincord.api.Version
import de.dseelp.kotlincord.api.command.Command
import de.dseelp.kotlincord.api.command.ConsoleSender
import de.dseelp.kotlincord.api.isValidUrl
import de.dseelp.kotlincord.api.plugins.repository.Repository
import de.dseelp.kotlincord.api.plugins.repository.RepositoryIndex
import de.dseelp.kotlincord.api.plugins.repository.RepositoryManager
import de.dseelp.kotlincord.api.round
import de.dseelp.kotlincord.api.utils.CommandScope
import de.dseelp.kotlincord.api.utils.koin.CordKoinComponent
import de.dseelp.kotlincord.api.utils.literal
import de.dseelp.kotlincord.api.utils.merge
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject
import kotlin.system.measureTimeMillis

@OptIn(InternalKotlinCordApi::class)
object RepositoryCommand : Command<ConsoleSender>, CordKoinComponent {
    val repositoryManager: RepositoryManager by inject()
    override val scopes: Array<CommandScope> = arrayOf(CommandScope.CONSOLE)
    override val node: CommandNode<ConsoleSender> = literal("repository", arrayOf("repos", "repositories")) {
        literal("list") {
            execute {
                val repositories = repositoryManager.repositories
                sender.sendMessage(buildString {
                    repositories.onEach {
                        append("${it.name}:${it.url}" + System.lineSeparator())
                    }
                    if (repositories.isEmpty()) append("No Repositories found!")
                }.removeLastLineSeparator())
            }
        }

        literal("update") {
            execute {
                if (repositoryManager.repositories.isEmpty()) {
                    sender.sendMessage("There are no repositories to update.")
                    return@execute
                }
                sender.sendMessage("Updating repositories...")
                val took = measureTimeMillis {
                    runBlocking {
                        repositoryManager.updateIndexes()
                    }
                }
                if (took == 0L) {
                    sender.sendMessage("Update completed instantaneous.")
                    return@execute
                }
                val dV = took.toDouble() / 1000
                sender.sendMessage("Update complete. Took ${dV.round(2)}s")
            }
        }

        literal("add") {
            execute {
                sender.sendMessage("Please use: repositories add <Url>")
            }
            argument(StringArgument("url")) {
                execute {
                    val raw: String = get("url")
                    val catching = runCatching { Url(raw) }
                    if (catching.isFailure || !isValidUrl(raw)) {
                        sender.sendMessage("$raw is not a valid url!")
                        return@execute
                    }
                    val url = catching.getOrThrow()
                    val repo = repositoryManager.addRepository(url.toString())
                    repositoryManager.reloadRepositories()
                    sender.sendMessage("The repository ${repo.name} (${repo.url}) was added")
                }
            }
        }
        literal("remove") {
            execute {
                sender.sendMessage("Please use: repositories add <Url/Name>")
            }
            argument(StringArgument("url/name")) {
                execute {
                    val repositories = repositoryManager.repositories
                    val raw: String = get("url/name")
                    val url = runCatching { Url(raw) }.isSuccess
                    val isUrl = isValidUrl(raw) && url
                    val repo = repositories.firstOrNull { raw == if (isUrl) it.url else it.name }
                    if (repo == null) {
                        sender.sendMessage("Failed to find repo with ${if (isUrl) "url" else "name"} $raw")
                        return@execute
                    }
                    repositoryManager.removeRepository(repo.url)
                    repositoryManager.reloadRepositories()
                    sender.sendMessage("The repository ${repo.name} (${repo.url}) was removed")
                }
            }
        }

        literal("install") {

            argument(StringArgument("term")) {
                execute {
                    val raw: String = get("term")
                    val splitted = raw.split('@')
                    if (!raw.contains('@') || splitted.size < 2) {
                        sender.sendMessage(
                            "Please provide the version you want to install.",
                            "If you want to install the latest version just specify 'latest'."
                        )
                        return@execute
                    }
                    val versionString = splitted[1]
                    val latest = versionString.lowercase() == "latest"
                    val version = runCatching { Version.parse(versionString) }.getOrNull()
                    if (version == null && !latest) {
                        sender.sendMessage("$versionString is not a valid version!")
                        return@execute
                    }
                    val results = findResults(exact = true, mustHaveArtifactId = true) {
                        val index = it.indexOf('@')
                        it.substring(0 until index)
                    } ?: return@execute
                    val merged = merge(*results.values.toTypedArray())
                    if (merged.size > 1) {
                        sender.sendMessage("An unknown error occured #232. Please report this error to an maintainer of the KotlinCord project.")
                        return@execute
                    }
                    val index = merged[0]
                    runBlocking {
                        val foundPackage = index.asPackage(results.keys.first())
                        sender.sendMessage("Installing package...")
                        if (latest) foundPackage.installLatest()
                        else foundPackage.install(version!!)
                    }
                }
            }
        }

        node(searchNode(false, nodeName = "search") {
            node(CommandNode("exact", target = searchNode(true), noAccess = null, executor = null))
        })
    }

    fun searchNode(
        exact: Boolean,
        nodeName: String = "",
        builder: CommandBuilder<ConsoleSender>.() -> Unit = {}
    ): CommandNode<ConsoleSender> = literal(nodeName, arrayOf()) {
        execute {
            sender.sendMessage("Please provide a search term")
        }

        argument(StringArgument("term")) {
            execute {
                val results = findResults(exact) ?: return@execute
                sender.sendMessage(buildString {
                    for (result in results.toList()) {
                        val repo = result.first
                        append(repo.name)
                        append(System.lineSeparator())
                        append(buildString {
                            result.second.onEach {
                                append("${it.groupId}:${it.artifactId}${System.lineSeparator()}")
                            }
                        }.removeLastLineSeparator().lines().map { "    $it" }.joinToString(System.lineSeparator()))
                    }
                })
            }
        }
        apply(builder)
    }

    fun CommandContext<ConsoleSender>.findResults(
        exact: Boolean,
        mustHaveArtifactId: Boolean = false,
        termModifier: (String) -> String = { it }
    ): Map<Repository, Array<RepositoryIndex>>? {
        val term: String = termModifier(get("term"))
        val results = (if (term.contains(':')) {
            val splitted = term.split(':')
            if (splitted.size > 2) {
                sender.sendMessage("A search term can only contain 1 ':'!")
                return null
            }
            repositoryManager.find(splitted[0], splitted[1], exact, exact)
        } else if (!mustHaveArtifactId) {
            repositoryManager.find(term, exact)
        } else null)?.filter { it.value.isNotEmpty() }
        if (results == null) {
            sender.sendMessage("You must provide a groupId and artifactId.")
            return null
        }
        if (results.isEmpty()) {
            sender.sendMessage("No results found for \"$term\"")
            return null
        }
        return results
    }

    fun String.removeLastLineSeparator(): String {
        val index = lastIndexOf(System.lineSeparator())
        if (index == -1) return this
        return substring(0, index)
    }
}