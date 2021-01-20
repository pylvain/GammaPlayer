package pylvain.gamma.playerservice

import android.content.Intent
import android.content.Intent.EXTRA_KEY_EVENT
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.koin.android.ext.android.inject
import pylvain.gamma.player.BookPlayer
import timber.log.Timber


class PlayerService : MediaBrowserServiceCompat() {

    private val scope = CoroutineScope(Dispatchers.IO)

    private lateinit var playerNotificationManager: PlayerNotificationManager
    lateinit var session: MediaSessionCompat

    val bookPlayer: BookPlayer by inject()

    private fun updateState() { //Rename that
        session.setPlaybackState(bookPlayer.playbackState)
        session.setMetadata(bookPlayer.getMetadata()) // TODO memo that on the bookplayer
    }

    override fun onCreate() {
        super.onCreate()

        session = MediaSessionCompat(this, "MusicService")
        session.apply {
            isActive = true
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(callbacks)
        }

        updateState()
        sessionToken = session.sessionToken

        playerNotificationManager = PlayerNotificationManager(this, bookPlayer)
        playerNotificationManager.startNotification()

        scope.launch {
            bookPlayer.messageFlow.collect() { msg ->
                if (msg.reasonEmitted == BookPlayer.MessageEmitReason.STATE_CHANGED) {
                    Timber.i("STATE CHANGED")
                    updateState()
                    playerNotificationManager.updateNotification()
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) { //TODO
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot("root", null)
    }

    override fun onLoadChildren(
        parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(null);
    }

    override fun onDestroy() {
        //session.release()
    }

    // MediaSession Callback: Transport Controls -> MediaPlayerAdapter
    val callbacks = object : MediaSessionCompat.Callback() {

        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            updateState()
            playerNotificationManager.updateNotification()
            return false
        }

        override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
            Timber.i("Test")
            super.onCommand(command, extras, cb)
        }

        override fun onPrepare() {
            Timber.i("Preparing")
        }

        override fun onPlay() {
            Timber.i("Playing")
            bookPlayer.play()
        }

        override fun onPause() {
            Timber.i("Pausing")
            bookPlayer.pause()
        }

        override fun onFastForward() {
            bookPlayer.seek(30_000L) //TODO
        }

        override fun onRewind() {
           bookPlayer.seek(-30_000L)
        }

        override fun onSkipToPrevious() {
            bookPlayer.previous()
        }

        override fun onSkipToNext() {
            bookPlayer.next()
        }

        override fun onSeekTo(pos: Long) {
            bookPlayer.jumpTo(pos)
        }

        override fun onStop() {
            bookPlayer.stop()
            super.onStop()
        }

        //set playback speedhttps://github.com/PaulWoitaschek/Voice/blob/113d54d0f072dd834e383bc625a48f98b182ad6b/playback/src/main/java/de/ph1b/audiobook/playback/session/MediaSessionCallback.kt

        override fun onCustomAction(action: String?, extras: Bundle?) {
            Timber.i("CUSTOM")
        }

    }

}

