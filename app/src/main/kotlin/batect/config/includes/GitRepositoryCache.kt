/*
   Copyright 2017-2020 Charles Korn.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package batect.config.includes

import batect.VersionInfo
import batect.git.GitClient
import batect.io.ApplicationPaths
import batect.os.deleteDirectory
import batect.primitives.mapToSet
import batect.utils.Json
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlinx.serialization.json.JsonException
import kotlinx.serialization.json.JsonLiteral
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.json

class GitRepositoryCache(
    private val applicationPaths: ApplicationPaths,
    private val gitClient: GitClient,
    private val versionInfo: VersionInfo,
    private val listener: GitRepositoryCacheNotificationListener,
    private val timeSource: TimeSource = ZonedDateTime::now
) {
    private val gitCacheDirectory = applicationPaths.rootLocalStorageDirectory.resolve("incl").toAbsolutePath()

    fun ensureCached(repo: GitRepositoryReference): Path {
        Files.createDirectories(gitCacheDirectory)

        val workingCopyPath = gitCacheDirectory.resolve(repo.cacheKey)
        val infoPath = gitCacheDirectory.resolve("${repo.cacheKey}.json")
        val now = timeSource()

        cloneRepoIfMissing(repo, workingCopyPath)
        updateInfoFile(repo, infoPath, now)

        return workingCopyPath
    }

    private fun cloneRepoIfMissing(repo: GitRepositoryReference, workingCopyPath: Path) {
        if (!Files.exists(workingCopyPath)) {
            listener.onCloning(repo)
            gitClient.clone(repo.remote, repo.ref, workingCopyPath)
            listener.onCloneComplete()
        }
    }

    private fun updateInfoFile(repo: GitRepositoryReference, infoPath: Path, lastUsed: ZonedDateTime) {
        val existingContent = if (Files.exists(infoPath)) {
            Json.default.parseJson(Files.readAllBytes(infoPath).toString(Charsets.UTF_8)).jsonObject
        } else {
            JsonObject(mapOf(
                "type" to JsonLiteral("git"),
                "repo" to json {
                    "remote" to repo.remote
                    "ref" to repo.ref
                },
                "clonedWithVersion" to JsonLiteral(versionInfo.version.toString())
            ))
        }

        val info = JsonObject(existingContent.content + mapOf(
            "lastUsed" to JsonLiteral(lastUsed.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        ))

        Files.write(infoPath, info.toString().toByteArray(Charsets.UTF_8))
    }

    fun listAll(): Set<CachedGitRepository> {
        if (!Files.isDirectory(gitCacheDirectory)) {
            return emptySet()
        }

        val cacheDirectoryContents = Files.list(gitCacheDirectory).toSet()
        val infoFiles = cacheDirectoryContents.filter { it.fileName.toString().endsWith(".json") }

        return infoFiles.mapToSet { loadInfoFile(it) }
    }

    private fun loadInfoFile(infoFilePath: Path): CachedGitRepository {
        try {
            val infoFileContent = Files.readAllBytes(infoFilePath).toString(Charsets.UTF_8)
            val infoFileJson = Json.default.parseJson(infoFileContent).jsonObject
            val repoJson = infoFileJson.getObject("repo")
            val repo = GitRepositoryReference(repoJson.getPrimitive("remote").content, repoJson.getPrimitive("ref").content)
            val lastUsed = ZonedDateTime.parse(infoFileJson.getPrimitive("lastUsed").content, DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            val expectedWorkingCopyPath = infoFilePath.parent.resolve(infoFilePath.fileName.toString().removeSuffix(".json"))
            val haveWorkingCopy = Files.exists(expectedWorkingCopyPath)

            return CachedGitRepository(
                repo,
                lastUsed,
                if (haveWorkingCopy) expectedWorkingCopyPath else { null },
                infoFilePath
            )
        } catch (e: JsonException) {
            throw GitRepositoryCacheException("The file $infoFilePath could not be loaded: ${e.message}", e)
        } catch (e: NoSuchElementException) {
            throw GitRepositoryCacheException("The file $infoFilePath could not be loaded: ${e.message}", e)
        }
    }

    fun delete(repo: CachedGitRepository) {
        if (repo.workingCopyPath != null) {
            deleteDirectory(repo.workingCopyPath)
        }

        if (Files.exists(repo.infoPath)) {
            Files.delete(repo.infoPath)
        }
    }

    private fun <T> Stream<T>.toSet(): Set<T> = collect(Collectors.toSet<T>())
}

data class CachedGitRepository(val repo: GitRepositoryReference, val lastUsed: ZonedDateTime, val workingCopyPath: Path?, val infoPath: Path)
class GitRepositoryCacheException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

typealias TimeSource = () -> ZonedDateTime
