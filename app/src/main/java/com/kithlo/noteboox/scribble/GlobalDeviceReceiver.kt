package com.kithlo.noteboox.scribble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.onyx.android.sdk.utils.BroadcastHelper
import com.onyx.android.sdk.utils.DeviceReceiver
import com.onyx.android.sdk.utils.StringUtils

class GlobalDeviceReceiver : BroadcastReceiver() {
    private var systemNotificationPanelChangeListener: ((open: Boolean) -> Unit)? = null
    private var systemScreenOnListener: (() -> Unit)? = null

    fun setSystemNotificationPanelChangeListener(listener: ((open: Boolean) -> Unit)?): GlobalDeviceReceiver {
        systemNotificationPanelChangeListener = listener
        return this
    }

    fun setSystemScreenOnListener(listener: (() -> Unit)?): GlobalDeviceReceiver {
        systemScreenOnListener = listener
        return this
    }

    fun enable(context: Context, enable: Boolean) {
        try {
            if (enable) {
                BroadcastHelper.ensureRegisterReceiver(context, this, intentFilter())
            } else {
                BroadcastHelper.ensureUnregisterReceiver(context, this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun intentFilter(): IntentFilter {
        val filter = IntentFilter()
        filter.addAction(SYSTEM_UI_DIALOG_OPEN_ACTION)
        filter.addAction(SYSTEM_UI_DIALOG_CLOSE_ACTION)
        filter.addAction(SYSTEM_SCREEN_ON_ACTION)
        return filter
    }

    private fun handleSystemUIDialogAction(intent: Intent) {
        val action = intent.action
        val dialogType = intent.getStringExtra(DIALOG_TYPE)
        var open = false
        if (StringUtils.safelyEquals(dialogType, DIALOG_TYPE_NOTIFICATION_PANEL)) {
            if (StringUtils.safelyEquals(action, SYSTEM_UI_DIALOG_OPEN_ACTION)) {
                open = true
            } else if (StringUtils.safelyEquals(action, SYSTEM_UI_DIALOG_CLOSE_ACTION)) {
                open = false
            }
            systemNotificationPanelChangeListener?.invoke(open)
        }
    }

    private fun handSystemScreenOnAction() {
        systemScreenOnListener?.invoke()
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (StringUtils.safelyEquals(action, SYSTEM_UI_DIALOG_OPEN_ACTION)
            || StringUtils.safelyEquals(action, SYSTEM_UI_DIALOG_CLOSE_ACTION)
        ) {
            handleSystemUIDialogAction(intent)
        } else if (StringUtils.safelyEquals(action, SYSTEM_SCREEN_ON_ACTION)) {
            handSystemScreenOnAction()
        }
    }

    companion object {
        const val SYSTEM_UI_DIALOG_OPEN_ACTION = DeviceReceiver.SYSTEM_UI_DIALOG_OPEN_ACTION
        const val SYSTEM_UI_DIALOG_CLOSE_ACTION = DeviceReceiver.SYSTEM_UI_DIALOG_CLOSE_ACTION
        const val SYSTEM_SCREEN_ON_ACTION = Intent.ACTION_SCREEN_ON
        const val DIALOG_TYPE_NOTIFICATION_PANEL = DeviceReceiver.DIALOG_TYPE_NOTIFICATION_PANEL
        const val DIALOG_TYPE = DeviceReceiver.DIALOG_TYPE
    }
}