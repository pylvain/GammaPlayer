package pylvain.gamma.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat.STATE_NONE
import android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING
import android.support.v4.media.session.PlaybackStateCompat.STATE_BUFFERING
import android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED
import android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.Player.STATE_ENDED
import com.google.android.exoplayer2.audio.AudioListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pylvain.gamma.Const
import pylvain.gamma.formatMs
import pylvain.gamma.library.*
import timber.log.Timber
import java.io.File
import java.lang.Exception


/*
-> Guarantees on functions?
 */

class BookPlayer(
    val context: Context,
    val player: SimpleExoPlayer,
    val db: BookDatabase,
    val libraryUtils: LibraryUtils
) { //Rename that. Set as Service ?

    //private val player by inject<SimpleExoPlayer>()

    //TODO inject that ?

    lateinit var book: BookEntry
        private set
    lateinit var files: Map<String, FileEntry> //TODO is that needed ?
        private set
    lateinit var currentBookmark: BookmarkEntry
        private set

    var pauseRewindMs = 2000L

    //TODO------------------------------------------------------------------------------------------

    /* val audioManager = AudioManager()

    */

    val audioFocusHelper = object : AudioManager.OnAudioFocusChangeListener {

        private fun requestAudioFocus(): Boolean = true
        private fun abandonAudioFocus() {}

        override fun onAudioFocusChange(focusChange: Int) {}

    }


    private val bookPlaybackStateBuilder by lazy {
        PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_FAST_FORWARD or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_REWIND or
                        PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP
            )
    }

    private fun makeState(state: Int) = bookPlaybackStateBuilder
        .setState(state, player.currentPosition, book.settings.speed).build()

    var playbackState: PlaybackStateCompat = bookPlaybackStateBuilder.build() //TODO
        private set

    var state: Int
        private set(v) {
            playbackState = makeState(v)
            emitMessage(makeMessage(MessageEmitReason.STATE_CHANGED))
        }
        get() = playbackState.state

    //----------------------------------------------------------------------------------------------

    private val cover by lazy { BitmapFactory.decodeFile(libraryUtils.getCover(File(book.source))) }

    private val bookMetadataBuilder by lazy {
        MediaMetadataCompat.Builder()
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, cover)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, cover)
            .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "Audiobook")
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, book.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, book.author)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, book.author)
            .putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, book.author)
            .putString(MediaMetadataCompat.METADATA_KEY_COMPOSER, book.author)
    }

    fun getMetadata(): MediaMetadataCompat = bookMetadataBuilder
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, player.duration)
        .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, currentIndex().toLong())
        .putString(
            MediaMetadataCompat.METADATA_KEY_TITLE,
            File(fileAtIndex(currentIndex()).source).nameWithoutExtension
        )
        .build()


    //----------------------------------------------------------------------------------------------

    /*
    The state property does not refers to the actual player.playbackState, but to a more general
    state (what would be the point of it if it dit ?). That means that for example, even if the
    player does stop between two consequent files, the state will and should remain State.PLAYING.
     */

    /*
    var state: PlaybackStateCompat
        private set(value) {
            if (value != _state.value) {
                _state.value = value
                emitMessage(makeMessage(MessageEmitReason.STATE_CHANGED))
            }
        }
        get() = _state.value

    private val _state = MutableStateFlow(bookPlaybackStateBuilder.build())*/
    //fun stateAsFlow(): StateFlow<PlaybackStateCompat> = _state.asStateFlow()

    //Messages--------------------------------------------------------------------------------------

    enum class MessageEmitReason { STATE_CHANGED, SETTINGS_CHANGED, TICK, INIT }

    //Do we attach metadata ?

    data class PlayerDataMessage(
        var reasonEmitted: MessageEmitReason = MessageEmitReason.INIT,
        var state: Int = STATE_NONE,
        var globalProgress: Pair<Long, Long> = 0L to 0L,
        var localProgress: Pair<Long, Long> = 0L to 0L,
        var title: String = ""
    )

    private val _messageFlow = MutableStateFlow<PlayerDataMessage>(PlayerDataMessage())
    val messageFlow: StateFlow<PlayerDataMessage> = _messageFlow

    fun makeMessage(reasonEmitted: MessageEmitReason) = PlayerDataMessage(
        reasonEmitted,
        state = this@BookPlayer.state,
        globalProgress = globalPosition() to book.totalDuration,
        localProgress = player.currentPosition to fileAtIndex(currentIndex()).duration,
        title = currentIndex().toString()
    )

    fun emitMessage(message: PlayerDataMessage) {
        _messageFlow.value = message
    }

    fun emitSettingsChanged() = emitMessage(makeMessage(MessageEmitReason.SETTINGS_CHANGED))

    //Tick------------------------------------------------------------------------------------------

    var tickIntervalMs = 250L
    var tickEnabled = false
        set(b) {
            field = if (b && !field) {
                mainHandler.post(tickRunner); true
            } else {
                mainHandler.removeCallbacks(tickRunner); false
            }
        }

    val mainHandler = Handler(Looper.getMainLooper())

    private val tickRunner = object : Runnable {
        override fun run() {
            if (tickEnabled) {
                emitMessage(makeMessage(MessageEmitReason.TICK))
            } //TODO load metadata
            mainHandler.postDelayed(this, tickIntervalMs)
        }
    }

    //db access-------------------------------------------------------------------------------------

    private val dbscope = CoroutineScope(Dispatchers.IO)

    fun downloadData(source: String = book.source) {
        book = db.bookDao().getBySource(source)
        currentBookmark = db.bookmarkDao().getByKey(book.settings.cursor)
        files = db.fileDao().getBySourceBook(source).associateBy { it.source } //TODO
    }

    fun uploadBookmark() = dbscope.launch { db.bookmarkDao().update(currentBookmark) }
    fun uploadBook() = dbscope.launch { db.bookDao().update(listOf(book)) }


