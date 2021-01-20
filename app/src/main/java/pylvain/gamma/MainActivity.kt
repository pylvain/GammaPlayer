package pylvain.gamma

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import pylvain.gamma.MainApplication.Companion.mainContext
import pylvain.gamma.library.BookDatabase
import pylvain.gamma.library.LibraryUtils
import pylvain.gamma.player.BookPlayer
import pylvain.gamma.playerservice.PlayerService
import pylvain.gamma.viewmodels.PlayerVM
import timber.log.Timber
import java.io.File


class MainActivity : AppCompatActivity() {

    private val bp by inject<BookPlayer>()
    private val libraryUtils by inject<LibraryUtils>()
    private val db by inject<BookDatabase>()


    private val playerVM:PlayerVM by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.i(db.isOpen().toString())

        runBlocking {
        CoroutineScope(Dispatchers.IO).launch {
            libraryUtils.apply {
                deleteDB()
                addLibrary(File("/storage/self/primary/Audiobooks"))
                addLibrary(File("/sdcard/Audiobooks"))
            }
            val books = db.bookDao().getAll()
            Timber.d("Found ${books.size} books")
            bp.loadBook(books[0].source)
            bp.play()
            bp.setSpeed(2f)
            bp.tickEnabled = true

            bp.next()
            //playerVM.onNext()

            delay(300)

            startService(Intent(mainContext, PlayerService::class.java))
        }}



        Timber.i("MAIN")

        setContentView(R.layout.activity_main)
    }

}
