package pylvain.gamma

import android.net.Uri
import android.text.format.DateUtils.formatElapsedTime
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.lang.System.currentTimeMillis


fun SimpleExoPlayer.addMediaFromString(s: String) = addMediaItem(MediaItem.fromUri(Uri.parse(s)))


fun epoch() = currentTimeMillis()

fun File.contains(f: File): Boolean {
    fun contains(s: String, d: File): Boolean = when (d.parentFile?.canonicalPath) {
        null -> false
        s -> true
        else -> contains(s, d.parentFile!!)
    }
    return this.canonicalPath == f.canonicalPath || contains(this.canonicalPath, f.canonicalFile)
}

fun <T> LiveData<T>.observeOnce(observer: (T) -> Unit) {
    observeForever(object : Observer<T> {
        override fun onChanged(value: T) {
            removeObserver(this)
            observer(value)
        }
    })
}

suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}

fun logd(msg: Any) = Timber.i("DEBUG", msg.toString())


fun formatMs(timeMs: Long): String? {
    val seconds = timeMs / 1000
    val ms = timeMs % 1000
    val s = seconds % 60
    val m = seconds / 60 % 60
    val h = seconds / (60 * 60) % 24
    return String.format("%d:%02d:%02d:%03d", h, m, s,ms)
}


