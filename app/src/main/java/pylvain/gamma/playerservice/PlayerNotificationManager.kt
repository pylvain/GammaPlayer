package pylvain.gamma.playerservice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media.session.MediaButtonReceiver
import pylvain.gamma.MainActivity
import pylvain.gamma.MainApplication.Companion.mainContext
import pylvain.gamma.R
import timber.log.Timber

class PlayerNotificationManager(val playerService: PlayerService) {

    private val TAG = "MediaNotificationManager"
    private val CHANNEL_ID = "pylvain.gamma"
    private val REQUEST_CODE = 501


    private val playAction: NotificationCompat.Action =
        NotificationCompat.Action.Builder(
            IconCompat.createWithResource(mainContext, R.drawable.ic_music_rest_quarter),
            "Rest",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                playerService,
                PlaybackStateCompat.ACTION_PLAY
            )
        ).build()

    val notificationManager =
        this.playerService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        notificationManager.cancelAll()
    }

    fun onDestroy() {}

    fun getNotification(
        metadata: MediaMetadataCompat,
        playbackState: PlaybackStateCompat,
        token: MediaSessionCompat.Token
    ): Notification {

    }

    private fun buildNotification(
        playbackState: PlaybackStateCompat,
        token: MediaSessionCompat.Token,
        description: MediaDescriptionCompat
    ): Notification.Builder {
        if(isAndroidOOrHigher()) createChannel()

        val builder = NotificationCompat.Builder(playerService, CHANNEL_ID)
        builder.setStyle( androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(token)
            .setShowActionsInCompactView()


        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel =
                NotificationChannel(CHANNEL_ID, "MediaSession", NotificationManager.IMPORTANCE_LOW)
            channel.apply {
                description = "MediaSession"
                lightColor = Color.Blue.toArgb()
            }
            notificationManager.createNotificationChannel(channel)
            Timber.i("new Channel created")
        } else {
            Timber.i("createChannel: Existing channel reused")
        }
    }

    private fun isAndroidOOrHigher() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    private fun createContentIntent(): PendingIntent { //TODO: Understand that
        val openUI = Intent(playerService, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            playerService, REQUEST_CODE, openUI, PendingIntent.FLAG_CANCEL_CURRENT
        )
    }


}