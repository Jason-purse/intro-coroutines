package tasks

import contributors.*
/**
 * @author FLJ
 * @dateTime 2021/12/28 15:13
 * @description 一个suspend 函数(不会阻塞底层的线程,这是它和runBlocking 最大的区别)
 * 这里的Retrofit 支持 协程..
 */
suspend fun loadContributorsSuspend(service: GitHubService, req: RequestData): List<User> {
    // 有多少个仓库...
    // 这样就返回了Rxjava  响应式流..
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .body() ?: listOf()

    // 将每一个仓库扁平化..
    // 然后在聚合
    // 在这里 我们可以知道协程到底是什么,官方说 是一个可以被延缓/ 暂停的计算..
    // 一个可暂停计算的协程可以运行在顶层的线程中,当协程停止时将它从线程中移除,并保留在内存中...
    // 并且当它可以继续时可以放入线程中,但不必是同一个线程执行..

    // 开启-Dkotlinx.coroutines.debug 查看协程调试工作...
    return repos.flatMap { repo ->
        service
            .getRepoContributors(req.org, repo.name)
            .also { logUsers(repo, it) }
            .bodyList()
    }.aggregate()
}