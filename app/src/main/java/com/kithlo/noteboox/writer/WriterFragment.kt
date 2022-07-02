package com.kithlo.noteboox.writer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.kithlo.noteboox.MainActivity
import com.kithlo.noteboox.R
import com.kithlo.noteboox.common.FileSystem
import com.kithlo.noteboox.common.FileTreeViewModel
import com.kithlo.noteboox.data.file.LeafNode
import com.kithlo.noteboox.databinding.FragmentWriterBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WriterFragment : Fragment(R.layout.fragment_writer) {

    companion object {
        val TAG: String = WriterFragment::class.java.simpleName
    }

    private lateinit var binding: FragmentWriterBinding

    private val fileTreeModel: FileTreeViewModel by viewModels(
        ownerProducer = {
            requireActivity()
        }
    )

    private lateinit var fileSystem: FileSystem

    private val node: LeafNode
        get() = fileTreeModel.currentLeaf!!

    private lateinit var bitmapsRepository: BitmapsRepository

    private val model: WriterViewModel by viewModels {
        WriterViewModel.Factory(
            fileTreeModel,
            object : WriterViewModel.Callback {
                override fun onEdit() {
                    startEditing()
                }

                override fun onBack() {
                    goBack()
                }

                override fun onDiscard() {
                    stopEditing()
                }

                override fun onSave() {
                    stopEditing()
                }

                override fun onAddPage(page: Int) {
                    bitmapsRepository.add(page)
                }

                override fun onChangePage(page: Int, prev: Int) {
                    bitmapsRepository.change(page, prev)
                }

                override fun onRemove(page: Int) {
                    bitmapsRepository.remove(page)
                }
            }
        )
    }

    private fun getDirOrNull(): FileSystem.Entry? {
        val dir = fileSystem.resolve(node.uuid.toString())
        if (!dir.exists()) {
            return null
        }
        return dir
    }

    private fun getDir(): FileSystem.Entry {
        val dir = fileSystem.resolve(node.uuid.toString())
        if (!dir.exists()) {
            dir.create()
        }
        return dir
    }

    private fun getFileName(page: Int): String {
        return "$page".padStart(4, '0') + ".png"
    }

    private fun getFile(dir: FileSystem.Entry, page: Int): FileSystem.Entry {
        return dir.resolve(getFileName(page), "image/png")
    }

    private fun loadBitmaps(): MutableList<Bitmap?> {
        val dir = getDirOrNull() ?: return mutableListOf(null)
        val pages = node.pages
        val list = mutableListOf<Bitmap?>()
        for (i in 1..pages) {
            val file = getFile(dir, i)
            val bitmap = file.read {
                BitmapFactory.decodeStream(it)
            }?.copy(Bitmap.Config.ARGB_8888, true)
            list.add(bitmap)
        }
        return list
    }

    private fun createBitmapsRepository(bitmaps: MutableList<Bitmap?>): BitmapsRepository {
        return BitmapsRepository(lifecycleScope, bitmaps, object : BitmapsRepository.Callback {
            override fun getBitmap(): Bitmap? {
                return binding.scribbleView.bitmap
            }

            override fun changeBitmap(bitmap: Bitmap?): Bitmap? {
                return binding.scribbleView.swapBitmap(bitmap)
            }

            override fun finish() {
                parentFragmentManager.popBackStack()
                fileTreeModel.currentLeaf = null
            }

            override fun save(bitmap: Bitmap, page: Int) {
                val dir = getDir()
                val file = getFile(dir, page)
                file.write {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }

            override fun move(from: Int, to: Int) {
                val dir = getDirOrNull() ?: return
                if (getFile(dir, to).exists()) {
                    return
                }
                val file = getFile(dir, from)
                if (file.exists()) {
                    file.rename(getFileName(to))
                }
            }

            override fun remove(page: Int) {
                val dir = getDirOrNull() ?: return
                val file = getFile(dir, page)
                if (file.exists()) {
                    file.remove()
                }
            }
        })
    }

    private fun goBack() {
        stopEditing()
        bitmapsRepository.finish()
    }

    private fun startEditing() {
        (requireActivity() as MainActivity).shouldAllowBack = false
        bitmapsRepository.start()
        binding.scribbleView.resume()
    }

    private fun stopEditing() {
        binding.scribbleView.pause()
        bitmapsRepository.stop()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                fileTreeModel.save()
            }
        }
        (requireActivity() as MainActivity).shouldAllowBack = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWriterBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = model
        binding.titleInput.setOnFocusChangeListener { _, b ->
            if (b) {
                stopEditing()
            } else if (model.editing.value) {
                startEditing()
            }
        }
        binding.titleInput.setOnEditorActionListener { textView, _, _ ->
            textView.clearFocus()
            false
        }
        binding.scribbleView.backgroundPainter = {
            node.paper.drawOn(it)
        }
        fileSystem = FileSystem(requireActivity().contentResolver, fileTreeModel.baseDir)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val bitmaps = loadBitmaps()
        bitmapsRepository = createBitmapsRepository(bitmaps)
        binding.scribbleView.swapBitmap(bitmaps[0])
    }

    override fun onDestroyView() {
        binding.scribbleView.quit()
        super.onDestroyView()
    }
}