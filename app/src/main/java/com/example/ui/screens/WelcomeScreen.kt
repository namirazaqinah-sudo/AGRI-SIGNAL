package com.example.ui.screens

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.FarmerAccountEntity
import com.example.ui.components.AgriAppBackground
import com.example.ui.components.AgriSignalLogoMark
import com.example.ui.components.AgriSignalWordmark
import com.example.ui.theme.EarthBrown
import com.example.ui.theme.ForestGreen
import java.util.Locale

@Composable
fun WelcomeScreen(
    farmer: FarmerAccountEntity,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val spokenGreeting = stringResource(id = R.string.welcome_voice_format, farmer.fullName)

    var visible by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }

    LaunchedEffect(Unit) { visible = true }

    fun playFallbackChime() {
        runCatching {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 65)
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 260)
            mainHandler.postDelayed({ runCatching { toneGen.release() } }, 400L)
        }
    }

    fun speakGreeting(tts: TextToSpeech) {
        runCatching {
            tts.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            val idLocale = Locale.Builder().setLanguage("id").setRegion("ID").build()
            val primaryRes = tts.setLanguage(idLocale)
            if (primaryRes == TextToSpeech.LANG_MISSING_DATA || primaryRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                val legacyId = Locale.Builder().setLanguage("in").setRegion("ID").build()
                val secondaryRes = tts.setLanguage(legacyId)
                if (secondaryRes == TextToSpeech.LANG_MISSING_DATA || secondaryRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.language = Locale.getDefault()
                }
            }
            tts.setSpeechRate(0.95f)
            tts.setPitch(1.02f)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    mainHandler.post { isSpeaking = true }
                }

                override fun onDone(utteranceId: String?) {
                    mainHandler.post { isSpeaking = false }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    mainHandler.post { isSpeaking = false }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    mainHandler.post { isSpeaking = false }
                }
            })
            val result = tts.speak(
                spokenGreeting,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "agri_welcome_${farmer.fullName.hashCode()}"
            )
            if (result == TextToSpeech.ERROR) {
                playFallbackChime()
            }
        }.onFailure {
            playFallbackChime()
        }
    }

    DisposableEffect(farmer.fullName) {
        var disposed = false
        var localTts: TextToSpeech? = null
        runCatching {
            localTts = TextToSpeech(context.applicationContext) { status ->
                mainHandler.post {
                    if (disposed) return@post
                    if (status == TextToSpeech.SUCCESS) {
                        ttsReady = true
                        localTts?.let { speakGreeting(it) }
                    } else {
                        playFallbackChime()
                    }
                }
            }
            ttsInstance = localTts
        }.onFailure {
            playFallbackChime()
        }

        onDispose {
            disposed = true
            isSpeaking = false
            ttsReady = false
            runCatching {
                localTts?.stop()
                localTts?.shutdown()
            }
            ttsInstance = null
        }
    }

    AgriAppBackground(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + scaleIn(
                    initialScale = 0.92f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 480.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AgriSignalLogoMark(size = 100.dp)
                        AgriSignalWordmark(fontSize = 32.sp)

                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(22.dp)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.welcome_greeting_format, farmer.fullName),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = EarthBrown,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.testTag("welcome_greeting_text")
                                )

                                FilledTonalButton(
                                    onClick = {
                                        val activeTts = ttsInstance
                                        if (ttsReady && activeTts != null) {
                                            speakGreeting(activeTts)
                                        } else {
                                            playFallbackChime()
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("welcome_voice_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                            contentDescription = stringResource(id = R.string.welcome_voice_replay),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (isSpeaking) {
                                                stringResource(id = R.string.welcome_voice_playing, farmer.fullName)
                                            } else {
                                                stringResource(id = R.string.welcome_voice_replay)
                                            },
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onContinue,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestGreen,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .testTag("welcome_continue_button")
                    ) {
                        Text(
                            text = stringResource(id = R.string.welcome_cta),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}
