package contributors

import contributors.Contributors.LoadingStatus.*
import contributors.Variant.*
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import tasks.*
import java.awt.event.ActionListener
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext

enum class Variant {
    BLOCKING,         // Request1Blocking
    BACKGROUND,       // Request2Background
    CALLBACKS,        // Request3Callbacks
    SUSPEND,          // Request4Coroutine
    CONCURRENT,       // Request5Concurrent
    CONCURRENT_MANY,  // 多个线程中执行协程
    CONCURRENT_STRUCTURE, // 结构化协程
    CONCURRENT_GLOBAL, // 全局非结构化..
    NOT_CANCELLABLE,  // Request6NotCancellable
    PROGRESS,         // Request6Progress
    CHANNELS          // Request7Channels
}

interface Contributors: CoroutineScope {

    val job: Job

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    fun init() {
        // Start a new loading on 'load' click
        addLoadListener {
            // save params
            saveParams()
            // load contributors.
            loadContributors()
        }

        // Save preferences and exit on closing the window
        addOnWindowClosingListener {
            job.cancel()
            saveParams()
            System.exit(0)
        }

        // Load stored params (user & password values)
        loadInitialParams()
    }

    fun loadContributors() {
        val (username, password, org, _) = getParams()
        val req = RequestData(username, password, org)

        clearResults()
        val service = createGitHubService(req.username, req.password)

        val startTime = System.currentTimeMillis()
        // 判断选择的变种..
        when (getSelectedVariant()) {
            BLOCKING -> { // Blocking UI thread
                val users = loadContributorsBlocking(service, req)
                // blocking .. update
                updateResults(users, startTime)
            }
            BACKGROUND -> { // Blocking a background thread
                //
                loadContributorsBackground(service, req) { users ->
                    // 将更新放入UI线程..
                    SwingUtilities.invokeLater {
                        updateResults(users, startTime)
                    }
                }
            }
            CALLBACKS -> { // Using callbacks
                loadContributorsCallbacks(service, req) { users ->
                    SwingUtilities.invokeLater {
                        updateResults(users, startTime)
                    }
                }
            }
            // 这种方式仅仅只是使用了suspend 函数,但是没有带来任何的并发性
            SUSPEND -> { // Using coroutines
                // 这里的协程运行在UI线程之上..
                launch {
                    val users = loadContributorsSuspend(service, req)
                    updateResults(users, startTime)
                }.setUpCancellation()
            }
            // 这种效率和回调相差无几....
            CONCURRENT -> { // Performing requests concurrently
                launch {
                    val users = loadContributorsConcurrent(service, req)
                    updateResults(users, startTime)
                }.setUpCancellation()
            }
            CONCURRENT_MANY -> {
              launch {
                  val users = loadContributorsConcurrentByDispatcher(service,req)
                  updateResults(users,startTime)
              }.setUpCancellation()
            }
            CONCURRENT_STRUCTURE -> {
                launch {
                    val users = loadContributorsConcurrentByDispatcher1(service,req)
                    updateResults(users,startTime)
                }.setUpCancellation()
            }
            CONCURRENT_GLOBAL -> {
                launch {
                    val users = loadContributorsConcurrentByDispatcher2(service,req)
                    updateResults(users,startTime)
                }.setUpCancellation()
            }
            NOT_CANCELLABLE -> { // Performing requests in a non-cancellable way
                launch {
                    val users = loadContributorsNotCancellable(service, req)
                    updateResults(users, startTime)
                }.setUpCancellation()
            }
            PROGRESS -> { // Showing progress
                launch(Dispatchers.Default) {
                    loadContributorsProgress(service, req) { users, completed ->
                        // 仅仅在主线程更新中间状态和数据
                        withContext(Dispatchers.Main) {
                            updateResults(users, startTime, completed)
                        }
                    }
                }.setUpCancellation()
            }
            CHANNELS -> {  // Performing requests concurrently and showing progress
                launch(Dispatchers.Default) {
                    loadContributorsChannels(service, req) { users, completed ->
                        // 仅在主线程工作,协同工作,效率较高...
                        withContext(Dispatchers.Main) {
                            updateResults(users, startTime, completed)
                        }
                    }
                }.setUpCancellation()
            }
        }
    }

    private enum class LoadingStatus { COMPLETED, CANCELED, IN_PROGRESS }

    private fun clearResults() {
        updateContributors(listOf())
        updateLoadingStatus(IN_PROGRESS)
        setActionsStatus(newLoadingEnabled = false)
    }

    private fun updateResults(
        users: List<User>,
        startTime: Long,
        completed: Boolean = true
    ) {
        updateContributors(users)
        updateLoadingStatus(if (completed) COMPLETED else IN_PROGRESS, startTime)
        if (completed) {
            setActionsStatus(newLoadingEnabled = true)
        }
    }

    private fun updateLoadingStatus(
        status: LoadingStatus,
        startTime: Long? = null
    ) {
        val time = if (startTime != null) {
            val time = System.currentTimeMillis() - startTime
            "${(time / 1000)}.${time % 1000 / 100} sec"
        } else ""

        val text = "Loading status: " +
                when (status) {
                    COMPLETED -> "completed in $time"
                    IN_PROGRESS -> "in progress $time"
                    CANCELED -> "canceled"
                }
        setLoadingStatus(text, status == IN_PROGRESS)
    }
// 能够相应cancel
    private fun Job.setUpCancellation() {
        // make active the 'cancel' button
        setActionsStatus(newLoadingEnabled = false, cancellationEnabled = true)

        val loadingJob = this

        // cancel the loading job if the 'cancel' button was clicked
        val listener = ActionListener {
            loadingJob.cancel()
            updateLoadingStatus(CANCELED)
        }
        addCancelListener(listener)

        // update the status and remove the listener after the loading job is completed
        launch {
            loadingJob.join()
            // 设置活动状态...
            setActionsStatus(newLoadingEnabled = true)
            // 移除cancelListener. 因为不需要了..
            removeCancelListener(listener)
        }
    }

    fun loadInitialParams() {
        setParams(loadStoredParams())
    }

    fun saveParams() {
        val params = getParams()
        if (params.username.isEmpty() && params.password.isEmpty()) {
            removeStoredParams()
        }
        else {
            saveParams(params)
        }
    }

    fun getSelectedVariant(): Variant

    fun updateContributors(users: List<User>)

    fun setLoadingStatus(text: String, iconRunning: Boolean)

    fun setActionsStatus(newLoadingEnabled: Boolean, cancellationEnabled: Boolean = false)

    fun addCancelListener(listener: ActionListener)

    fun removeCancelListener(listener: ActionListener)

    fun addLoadListener(listener: () -> Unit)

    fun addOnWindowClosingListener(listener: () -> Unit)

    fun setParams(params: Params)

    fun getParams(): Params
}
