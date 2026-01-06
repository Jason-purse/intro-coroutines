package contributors

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.SocketAddress
import java.util.Base64

/**
 * 你将使用 Retrofit 库对 GitHub 执行 HTTP 请求。它允许请求该组织下的仓库列表以及每个仓库的贡献者列表：
 */
interface GitHubService {

    // 这两个阻塞式API
    // 该 API 被loadContributorsBlocking()函数用来获取给定组织的贡献者列表。
    @GET("orgs/{org}/repos?per_page=100")
    fun getOrgReposCall(
        @Path("org") org: String
    ): Call<List<Repo>>

    @GET("repos/{owner}/{repo}/contributors?per_page=100")
    fun getRepoContributorsCall(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Call<List<User>>


    // 现在使用RxJava的方式,取消Call 返回结果..
    // 自己检测结果和异常
    @GET("orgs/{org}/repos?per_page=100")
    suspend fun getOrgRepos(
        @Path("org") org: String
    ): Response<List<Repo>>
    // 自己检测结果和异常..
    @GET("repos/{owner}/{repo}/contributors?per_page=100")
    suspend fun getRepoContributors(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<List<User>>
}

@Serializable
data class Repo(
    val id: Long,
    val name: String
)

@Serializable
data class User(
    val login: String,
    val contributions: Int
)

@Serializable
data class RequestData(
    val username: String,
    val password: String,
    val org: String
)

private val json = Json { ignoreUnknownKeys = true }

fun createGitHubService(username: String, password: String): GitHubService {
    // 编码base64
    val authToken = "Basic " + Base64.getEncoder().encode("$username:$password".toByteArray()).toString(Charsets.UTF_8)
    // okHttpClient
    // 开启代理...
    val httpClient = OkHttpClient.Builder()
        .proxy(
            Proxy(Proxy.Type.HTTP,InetSocketAddress("127.0.0.1",7890))
        )
        .addInterceptor { chain ->
            val original = chain.request()
            // 接受形式
            val builder = original.newBuilder()
                .header("Accept", "application/vnd.github.v3+json")
                    // 认证token
                .header("Authorization", authToken)
            val request = builder.build()
            chain.proceed(request)
        }
        .build()

    val contentType = "application/json".toMediaType()
    // retrofit 处理...
    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com")
        // 转换器工厂..
        // .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory(contentType))
        // .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(json.asConverterFactory(contentType))
        .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
        .client(httpClient)
        .build()
    return retrofit.create(GitHubService::class.java)
}
