package tasks

import contributors.GitHubService
import contributors.RequestData
import contributors.User
import javax.swing.SwingUtilities
import kotlin.concurrent.thread
/**
 *  首先，整个计算被移动到另一个线程。函数thread()启动一个新线程：
 *
 * @author FLJ
 * @dateTime 2021/12/27 13:50
 * @description 感觉是回调的方式...
 *
 * loadContributorsBackground()函数的签名会发生变化。在所有加载完成后，调用该函数的最后一个参数需要updateResults()回调：
 *
 * 现在，当调用loadContributorsBackground()时，调用updateResults()会进入回调，而不是像之前那样紧接着：
 */
fun loadContributorsBackground(service: GitHubService, req: RequestData, updateResults: (List<User>) -> Unit) {
    // 开一个新的线程,加载...
    thread {
        // 同样是blocking
        loadContributorsBlocking(service, req)
            .aggregate()
            .also {
                // 通过调用 SwingUtilities.invokeLater ，
                // 你确保updateResults()调用（更新结果）
                // 发生在主界面线程（AWT 事件调度线程）。

                // 但是，如果你尝试通过BACKGROUND选项加载贡献者，
                // 你会看到列表已经更新，但没有任何变化。


                // 当我们修复了之后,就能看到界面变化了
                // 但是不需要这个,因为 updateResults 有这个东西 ..
                // SwingUtilities.invokeLater {
                    updateResults(it)
                //}
            }
    }
}