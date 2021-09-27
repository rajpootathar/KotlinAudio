package com.doublesymmetry.kotlinaudio.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.RequiresApi
import com.doublesymmetry.kotlinaudio.R
import com.doublesymmetry.kotlinaudio.models.NotificationButton
import com.doublesymmetry.kotlinaudio.models.NotificationConfig
import com.doublesymmetry.kotlinaudio.utils.isJUnitTest
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class NotificationManager(private val context: Context, private val exoPlayer: ExoPlayer) : PlayerNotificationManager.PrimaryActionReceiver {
    private val descriptionAdapter = DescriptionAdapter(context, null)
    private val mediaSession: MediaSessionCompat = MediaSessionCompat(context, "AudioPlayerSession")
    private val mediaSessionConnector: MediaSessionConnector = MediaSessionConnector(mediaSession)

    internal val onNotificationAction = MutableSharedFlow<NotificationButton.Action>()

    private val scope = CoroutineScope(Dispatchers.Main)

    val buttons = mutableSetOf<NotificationButton?>()

    private val channelId: String

    private lateinit var playerNotificationManager: PlayerNotificationManager

    private var isNotificationCreated = false

    init {
        channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        } else {
            ""
        }

        mediaSessionConnector.setPlayer(exoPlayer)

//
//        playerNotificationManager = PlayerNotificationManager.Builder(context, NOTIFICATION_ID, channelId)
//            .setMediaDescriptionAdapter(descriptionAdapter)
//            .setPrimaryActionReceiver(this)
//            .build()
//
//        if (!isJUnitTest()) {
//            playerNotificationManager.apply {
//                setPlayer(exoPlayer)
//                setMediaSessionToken(mediaSession.sessionToken)
////                setUsePlayPauseActions(false)
//                setUseFastForwardAction(false)
//                setUseNextAction(false)
//                setUsePreviousAction(false)
//                setUseStopAction(false)
//                setUseRewindAction(false)
//            }
//        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = CHANNEL_ID
        val channelName = context.getString(R.string.playback_channel_name)
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        channel.description = "Used when playing music"
        channel.setSound(null, null)

        val service = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return channelId
    }

    fun createNotification(config: NotificationConfig) {
        if (isNotificationCreated) error("Cannot recreate notification once it's been created.")

        buttons.apply {
            clear()
            addAll(config.buttons)
        }

        playerNotificationManager = PlayerNotificationManager.Builder(context, NOTIFICATION_ID, channelId).apply {
            setMediaDescriptionAdapter(descriptionAdapter)

            if (buttons.isNotEmpty()) {
                setPrimaryActionReceiver(this@NotificationManager)

                config.buttons.forEach { button ->
                    when (button) {
                        is NotificationButton.PLAY -> button.icon?.let { setPlayActionIconResourceId(it) }
                        is NotificationButton.PAUSE -> button.icon?.let { setPauseActionIconResourceId(it) }
                        is NotificationButton.STOP -> button.icon?.let { setStopActionIconResourceId(it) }
                        is NotificationButton.FORWARD -> button.icon?.let { setFastForwardActionIconResourceId(it) }
                        is NotificationButton.REWIND -> button.icon?.let { setRewindActionIconResourceId(it) }
                        is NotificationButton.NEXT -> button.icon?.let { setNextActionIconResourceId(it) }
                        is NotificationButton.PREVIOUS -> button.icon?.let { setPreviousActionIconResourceId(it) }
                    }
                }
            }
        }.build()


        if (!isJUnitTest()) {
            playerNotificationManager.apply {
                setPlayer(exoPlayer)

                config.buttons.forEach { button ->
                    when (button) {
                        is NotificationButton.PLAY, is NotificationButton.PAUSE -> setUsePlayPauseActions(true)
                        is NotificationButton.STOP -> setUseStopAction(true)
                        is NotificationButton.FORWARD -> {
                            setUseFastForwardAction(true)
                            setUseFastForwardActionInCompactView(button.isCompact)
                        }
                        is NotificationButton.REWIND -> {
                            setUseRewindAction(true)
                            setUseRewindActionInCompactView(button.isCompact)
                        }
                        is NotificationButton.NEXT -> {
                            setUseNextAction(true)
                            setUseNextActionInCompactView(button.isCompact)
                        }
                        is NotificationButton.PREVIOUS -> {
                            setUsePreviousAction(true)
                            setUsePreviousActionInCompactView(button.isCompact)
                        }
                    }
                }

                setMediaSessionToken(mediaSession.sessionToken)
                setUsePlayPauseActions(buttons.any { it is NotificationButton.PLAY || it is NotificationButton.PAUSE })
                setUseFastForwardAction(buttons.any { it is NotificationButton.FORWARD })
                setUseRewindAction(buttons.any { it is NotificationButton.REWIND })
                setUseNextAction(buttons.any { it is NotificationButton.NEXT })
                setUsePreviousAction(buttons.any { it is NotificationButton.PREVIOUS })
                setUseStopAction(buttons.any { it is NotificationButton.STOP })
            }
        }
    }

    override fun onAction(player: Player, action: String, intent: Intent) {
        scope.launch {
            onNotificationAction.emit(NotificationButton.valueOf(action))
        }
    }

    fun onPlay() {
        mediaSession.isActive = true
    }

//    fun refresh() {
//        if (!isJUnitTest()) {
//            playerNotificationManager.apply {
//                setPlayer(exoPlayer)
//                setMediaSessionToken(mediaSession.sessionToken)
//                setUsePlayPauseActions(buttons.any { it is NotificationButton.PLAY || it is NotificationButton.PAUSE })
//                setUseFastForwardAction(buttons.any { it is NotificationButton.FORWARD })
//                setUseRewindAction(buttons.any { it is NotificationButton.REWIND })
//                setUseNextAction(buttons.any { it is NotificationButton.NEXT })
//                setUsePreviousAction(buttons.any { it is NotificationButton.PREVIOUS })
//                setUseStopAction(buttons.any { it is NotificationButton.STOP })
//            }
//        }
//    }

    fun destroy() {
        descriptionAdapter.release()
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "kotlin_audio_player"
    }
}