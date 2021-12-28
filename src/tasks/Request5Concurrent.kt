package tasks

import contributors.*
import kotlinx.coroutines.*

// async 是一个普通的suspend 函数,需要运行在协程scope中,所以为了能够运用async,那么将当前suspend函数  作为一个协程scope..
suspend fun loadContributorsConcurrent(service: GitHubService, req: RequestData): List<User>  = coroutineScope  {
    // 这种方式使用了 反应式编程,能够解决并发处理..
    // 虽然同样是一个主线程中运行这些协程,但是已经得到了并发处理的好处..

    // 如果实在是需要将协程在不同线程上执行,那么可以使用默认的线程池.. 例如
    // async(Dispatchers.Default) {}
    // CoroutineDispatcher 决定了线程和线程相关的协程应该如何执行..
    // 如果没有指定派发器,那么将默认使用外部协程作用域的派发器..
    // Dispatchers.Default  是jvm上共享的一个线程池.  是可用cpu核心数的数量,如果仅有一核心也有两个线程..
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()
    // 这里的lambada 将this 指向了 此函数的接收者..,就是协程scope
    val deferred: List<Deferred<List<User>>> = repos.map { repo ->
           // 可以得到一个Deferred<List<User>>
            async {
                service.getRepoContributors(req.org, repo.name)
                    .also { logUsers(repo, it) }
                    .bodyList()
            }
    }
    // 当List<Deferred> awaitAll() 之后 就相当于拆包  => List<List<User>>  然后调用flattern() 解包 .. 最终聚合..
    deferred.awaitAll().flatten().aggregate()
}


suspend fun loadContributorsConcurrentByDispatcher(service: GitHubService, req: RequestData): List<User>  = coroutineScope  {
// 通过不同的线程执行...
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()
    // 这里的lambada 将this 指向了 此函数的接收者..,就是协程scope
    val deferred: List<Deferred<List<User>>> = repos.map { repo ->
        // 可以得到一个Deferred<List<User>>
        // 为了仅仅在主线程上运行协程
        // async(Dispatchers.MAIN)
        // 当主线程比较忙的时候,我们应该开启一个协程,协程可以被暂停、可以被调度执行、仅当线程可用、空闲时..
        async(Dispatchers.Default) {
            service.getRepoContributors(req.org, repo.name)
                .also { logUsers(repo, it) }
                .bodyList()
        }


        // 由于协程调度器可以使用外部作用域的,那么我们可以让代码变得灵活,例如在主线程中就左主线程的事情,在Default中做不同的事情,或者它可以来自不同调度器的上下文...
        // 例如下面的这一种,仅当在主线程中才更新...
        // 要让它生效,使用默认的线程池  CoroutineDispatcher.Default..(这里面包含了所有必要的线程池)
//        launch(Dispatchers.Default) {
//            val users = loadContributorsConcurrent(service, req)
//            withContext(Dispatchers.Main) {
//                updateResults(users, startTime)
//            }
//        }
        // 显式暂停一个协程. join 即可..


        // 这里留下了一些疑问,协程上下文和协程scope的不同
        // 如何使用来自外部scope的调度器正确工作, -> 正确使用外部scope的上下文的调度器工作
        // 为什么需要在协程scope中开启一个异步的贡献者协程..
        // 这一切都是为了结构化并发..

        // 结构化并发:
        // 1. 协程scope  需要维持结构以及不同协程之间的父子关系..
        // 协程上下文存储额外的技术信息,例如协程自定义名称,或者指定一个调度此协程的线程,-仅当运行一个携程时才会给定一个协程上下文,
        // launch / async / runBlocking 都被用来开启一个协程. 并自动的创建相应的协程作用域
        // 这种函数都将具有接收者的lambda作为一个参数进行调用,而接收者就是协程Scope.
        // 新的协程只能够在协程Scope中产生..
        // 并且launch /async 都是CoroutineScope的扩展..,所以当每次调用这样的方法时,不管是隐式或者显式都是自动将接收者作为参数传递..
        // runBlocking 是一个意外,因为它仅仅是为了在main函数中使用或者在测试中作为一个从普通环境切换到协程环境的一个桥接..

        // 例如 这样的代码其实从launch开始就是隐式coroutineScope的隐式调用,所以launch开启的一个协程是外面协程的子协程.
        // 同样可以不创建协程就创建一个新的协程Scope,例如coroutineScope函数就是这样做的..
        // 例如通过一种结构化的方式,在suspend函数中不需要访问外部scope,只需创建一个scope(将会自动作为谁(外部的scope)调用此suspend的子coroutineScope
//        runBlocking {
//            launch {  }
//        }

        // 同样,我们可以从全局Scope中创建一个新的协程- 通过GlobalScope.async 或者GlobalScope.launch.
        // 这样将创建一个顶级scope中独立的协程.. 或者 GlobalScope.launch..
        // 相比全局scope来说,这种结构称为结构化并发...
        // 结构化并发对比全局scope的协程好处..
        // 1.一个scope 对它的子协程负责,每一个协程的生命周期依附于所在的外层scope.
        // 2. scope能够取消子协程,不管是发生了错误还是用户的想法发生了改变...
        // 3. scope 将自动等待每一个子协程的处理完毕. 如果一个协程关联了scope. 那么父亲协程(拥有这个scope)将等待所有的在此scope中的子协程执行完毕,否则不会关闭..释放..
        // 4. 当使用Global.async 将所有的协程放置到一个小的scope中,从全局scope中启动的协程将相互独立..生命周期取决于整个应用的声明周期...
        // 它也可以引用一个从Global Scope启动的协程-并等待它完成或者显式的取消.
        // 但是但它不会像结构化的那样自动发生,也就是说他没有那个义务帮你处理(帮你等待,完成释放)

        // 通过Global scope 和 coroutineScope 来观察两种行为..



    }
    // 当List<Deferred> awaitAll() 之后 就相当于拆包  => List<List<User>>  然后调用flattern() 解包 .. 最终聚合..
    deferred.awaitAll().flatten().aggregate()
}

