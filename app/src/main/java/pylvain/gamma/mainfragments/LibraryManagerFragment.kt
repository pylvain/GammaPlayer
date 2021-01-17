package pylvain.gamma.mainfragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment.getExternalStorageDirectory
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pylvain.gamma.R
import pylvain.gamma.databinding.BookLibraryListElementBinding
import pylvain.gamma.databinding.LibraryManagerFragmentBinding
import pylvain.gamma.library.BookDatabase
import pylvain.gamma.library.LibraryEntry
import pylvain.gamma.library.LibraryUtils
import pylvain.gamma.logd
import java.io.File
import java.lang.Exception


//TODO do i need to call super ?
//TODO Rename b.view.adapter

class LibraryManagerFragment : Fragment() {

    private val REQUEST_CODE_OPEN_DIRECTORY = 1

    val db:BookDatabase by inject()
    val libraryUtils:LibraryUtils by inject()

    lateinit var b: LibraryManagerFragmentBinding
    lateinit var toolbar: Toolbar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        super.onCreateView(inflater, container, saved)
        b = LibraryManagerFragmentBinding.inflate(layoutInflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.fab.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY)
        }
        b.fab.show()

        //------------------------------------------------------------------------------------------

        val adapter = ViewAdapter(arrayListOf())
        b.mainRview.adapter = adapter
        b.mainRview.layoutManager = GridLayoutManager(context, 1)

        db.libraryDao().getAllLive().observe(viewLifecycleOwner, Observer<List<LibraryEntry>> { t ->
            adapter.setData(t.toMutableList())
        })

        //------------------------------------------------------------------------------------------


        toolbar = b.includedToolbar.toolbar
        toolbar.title = getString(R.string.library_manager)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        /*
        toolbar.menuInflater.inflate(R.menu.library_manager, toolbar.menu)
        toolbar.navigationIcon = ContextCompat.getDrawable(activity!!.applicationContext,
            R.drawable.ic_arrow_back_black_18dp
        )*/

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
            if(requestCode == REQUEST_CODE_OPEN_DIRECTORY && data != null) {
                val uri: Uri = data.data!!
                try {
                    GlobalScope.launch { libraryUtils.addLibrary(File(resolveContentUri(uri))) }
                } catch (e:Exception) {
                    logd("Can't resolve $uri")
                    logd(e.printStackTrace())
                }
                logd(resolveContentUri(uri))
            }
        }


    class ViewAdapter(var dataSet: MutableList<LibraryEntry>) : KoinComponent,
        RecyclerView.Adapter<ViewAdapter.ViewHolder>() {

        val lvm: LibraryUtils by inject()
        val db: BookDatabase by inject()
        //ViewBinding does'nt work for no reason

        class ViewHolder(view: BookLibraryListElementBinding) : RecyclerView.ViewHolder(view.root) {
            val textView: TextView = view.folderName
            val button: ImageButton = view.button
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : ViewHolder {
            return ViewHolder(BookLibraryListElementBinding.inflate(LayoutInflater.from(parent.context)))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.button.setOnClickListener { this.onClicked(position) }
            holder.textView.text = dataSet[position].source
        }

        fun onClicked(position: Int) {
            logd(dataSet[position].source)
            db.libraryDao().deleteBySource(dataSet[position].source)
            notifyItemRangeChanged(position, dataSet.size)
        }


        fun removeAt(position: Int) { // Not usefull since livedata is used
            dataSet.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, dataSet.size)
        }

        override fun getItemCount(): Int = dataSet.size

        fun setData(d: MutableList<LibraryEntry>) {
            dataSet = d
            notifyDataSetChanged()
        }
    }


    private fun resolveContentUri(uri:Uri): String { //Dégueulasse

        val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        val docCursor = requireContext().contentResolver.query(docUri, null, null, null, null)

        var str = ""

        // get a string of the form : primary:Audiobooks or 1407-1105:Audiobooks
        while(docCursor!!.moveToNext()) {
            str = docCursor.getString(0)
            if(str.matches(Regex(".*:.*"))) break //Maybe useless
        }

        docCursor.close()

        val split = str.split(":")

        val base: File =
            if(split[0] == "primary") getExternalStorageDirectory()
            else File("/storage/${split[0]}")

        if(!base.isDirectory) throw Exception("'$uri' cannot be resolved in a valid path")
        return File(base,split[1]).canonicalPath
    }


}


