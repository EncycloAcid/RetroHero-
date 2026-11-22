package com.retrohero.app.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

/** Small helpers over the Storage Access Framework document tree API. */
object Saf {
    data class Doc(val docId: String, val name: String, val isDir: Boolean, val size: Long)

    fun rootDocId(tree: Uri): String = DocumentsContract.getTreeDocumentId(tree)

    fun documentUri(tree: Uri, docId: String): Uri = DocumentsContract.buildDocumentUriUsingTree(tree, docId)

    fun children(resolver: ContentResolver, tree: Uri, docId: String): List<Doc> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
        )
        val out = ArrayList<Doc>()
        resolver.query(uri, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                val name = c.getString(1) ?: continue
                val isDir = c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR
                out += Doc(id, name, isDir, if (c.isNull(3)) 0L else c.getLong(3))
            }
        }
        return out
    }

    fun folderName(tree: Uri): String =
        rootDocId(tree).substringAfterLast(':').substringAfterLast('/').ifEmpty { rootDocId(tree) }
}