//----------------------------------------------------------------------------------------------

    fun loadBook(source: String) {
        //state = STATE_BUFFERING
        downloadData(source)
        if (book.totalDuration < 1L) {
            libraryUtils.computeBookDuration(book.source)
            downloadData(source)
        }
        effects.initEffects()
        loadSettings()
        jumpTo(currentBookmark)
        setAutomaticPlayNext(true)
        state = STATE_STOPPED
    }

    fun loadSettings(settings: BookSettings = book.settings) {
        setVolume(settings.volume)
        setSpeed(settings.speed)
        setSkipSilencesEnabled(settings.skipSilences)
        effects.setLoudness(settings.loudness)
    }

    fun release() {
        setAutomaticPlayNext(false)
    }

//----------------------------------------------------------------------------------------------

    private val nextFileListener = object : Player.EventListener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == STATE_ENDED) {
                next()
            }
        }
    }

    private fun setAutomaticPlayNext(b: Boolean) = //Tested: cannot add the same listener twice
        if (b) player.addListener(nextFileListener)
        else player.removeListener(nextFileListener)

//----------------------------------------------------------------------------------------------

    private var lastIndexComputed = 0 //Just for optimisation purpose
    private fun currentIndex(bookmark: BookmarkEntry = currentBookmark): Int {
        for (i in (lastIndexComputed until book.playlist.size).union(0 until lastIndexComputed)) {
            if (book.playlist[i] == bookmark.file) {
                lastIndexComputed = i
                return i
            }
        }
        throw ArrayIndexOutOfBoundsException("computeIndex()")
    }

    fun globalPosition() = (0 until currentIndex()).fold(0L) { acc, it ->
        acc + fileAtIndex(it).duration
    } + player.currentPosition

//----------------------------------------------------------------------------------------------

    private fun setCurrentBookmark(b: BookmarkEntry) = currentBookmark.apply {
        time = b.time
        file = b.file
    }

    private fun updateCurrentBookmarkFromPlayer() {
        setCurrentBookmark(
            BookmarkEntry(
                time = if (player.duration != C.TIME_UNSET) player.duration else 0L,
                file = book.playlist[currentIndex()]
            )
        )
        uploadBookmark()
    }

