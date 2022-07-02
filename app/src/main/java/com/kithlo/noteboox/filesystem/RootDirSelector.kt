package com.kithlo.noteboox.filesystem

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit

object RootDirSelector {
    val TAG: String = RootDirSelector::class.java.simpleName
    private const val PREFERENCES = "PREFERENCES"
    private const val BASE_DIR_KEY = "BASE_DIR"

    fun get(context: ComponentActivity): Uri? {
        val preferences = context.getSharedPreferences(
            PREFERENCES,
            Context.MODE_PRIVATE
        )
        val baseDir = preferences.getString(BASE_DIR_KEY, null) ?: return null
        val permissions = context.contentResolver.persistedUriPermissions
        for (perm in permissions) {
            val uri = perm.uri
            if (uri.toString() == baseDir) {
                return uri
            }
        }
        preferences.edit {
            remove(BASE_DIR_KEY)
        }
        return null
    }

    fun put(context: ComponentActivity, uri: Uri?) {
        val preferences = context.getSharedPreferences(
            PREFERENCES,
            Context.MODE_PRIVATE
        )
        preferences.edit {
            if (uri == null) {
                remove(BASE_DIR_KEY)
            } else {
                putString(BASE_DIR_KEY, uri.toString())
            }
        }
    }

    fun launch(context: ComponentActivity, callback: (uri: Uri) -> Unit) {
        val resultLauncher =
            context.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val uri = result.data?.data
                    if (uri != null) {
                        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                        callback(uri)
                    }
                }
            }
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        )

        resultLauncher.launch(intent)
    }
}