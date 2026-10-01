@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.legado.app.help.media

import io.legado.app.constant.AppLog
import io.legado.app.platform.kvo.LegadoKeyValueObservingProtocol
import kotlinx.cinterop.COpaquePointer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.Foundation.NSKeyValueObservingOptionInitial
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.addObserver
import platform.Foundation.removeObserver
import platform.darwin.NSObject

/** AVPlayerItem.status 的可释放 KVO 观察器。 */
class AvPlayerItemStatusObserver(
    private val item: AVPlayerItem,
    private val onReady: () -> Unit,
    private val onFailed: (String) -> Unit,
) : NSObject(), LegadoKeyValueObservingProtocol {

    private var observing = false

    fun start() {
        if (observing) return
        observing = true
        AppLog.put("iOS 播放器 KVO：开始监听，status=${item.status}")
        item.addObserver(
            observer = this,
            forKeyPath = STATUS_KEY,
            options = NSKeyValueObservingOptionInitial or NSKeyValueObservingOptionNew,
            context = null,
        )
        AppLog.put("iOS 播放器 KVO：注册返回，observing=$observing，status=${item.status}")
    }

    fun dispose() {
        if (!observing) return
        observing = false
        AppLog.put("iOS 播放器 KVO：移除监听，status=${item.status}")
        item.removeObserver(this, forKeyPath = STATUS_KEY)
    }

    override fun observeValueForKeyPath(
        keyPath: String?,
        ofObject: Any?,
        change: Map<Any?, *>?,
        context: COpaquePointer?,
    ) {
        val sameItem = ofObject === item
        AppLog.put("iOS 播放器 KVO：回调，keyPath=$keyPath，observing=$observing，sameItem=$sameItem，status=${item.status}，主线程=${platform.Foundation.NSThread.isMainThread}")
        if (!observing || keyPath != STATUS_KEY || !sameItem) {
            AppLog.put("iOS 播放器 KVO：回调被过滤")
            return
        }
        when (item.status) {
            AVPlayerItemStatusReadyToPlay -> {
                dispose()
                onReady()
            }

            AVPlayerItemStatusFailed -> {
                val message = item.error?.localizedDescription ?: "AVPlayerItem 加载失败"
                dispose()
                onFailed(message)
            }
        }
    }

}

// K/N 限制: ObjC 子类 (NSObject) 的 companion 不允许字段, const 常量放文件顶层
private const val STATUS_KEY = "status"
