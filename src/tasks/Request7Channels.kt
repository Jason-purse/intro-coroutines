package tasks

import contributors.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun loadContributorsChannels(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    // 这里继承外部协程作用域...,谁调用就是继承谁的协程scope...
    coroutineScope {

    }
}

// 管道
// 对一个共享可变状态进行写是一个十分困难且容易出错的事情(甚至在这个教程中使用回调也会遭遇这个事情)
// 通过交流而共享的信息而不是通过公共的可变状态来共享信息 更加简单...
// 协程能够通过管道相互叫交流..

// 管道是协程允许我们在不同协程中传递数据的基本方式.. 一个协程能够发送某些信息到管道中,然而另一个能从管道中接受消息..
// 生产者与消费者模型  管道能够被多个生产者写 或者多个消费者读取.. 如果需要许多协程能够能够发送消息到相同的管道中...进行同步..
// 注意的是许多协程从一个管道中接受信息时,每一个消息仅仅能够被消费者之一处理,处理它之后消息会自动从管道中移除..

// 我们能够认为管道类似于集合中的元素(直接一点  也可以叫做队列):
// 元素从一边加入从另一边输出.. 然而有巨大的不同,不像collection,甚至在他们的同步版本中,一个管道能够暂停send 和 receive 操作..
// 当管道为空 /或者满的时候会发生(管道的尺寸可能被限制,然后它就好了)
// Channel 能够使用三种不同的接口呈现: SendChannel / ReceiveChannel / Channel 首先继承了前两种,你能够使用它创建一个叫做生产者的管道..
// 因为它仅仅能够发送,然后创建一个ReceiveChannel 的消费者,, 仅仅能够接受信息
// send / receive 的方法都被声明suspend....

///**
// * interface SendChannel<in E> {
//suspend fun send(element: E)
//fun close(): Boolean
//}
//
//interface ReceiveChannel<out E> {
//suspend fun receive(): E
//}
//
//interface Channel<E> : SendChannel<E>, ReceiveChannel<E>
// */

// Unlimited channel
// 不限制管道 不会被suspend  /  当没有内存时,oom

// 可缓存的channel
// 约定管道就是科缓存管道的0尺寸管道
//只能存储一个元素,仅当其中一个方法调用之后,另一个方法才可以调用
// 在统一的时间和地点会面..   -这意味着send / receive 应该同时会面...

// 合并管道(conflated)
// 能够覆盖元素 / send 元素永远不会阻塞..
// 当你创建一个管道 / 你能够指定它的类型或者缓存尺寸(如果你需要缓存)
// val rendezvousChannel = Channel<String>()
//val bufferedChannel = Channel<String>(10)
//val conflatedChannel = Channel<String>(CONFLATED)
//val unlimitedChannel = Channel<String>(UNLIMITED)

//val channel = Channel<String>()  默认创建Rendezvous(约定) 管道///