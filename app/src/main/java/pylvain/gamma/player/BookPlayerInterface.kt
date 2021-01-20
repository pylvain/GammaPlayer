package pylvain.gamma.player

import android.support.v4.media.session.PlaybackStateCompat
import pylvain.gamma.library.BookmarkEntry

interface BookPlayerInterface {

    fun load(source: String)
    fun play()
    fun pause()
    fun seek(timeMs: Long)
    fun seekTo(timeMs: Long)

    fun jumpTo(file:String,timeMs: Long)

    fun getState(): PlaybackStateCompat

}

class AgnosticBookPlayer(val bp:BookPlayerInterface) {

    fun jumpTo(bm:BookmarkEntry){}

    fun fastForward(){}
    fun previous(){}

    fun pause(){}

}