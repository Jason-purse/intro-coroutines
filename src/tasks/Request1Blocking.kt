package tasks

import contributors.*
import retrofit2.Response

fun loadContributorsBlocking(service: GitHubService, req: RequestData) : List<User> {
    // 有多少个仓库...
    val repos = service
        .getOrgReposCall(req.org)
        .execute() // Executes request and blocks the current thread
        .also { logRepos(req, it) }
        .body() ?: emptyList()

    // 将每一个仓库扁平化..
    // 然后在聚合
    return repos.flatMap { repo ->
        service
            .getRepoContributorsCall(req.org, repo.name)
            .execute() // Executes request and blocks the current thread
            .also { logUsers(repo, it) }
            .bodyList()
    }.aggregate()
        .groupBy { it.login }
        .map { (name,list) -> User(name,list.sumOf { it.contributions }) }
        .toList()
}

fun <T> Response<List<T>>.bodyList(): List<T> {
    return body() ?: emptyList()
}