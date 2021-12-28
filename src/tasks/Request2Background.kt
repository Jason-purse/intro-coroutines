package tasks

import contributors.GitHubService
import contributors.RequestData
import contributors.User
import kotlin.concurrent.thread
/**
 * @author FLJ
 * @dateTime 2021/12/27 13:50
 * @description 感觉是回调的方式...
 */
fun loadContributorsBackground(service: GitHubService, req: RequestData, updateResults: (List<User>) -> Unit) {
    // 开一个新的线程,加载...
    thread {
        // 同样是blocking
        loadContributorsBlocking(service, req)
    }
}