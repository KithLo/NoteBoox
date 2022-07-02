package com.kithlo.noteboox

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.kithlo.noteboox.filesystem.FilesystemFragment

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    companion object {
        val TAG: String = MainActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<FilesystemFragment>(R.id.fragment_container_view)
            }
        }
    }

    var shouldAllowBack: Boolean = true

    override fun onBackPressed() {
        if (shouldAllowBack) {
            super.onBackPressed()
        }
    }
}