package com.kithlo.noteboox.common

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception

class FileSystem(private val contentResolver: ContentResolver, private val baseDir: Uri) {
    private val baseDirId = DocumentsContract.getTreeDocumentId(baseDir)

    inner class Entry(
        private val mimeType: String,
        private var basename: String,
        private val paths: List<String>
    ) {
        val path = paths.joinToString("/")
        val fullName = path / basename

        private val uri = getUri(basename)
        private val ext
            get() =
                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) ""
                else basename.split('.').last()

        private val oldFile
            get() = basename.substring(0, basename.length - ext.length - 1) + "__old." + ext
        private val newFile
            get() = basename.substring(0, basename.length - ext.length - 1) + "__new." + ext

        fun exists(): Boolean {
            return exists(uri)
        }

        fun create() {
            create(path, basename, mimeType)
        }

        fun rename(newName: String) {
            basename = newName
            rename(uri, newName)
        }

        fun remove() {
            remove(uri)
        }

        fun resolve(
            name: String,
            mimeType: String = DocumentsContract.Document.MIME_TYPE_DIR
        ): Entry {
            val parts = split(name)
            if (parts.isEmpty()) {
                return this
            }
            return Entry(mimeType, parts.last(), paths + listOf(basename) + parts.dropLast(1))
        }

        fun <T> read(reader: (stream: InputStream) -> T?): T? {
            return try {
                val newUri = getUri(newFile)
                if (exists(newUri)) {
                    remove(newUri)
                }
                val oldUri = getUri(oldFile)
                if (exists(oldUri)) {
                    if (exists()) {
                        remove(oldUri)
                    } else {
                        rename(oldUri, basename)
                    }
                }
                contentResolver.openInputStream(uri)?.let { reader(it) }
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        fun write(writer: (stream: OutputStream) -> Unit) {
            val newUri = getUri(newFile)
            create(path, newFile, mimeType)
            val stream = contentResolver.openOutputStream(newUri, "wt") ?: return
            writer(stream)
            if (exists()) {
                rename(uri, oldFile)
                rename(newUri, basename)
                remove(getUri(oldFile))
            } else {
                rename(newUri, basename)
            }
        }

        private fun getUri(name: String): Uri {
            return DocumentsContract.buildDocumentUriUsingTree(
                baseDir,
                baseDirId / path / name
            )
        }
    }

    fun resolve(name: String, mimeType: String = DocumentsContract.Document.MIME_TYPE_DIR): Entry {
        val parts = split(name)
        if (parts.isEmpty()) {
            throw IllegalArgumentException("Invalid file name")
        }
        return Entry(mimeType, parts.last(), parts.dropLast(1))
    }

    private fun rename(uri: Uri, newName: String) {
        DocumentsContract.renameDocument(contentResolver, uri, newName)
    }

    private fun exists(uri: Uri): Boolean {
        try {
            val count = contentResolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null,
                null,
                null
            ).use { it?.count } ?: return false
            return count > 0
        } catch (e: Exception) {
            return false
        }
    }

    private fun remove(uri: Uri) {
        DocumentsContract.deleteDocument(contentResolver, uri)
    }

    private fun create(path: String, basename: String, mimeType: String) {
        DocumentsContract.createDocument(
            contentResolver,
            DocumentsContract.buildDocumentUriUsingTree(baseDir, baseDirId / path),
            mimeType,
            basename
        )
    }

    companion object {
        private fun split(path: String): List<String> {
            return path.split('/').filter { it.isNotBlank() }
        }

        private operator fun String.div(part: String): String {
            val first = trim('/')
            val second = part.trim('/')
            if (first.isEmpty()) {
                return second
            }
            if (second.isEmpty()) {
                return first
            }
            return "$first/$second"
        }
    }
}