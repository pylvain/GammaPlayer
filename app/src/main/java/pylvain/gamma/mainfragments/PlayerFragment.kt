package pylvain.gamma.mainfragments

import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.compat.ScopeCompat.viewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import pylvain.gamma.R
import pylvain.gamma.databinding.PlayerBinding
import pylvain.gamma.library.BookDatabase
import pylvain.gamma.library.LibraryUtils
import pylvain.gamma.player.BookPlayer
import pylvain.gamma.viewmodels.PlayerVM
import timber.log.Timber
import java.io.File

class PlayerFragment : Fragment() {

    lateinit var b: PlayerBinding

    override fun onCreateView(i: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        super.onCreateView(i, container, saved)
        b = PlayerBinding.inflate(layoutInflater)
        return b.root
    }

    override fun onStart() {

        val vm = getViewModel<PlayerVM>()

        vm.viewModelScope.launch {
            vm.fileProgress.collect {
                b.fileProgressBar.progress = it
            }
        }

        b.nextButton.setOnClickListener {
            vm.test()
        }

        b.previousButton.setOnClickListener {
        }

        b.plus1Button.setOnClickListener {
        }

        b.minus1Button.setOnClickListener {
        }

        b.playButton.setOnClickListener {
            vm.onPlay()
        }

        val mediaSession = MediaSessionCompat(this.context, "PlayerService")
        val mediaController = MediaControllerCompat(context,mediaSession.sessionToken)

        val notification = NotificationCompat.Builder(context)

        super.onStart()
    }


}