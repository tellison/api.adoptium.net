package net.adoptium.api.v3.dataSources.github.graphql.clients

/* ktlint-disable no-wildcard-imports */
/* ktlint-enable no-wildcard-imports */
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import net.adoptium.api.v3.dataSources.UpdaterHtmlClient
import net.adoptium.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptium.api.v3.dataSources.github.graphql.models.GHReleases
import net.adoptium.api.v3.dataSources.github.graphql.models.GHRepository
import net.adoptium.api.v3.dataSources.github.graphql.models.PageInfo
import net.adoptium.api.v3.dataSources.github.graphql.models.QueryData
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

@Singleton
class GraphQLGitHubRepositoryClient @Inject constructor(
    graphQLRequest: GraphQLRequest,
    updaterHtmlClient: UpdaterHtmlClient
) : GraphQLGitHubReleaseRequest(graphQLRequest, updaterHtmlClient) {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    suspend fun getRepository(owner: String, repoName: String): GHRepository {
        val query = GetQueryData(owner, repoName)

        LOGGER.info("Getting repo $repoName")

        val releases = getAll(
            query::withCursor,
            { request -> getAllAssets(request) },
            { it.repository!!.releases.pageInfo.hasNextPage },
            { it.repository!!.releases.pageInfo.endCursor }
        )

        LOGGER.info("Done getting $repoName")

        return GHRepository(GHReleases(releases, PageInfo(false, null)))
    }

    private suspend fun getAllAssets(request: QueryData): List<GHRelease> {
        if (request.repository == null) return listOf()

        // nested releases based on how we deserialise githubs data
        return request.repository.releases.releases
            .map { release ->
                if (release.releaseAssets.pageInfo.hasNextPage) {
                    getAllReleaseAssets(release)
                } else {
                    release
                }
            }
    }

    class GetQueryData(private val owner: String, private val repoName: String, override val variables: Any = mapOf<String, String>()) :
        GraphQLClientRequest<QueryData> {

        fun withCursor(cursor: String?): GetQueryData {
            return if (cursor != null) GetQueryData(owner, repoName, mapOf("cursorPointer" to cursor))
            else this
        }

        override val query: String
            get() = """
                        query(${'$'}cursorPointer:String) {
                            repository(owner:"$owner", name:"$repoName") {
                                releases(first:50, after:${'$'}cursorPointer, orderBy: {field: CREATED_AT, direction: DESC}) {
                                    nodes {
                                        id,
                                        url,
                                        name, 
                                        publishedAt,
                                        updatedAt,
                                        isPrerelease,
                                        resourcePath,
                                        releaseAssets(first:50) {
                                            totalCount,
                                            nodes {
                                                downloadCount,
                                                updatedAt,
                                                name,
                                                downloadUrl,
                                                size
                                            },
                                            pageInfo {
                                                hasNextPage,
                                                endCursor
                                            }
                                        }
                                    },
                                    pageInfo {
                                        hasNextPage,
                                        endCursor
                                    }
                                }
                            }
                            rateLimit {
                                cost,
                                remaining
                            }
                        }
                    """
                .trimIndent()
                .replace("\n", "")

        override fun responseType(): KClass<QueryData> {
            return QueryData::class
        }
    }
}
