package contributors

import kotlinx.coroutines.launch

/**
 * 1.  启动程序
 * 2. 在相应字段中填写您的GitHub用户名和令牌（或密码）。
 * 3. 确保在变体下拉菜单中选择了BLOCKING选项。
 * 4. 点击“加载贡献者”。界面应该会冻结一段时间，然后显示贡献者列表。
 * 5. 打开程序输出确认数据已加载。每次成功请求后都会记录贡献者名单。
 * 6. 实现这种逻辑有多种方式：使用阻断请求或回调。你会将这些方案与使用协程的方案进行比较，看看通道如何用于不同协程之间的信息共享。
 */
fun main() {

    // 如果字体太小,可以设置大一点
    setDefaultFontSize(18f)
    ContributorsUI().apply {
        pack()
        setLocationRelativeTo(null)
        isVisible = true

    }

}