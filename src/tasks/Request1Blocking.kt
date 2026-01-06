package tasks

import contributors.*
import retrofit2.Response

/**
 * 再次运行程序，看看IntelliJ IDEA中的系统输出。应该有类似这样的内容：
 * ```txt
 * 1770 [AWT-EventQueue-0] INFO  Contributors - kotlin: loaded 40 repos
 * 2025 [AWT-EventQueue-0] INFO  Contributors - kotlin-examples: loaded 23 contributors
 * 2229 [AWT-EventQueue-0] INFO  Contributors - kotlin-koans: loaded 45 contributors
 * ...
 * ```
 * 每行的第一个项是程序启动以来经过的毫秒数，然后是方括号内的线程名称。
 * 你可以看到加载请求是从哪个线程被调用的。
 *
 * 每行的最后一项是实际信息：加载了多少仓库或贡献者。
 *
 * 该日志输出表明所有结果均来自主线程。当你用BLOCKING选项运行代码时，窗口会冻结，直到加载完成才会响应输入。所有请求都从被调用loadContributorsBlocking()的线程执行，而该线程是主UI线程（在Swing中，是AWT事件调度线程）。
 * 这个主线程会被阻塞，这就是界面卡住的原因：
 */
fun loadContributorsBlocking(service: GitHubService, req: RequestData) : List<User> {
    // 有多少个仓库...
    val repos = service
        .getOrgReposCall(req.org)
        .execute() // Executes request and blocks the current thread
        // 副作用方法,记录加载了那些仓库,以及错误
        .also { logRepos(req, it) }
        // 这里用空列表的原因是有可能发生了错误,并且记录了对应的错误
        .body() ?: emptyList()

    // 将每一个仓库扁平化..
    // 然后在聚合
    return repos.flatMap { repo ->
        service
            .getRepoContributorsCall(req.org, repo.name)
            // 发起请求并阻塞
            .execute() // Executes request and blocks the current thread
            .also { logUsers(repo, it) }
            // 同上
            .bodyList()
    }.aggregate()
        .groupBy { it.login }
        .map { (name,list) -> User(name,list.sumOf { it.contributions }) }
        .toList()
}

fun <T> Response<List<T>>.bodyList(): List<T> {
    return body() ?: emptyList()
}