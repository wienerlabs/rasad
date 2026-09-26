package xyz.wienerlabs.rasad.islam

import android.content.SharedPreferences
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class IslamicPreferences(private val store: SharedPreferences) {
    var prayer by mutableStateOf(
        PrayerSettings(
            twilight = enumOrDefault(store.getString(KEY_TWILIGHT, null), TwilightMethod.Diyanet),
            asr = enumOrDefault(store.getString(KEY_ASR, null), AsrMethod.Majority),
        ),
    )
        private set

    var hijriOffset by mutableIntStateOf(store.getInt(KEY_OFFSET, 0).coerceIn(-1, 1))
        private set

    fun updatePrayer(settings: PrayerSettings) {
        prayer = settings
        store.edit().putString(KEY_TWILIGHT, settings.twilight.name).putString(KEY_ASR, settings.asr.name).apply()
    }

    fun updateHijriOffset(days: Int) {
        hijriOffset = days.coerceIn(-1, 1)
        store.edit().putInt(KEY_OFFSET, hijriOffset).apply()
    }

    private companion object {
        const val KEY_TWILIGHT = "prayerTwilight"
        const val KEY_ASR = "prayerAsr"
        const val KEY_OFFSET = "hijriOffset"

        inline fun <reified T : Enum<T>> enumOrDefault(name: String?, fallback: T): T =
            name?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } } ?: fallback
    }
}
