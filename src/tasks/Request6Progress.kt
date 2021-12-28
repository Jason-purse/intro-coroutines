package tasks

import contributors.*
import kotlinx.coroutines.launch

suspend fun loadContributorsProgress(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    // 作为回调处理更新...
    val repos = service.getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()

    var allUsers = emptyList<User>()
    // 这是安全的...
    // 不存在任何并发... 但是如何 使用并发更新呢....
    // 管道解决这一切..
    for ((index, repo) in repos.withIndex()) {
        val users = service.getRepoContributors(req.org, repo.name)
            .also { logUsers(repo, it) }
            .bodyList()

        allUsers = (allUsers + users).aggregate()
        // 只有它做完了才进行下一次处理...
        updateResults(allUsers, index == repos.lastIndex)
    }
}
