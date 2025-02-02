package com.doublesymmetry.kotlinaudio.models

import android.app.Notification

sealed class NotificationState {
    class POSTED(val notificationId: Int, val notification: Notification) : NotificationState()
    class CANCELLED(val notificationId: Int): NotificationState()
}