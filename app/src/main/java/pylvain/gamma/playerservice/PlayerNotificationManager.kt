package pylvain.gamma.playerservice


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.session.PlaybackState.ACTION_STOP
import android.media.session.PlaybackState.STATE_PLAYING
import android.os.Build
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver.buildMediaButtonPendingIntent
import androidx.media.app.NotificationCompat.MediaStyle
import pylvain.gamma.MainActivity
import pylvain.gamma.R
import pylvain.gamma.player.BookPlayer
import timber.log.Timber


class PlayerNotificationManager(val playerService: PlayerService, val bp: BookPlayer) {

    val CHANNEL_ID = "pylvain.gamma"
    val NOTIFICATION_ID = 412
    val REQUEST_CODE = 501

    val notificationManager =
        playerService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun getPlayPauseAction() = NotificationCompat.Action( // Move that out ?
        if (bp.state == STATE_PLAYING) R.drawable.exo_icon_pause else R.drawable.exo_icon_play,
        playerService.getString(
            if (bp.state == STATE_PLAYING) R.string.exo_controls_pause_description
            else R.string.exo_controls_play_description
        ),
        buildMediaButtonPendingIntent(playerService, PlaybackStateCompat.ACTION_PLAY_PAUSE)
    )

    private val skipToPreviousAction = NotificationCompat.Action(
        R.drawable.exo_icon_previous,
        playerService.getString(R.string.exo_controls_previous_description),
        buildMediaButtonPendingIntent(playerService,PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
    )

    private val skipToNextAction = NotificationCompat.Action(
        R.drawable.exo_icon_next,
        playerService.getString(R.string.exo_controls_next_description),
        buildMediaButtonPendingIntent(playerService,PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
    )

    private val rewindAction = NotificationCompat.Action(
        R.drawable.exo_icon_rewind,
        playerService.getString(R.string.exo_controls_rewind_description),
        buildMediaButtonPendingIntent(playerService, PlaybackStateCompat.ACTION_REWIND)
    )

    private val fastForwardAction = NotificationCompat.Action(
        R.drawable.exo_icon_fastforward,
        playerService.getString(R.string.exo_controls_fastforward_description),
        buildMediaButtonPendingIntent(playerService, PlaybackStateCompat.ACTION_FAST_FORWARD)
    )

    private val style = MediaStyle()
        .setMediaSession(playerService.sessionToken)
        .setShowCancelButton(true)
        .setCancelButtonIntent(
            buildMediaButtonPendingIntent(
                playerService,
                PlaybackStateCompat.ACTION_STOP
            )
        )
        .setShowActionsInCompactView(1, 2) //TODO

    private fun getNotificationBuilder() = NotificationCompat.Builder(playerService, CHANNEL_ID)
        .setStyle(style)
        .setContentIntent(createContentIntent())
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setSmallIcon(R.drawable.ic_music_rest_quarter)
        .addAction(skipToPreviousAction)
        .addAction(rewindAction)
        .addAction(getPlayPauseAction())
        .addAction(fastForwardAction)
        .addAction(skipToNextAction)

    init {
        if (isAndroidOOrHigher()) createChannel()
    }

    fun makeNotification() = getNotificationBuilder().build()

    fun updateNotification() =
        notificationManager.notify(NOTIFICATION_ID, makeNotification())

    fun startNotification() =
        playerService.startForeground(NOTIFICATION_ID, makeNotification())

    private fun createContentIntent(): PendingIntent { //TODO: Understand that
        val openUI = Intent(playerService, MainActivity::class.java)
        openUI.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(
            playerService, 0, openUI, PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

//----------------------------------------------------------------------------------------------


    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannel(): String {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel =
                NotificationChannel(CHANNEL_ID, "MediaSession", NotificationManager.IMPORTANCE_LOW)
            channel.apply {
                description = "MediaSession"
            }
            notificationManager.createNotificationChannel(channel)
            Timber.i("new Channel created")
        } else {
            Timber.i("createChannel: Existing channel reused")
        }
        return CHANNEL_ID
    }

    fun isAndroidOOrHigher() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O


}