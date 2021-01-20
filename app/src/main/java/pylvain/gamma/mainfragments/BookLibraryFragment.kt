package pylvain.gamma.mainfragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pylvain.gamma.MainApplication.Companion.mainContext
import pylvain.gamma.R
import pylvain.gamma.library.BookEntry
import pylvain.gamma.databinding.BookLibraryFragmentBinding
import pylvain.gamma.databinding.BookListElementBinding
import pylvain.gamma.library.BookDatabase
import pylvain.gamma.library.LibraryUtils
import java.io.File


class BookLibraryFragment : Fragment() {

    val db:BookDatabase by inject()
    val entries = emptyList<BookListElementBinding>()

    lateinit var b: BookLibraryFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        super.onCreateView(inflater, container, saved)
        b = BookLibraryFragmentBinding.inflate(layoutInflater)
        return b.root
    }

    override fun onViewCreated(view: View, saved: Bundle?) {

        val adapter = ViewAdapter(emptyList())
        b.mainRview.adapter = adapter
        b.mainRview.layoutManager = GridLayoutManager(context, 1)

        //--------------------------------------------------------------------------------------

        db.bookDao().getAllLive().observe(this.viewLifecycleOwner, { t -> adapter.setData(t) })

        val snackBar = Snackbar.make(b.root, R.string.importing, Snackbar.LENGTH_SHORT)
        snackBar.duration = 10000000

        /*lm.isImporting.observe(this.viewLifecycleOwner, Observer { b ->
            if (b) snackBar.show() else snackBar.dismiss()
        })*/

    }


    class ViewAdapter(var dataSet: List<BookEntry>) : KoinComponent,
        RecyclerView.Adapter<ViewAdapter.ViewHolder>() {

        val lvm: LibraryUtils by inject() //TODO inner class
        val db:BookDatabase by inject()

        class ViewHolder(val view: BookListElementBinding) : RecyclerView.ViewHolder(view.root) {}

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int) = ViewHolder(
            BookListElementBinding.inflate(LayoutInflater.from(mainContext))
        )

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val coverUri: Uri = Uri.parse(lvm.getCover(File(dataSet[position].source)))
            val entry: BookEntry = db.bookDao().getBySource(dataSet[position].source) ?: return
            holder.view.apply {
                cover.setImageURI(coverUri)
                folderName.text = File(entry.source).name
                title.text = entry.title
                progress.progress = (0..100).random()
            }
        }

        override fun getItemCount(): Int = dataSet.size

        fun setData(d: List<BookEntry>) {
            dataSet = d
            notifyDataSetChanged()
        }

    }

}

//TODO make a generic adapter