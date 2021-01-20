package pylvain.gamma.mainfragments

import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.getViewModel
import pylvain.gamma.databinding.PlayerBinding
import pylvain.gamma.viewmodels.PlayerVM

class PlayerFragment : Fragment() {

    lateinit var b: PlayerBinding

    override fun onCreateView(i: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        super.onCreateView(i, container, saved)
        b = PlayerBinding.inflate(layoutInflater)
        return b.root
    }

    override fun onStart() {

        val vm = getViewModel<PlayerVM>()

        vm.viewModelScope.apply {
            launch {
                vm.fileProgress.collect {
                    b.fileProgressBar.progress = it
                }
            }
            launch {
                vm.bp.messageFlow.collect() {
                    b.bookTitle.text = it.title
                }
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
        val mediaController = MediaControllerCompat(context, mediaSession.sessionToken)

        val notification = NotificationCompat.Builder(context)

        super.onStart()
    }


}