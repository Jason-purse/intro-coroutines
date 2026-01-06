package tasks

import contributors.User

/*
TODO: Write aggregation code.

你可以通过 IntelliJ IDEA 快捷方式 Ctrl+Shift+T / ⇧ ⌘ T 自动在源代码和测试类之间切换。
 In the initial list each user is present several times, once for each
 repository he or she contributed to.
 Merge duplications: each user should be present only once in the resulting list
 with the total value of contributions for all the repositories.
 Users should be sorted in a descending order by their contributions.

 The corresponding test can be found in test/tasks/AggregationKtTest.kt.
 You can use 'Navigate | Test' menu action (note the shortcut) to navigate to the test.
*/
// fun List<User>.aggregate(): List<User> =
//     this.groupBy { it.login }
//         //.values.map { it.reduce { acc, user -> User(acc.login,acc.contributions + user.contributions) } }
//         // 直接将整个entry 进行map
//         .map { (login, group) -> User(login, group.sumOf { it.contributions }) }
//         .sortedByDescending { it.contributions }


fun List<User>.aggregate(): List<User> =
    this.groupingBy { it.login }
        // 如果用aggregate  accumulator 类型还需要详细指定 ..
            // 就离谱
        // .aggregate { key, accumulator: User?, element, first ->
        //     User(key,( accumulator?.contributions ?: 0 ) + element.contributions)
        // }
        .reduce { key, accumulator, element ->
            User(key, accumulator.contributions + element.contributions)
        }.values
        .sortedByDescending { it.contributions }
