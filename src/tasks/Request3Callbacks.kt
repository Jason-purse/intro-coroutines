package tasks

import contributors.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

fun loadContributorsCallbacks(service: GitHubService, req: RequestData, updateResults: (List<User>) -> Unit) {
    service.getOrgReposCall(req.org).onResponse { responseRepos ->
        logRepos(req, responseRepos)
        val repos = responseRepos.bodyList()
        // 由于处理回调的线程可能不是同一个...
        // 更好的方式是使用同步版本(这里所说的回调是下面的回调);
        // 于是.
//        val allUsers = mutableListOf<User>()
        // 这里使用同步集合,保证所有的回调对集合的操作都是原子性的..
        val allUsers = Collections.synchronizedList(mutableListOf<User>())

        val countDownLatch = CountDownLatch(repos.size)

        // 但是这不是一个最优解.. 使用CountDownLatch,结合synchronizedList 处理.. 然后得到最终结果并 更新.
        // 所以使用原子数字
//        #2 第二种
//        val number = AtomicInteger(0)
        repos.forEachIndexed { index, repo ->
            service.getRepoContributorsCall(req.org, repo.name).onResponse { responseUsers ->
                logUsers(repo, responseUsers)
                val users = responseUsers.bodyList()
                allUsers += users
                // 这是第一种..
                // 将它放置在最后一个任务中 刷新即可...
                // 存在一个问题,当 最后一个任务完成的比其他任务快,将会导致任务丢失..
//                if(index == repos.size - 1) {
//                    updateResults(allUsers)
//                }
                // 这是第二种 ..
                // 增加
                // 原子操作
//                if (number.incrementAndGet() == repos.size) {
//                    updateResults(allUsers.aggregate())
//                }

                // 第三种
                countDownLatch.countDown()
            }
        }
        // 回调的位置有问题..
        // TODO: Why this code doesn't work? How to fix that?
    /*    log.info("callback result user infos: $allUsers")
        updateResults(allUsers.aggregate())*/
        countDownLatch.await()
        updateResults(allUsers.aggregate())

        // 根据这个例子我们发现了  书写一个正确的回调是不简单且容易发生错误的...
        // 应该使用资源共享/同步的机制去处理它..

        // 例如使用RXJava 来正确书写代码...

    }
}
// kotlin 不允许 inline 函数在非本地上下文中使用本地return ,所以需要crossinline关键字..
inline fun <T> Call<T>.onResponse(crossinline callback: (Response<T>) -> Unit) {
    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            callback(response)
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            log.error("Call failed", t)
        }
    })
}