//----------------------------------------------------------------------------------------------

    /*
        All position changes go through this function
     */

    fun jumpTo(bookmark: BookmarkEntry, keepIfPlaying: Boolean = true) {

        fun bufferFile(source: String) {
            player.stop(true)
            player.addMediaItem(MediaItem.fromUri(Uri.parse(source)))
            player.prepare()
        }

        Timber.i("Jumping at ${formatMs(bookmark.time)} in file :${bookmark.file}")

        if (bookmark.file != currentBookmark.file || player.mediaItemCount == 0)
            bufferFile(bookmark.file)

        player.seekTo(bookmark.time)
        setCurrentBookmark(bookmark)

        if (state == STATE_PLAYING && keepIfPlaying) player.play() // Hmm
        else {
            player.pause()
            state = STATE_PAUSED
        }

        uploadBookmark()
    }

    fun jumpToGlobal(timeMs: Long) {
        var time = timeMs
        for (i in book.playlist.indices) {
            val f = fileAtIndex(i)
            if (f.duration > time)
                time -= f.duration
            else
                jumpToIndex(i, time)
        }
    }

    fun jumpTo(file: String, timeMs: Long) = jumpTo(
        BookmarkEntry(file = file, time = timeMs)
    )

    fun jumpTo(timeMs: Long) {
        jumpTo(book.playlist[currentIndex()], timeMs)
    }

    private fun jumpToIndex(index: Int, timeMs: Long) {
        if (index >= 0 && index < files.size)
            jumpTo(BookmarkEntry(file = fileAtIndex(index).source, time = timeMs))
        else throw Exception("jumpToIndex: $index is out of bounds")
    }

    private fun fileAtIndex(index: Int): FileEntry =
        files[book.playlist[index]] ?: throw Exception("fileAtIndex($index)")

//----------------------------------------------------------------------------------------------

    private fun seekForward(
        timeMs: Long, index: Int = currentIndex(), position: Long = player.currentPosition
    ) {
        val currentDuration = fileAtIndex(index).duration
        val remaining = currentDuration - position
        when {
            timeMs < remaining -> jumpToIndex(index, position + timeMs)
            index < book.playlist.size - 1 -> seekForward(timeMs - remaining, index + 1, 0L)
        }
    }

    private fun seekBackward(
        timeMs: Long, index: Int = currentIndex(), position: Long = player.currentPosition
    ) {
        when {
            timeMs < position -> jumpToIndex(index, position - timeMs)
            index > 0 -> seekBackward(
                timeMs - position,
                index - 1,
                fileAtIndex(index - 1).duration - 1
            )
            index == 0 && timeMs > position -> jumpToIndex(0, 0L)
        }
    }

//High Level Functions-------------------------------------------------------------------------

    fun seek(timeMs: Long, noPrevious: Boolean = false) =
        if (timeMs >= 0) seekForward(timeMs)
        else {
            val rewindTimeMs = -timeMs
            if (noPrevious && rewindTimeMs > player.duration)
                jumpToIndex(currentIndex(), 0L)
            else seekBackward(rewindTimeMs)
        }

    fun next() = currentIndex().let { index ->
        if (index < book.playlist.size - 1) {
            jumpToIndex(index + 1, 0L)
        } else state = STATE_STOPPED
    }

    fun previous(guard: Boolean = true) = currentIndex().let { index ->
        if (guard && player.currentPosition > Const.PREVIOUS_GUARD_TIME) jumpToIndex(index, 0L)
        else if (index > 0) jumpToIndex(index - 1, 0L)
        else jumpToIndex(0, 0L)
    }

    fun togglePlayPause() {
        if (state == STATE_PLAYING) pause() else play()
    }

    fun play() {
        when (state) {
            STATE_PAUSED, STATE_STOPPED -> { // TODO STOPPED = end
                if (player.mediaItemCount < 1 || state == STATE_STOPPED)
                    jumpTo(currentBookmark)
                player.play()
                state = STATE_PLAYING
            }
            else -> Unit
        }
    }

    fun pause(rewind: Boolean = true) {
        if (state == STATE_PLAYING) {
            state = STATE_PAUSED
            player.pause()
            if ( rewind && pauseRewindMs > 0L) {
                seek(-pauseRewindMs, noPrevious = true)
            }
        }
    }

    fun stop() {
        state = STATE_STOPPED
        player.stop()
    }
//----------------------------------------------------------------------------------------------

    fun setVolume(f: Float) {
        player.volume = f
        book.settings.volume = f
    }

    fun setSpeed(f: Float) {
        book.settings.speed = f
        player.setPlaybackParameters(PlaybackParameters(f))
    }

    fun setSkipSilencesEnabled(b: Boolean) {
        book.settings.skipSilences = b
        player.skipSilenceEnabled = b
    }

//----------------------------------------------------------------------------------------------

    val effects = Effects()

    inner class Effects {

        private var loudnessEnhancer: LoudnessEnhancer? = null

        fun initEffects() {
            player.addAudioListener(
                object : AudioListener {
                    override fun onAudioSessionId(audioSessionId: Int) {
                        loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
                        setLoudness(book.settings.loudness)
                    }
                })
        }

        fun setLoudness(mdb: Int) = loudnessEnhancer?.apply {
            enabled = (mdb != 0)
            setTargetGain(mdb)
            book.settings.loudness = mdb
        }
    }

}