/**
 * 使用structure scope
 */
suspend fun loadContributorsConcurrentByDispatcher1(service: GitHubService, req: RequestData): List<User> = coroutineScope {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()
    // 这里的lambada 将this 指向了 此函数的接收者..,就是协程scope
    val deferred: List<Deferred<List<User>>> = repos.map { repo ->
        async {
            // 延时三秒钟
            delay(3000)
            service.getRepoContributors(req.org,repo.name)
            .also { logUsers(repo, it) }
            .bodyList()
        }
    }
    deferred.awaitAll().flatten().aggregate()
}

/**
 * 使用global scope
 *
 * 当cancel 的时候,具有结构化并发的结构能够快速的取消并传播信号,而全局的协程相互独立传播取消信息..
 */
@OptIn(DelicateCoroutinesApi::class)
suspend fun loadContributorsConcurrentByDispatcher2(service: GitHubService, req: RequestData): List<User>  {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()

    val deferred: List<Deferred<List<User>>> = repos.map { repo ->
        GlobalScope.async {
            // 延时三秒钟
            delay(3000)
            service.getRepoContributors(req.org,repo.name)
                .also { logUsers(repo, it) }
                .bodyList()
        }
    }
    // 为什么这里需要返回,因为不在lambda中..
    return deferred.awaitAll().flatten().aggregate()
}

// ------------------------ 由于scope 是继承,那么 上下文必然也是继承 那么使用scope的派发器等同于使用上下文的派发器...
// 正确使用外部scope的上下文,将子协程都利用外部scope的上下文是一件很容易的事情..,同时如果需要也可以直接替换上下文..
// 现在我们来回答如何使用外部作用域的派发器或者更加规范的说法是如何使用外部作用域的上下文的派发器..
// 注意的是 coroutineScope 或者协程构建器   创建一个继承于外部scope的新的子scope,这种情况下,
//launch(Dispatchers.Default) {  // outer scope
//    val users = loadContributorsConcurrent(service, req)
//    // ...
//}
// 例如loadContributorsConcurrent 创建了一个子scope,那么launch 就是外部scope,而函数内部就是子scope..
// 所有内嵌的协程都是自动从继承的上下文开始,并且派发器是上下文的一部分..
// 那就是为什么async 协程构建器默认使用default dispatcher的上下文..
// 同样结构化并发能够指定一个主上下文元素(例如dispatcher) - 当我们创建一个顶级协程的时候
//所有内嵌的协程都会继承此上下文并在必要时修改它.
// 注意在写例如UI应用的代码时,Dispatcher.Default默认是顶级协程,如果需要放在不同线程中调用协程可以使用不同的dispatcher...