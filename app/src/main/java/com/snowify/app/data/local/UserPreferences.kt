package com.snowify.app.data.local
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.snowify.app.util.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "snowify_prefs")
class UserPreferences(private val context: Context) {
    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val VOLUME_KEY = floatPreferencesKey("volume")
        val SHUFFLE_KEY = booleanPreferencesKey("shuffle")
        val REPEAT_MODE_KEY = stringPreferencesKey("repeat_mode")
        val AUTOPLAY_KEY = booleanPreferencesKey("autoplay")
        val AUDIO_QUALITY_KEY = stringPreferencesKey("audio_quality")
        val VIDEO_QUALITY_KEY = stringPreferencesKey("video_quality")
        val NORMALIZATION_KEY = booleanPreferencesKey("normalization")
        val NORMALIZATION_TARGET_KEY = intPreferencesKey("normalization_target")
        val ANIMATIONS_KEY = booleanPreferencesKey("animations")
        val EFFECTS_KEY = booleanPreferencesKey("effects")
        val COUNTRY_KEY = stringPreferencesKey("country")
        val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
        val MUSIC_ONLY_KEY = booleanPreferencesKey("music_only")
        val LISTENING_ACTIVITY_KEY = booleanPreferencesKey("listening_activity")
        val FOLLOWED_ARTISTS_KEY = stringPreferencesKey("followed_artists_json")
    }
    val themeFlow: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        AppTheme.fromString(prefs[THEME_KEY] ?: AppTheme.DARK.id)
    }
    val volumeFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[VOLUME_KEY] ?: 1.0f
    }
    val shuffleFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SHUFFLE_KEY] ?: false
    }
    val autoplayFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTOPLAY_KEY] ?: true
    }
    val audioQualityFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[AUDIO_QUALITY_KEY] ?: "bestaudio"
    }
    val normalizationFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[NORMALIZATION_KEY] ?: false
    }
    val normalizationTargetFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[NORMALIZATION_TARGET_KEY] ?: -14
    }
    val animationsFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ANIMATIONS_KEY] ?: true
    }
    val effectsFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[EFFECTS_KEY] ?: true
    }
    val countryFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[COUNTRY_KEY] ?: "US"
    }
    val onboardingCompleteFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETE_KEY] ?: false
    }
    val listeningActivityFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[LISTENING_ACTIVITY_KEY] ?: true
    }
    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[THEME_KEY] = theme.id }
    }
    suspend fun setVolume(volume: Float) {
        context.dataStore.edit { it[VOLUME_KEY] = volume }
    }
    suspend fun setShuffle(shuffle: Boolean) {
        context.dataStore.edit { it[SHUFFLE_KEY] = shuffle }
    }
    suspend fun setAutoplay(autoplay: Boolean) {
        context.dataStore.edit { it[AUTOPLAY_KEY] = autoplay }
    }
    suspend fun setAudioQuality(quality: String) {
        context.dataStore.edit { it[AUDIO_QUALITY_KEY] = quality }
    }
    suspend fun setNormalization(enabled: Boolean) {
        context.dataStore.edit { it[NORMALIZATION_KEY] = enabled }
    }
    suspend fun setNormalizationTarget(target: Int) {
        context.dataStore.edit { it[NORMALIZATION_TARGET_KEY] = target }
    }
    suspend fun setAnimations(enabled: Boolean) {
        context.dataStore.edit { it[ANIMATIONS_KEY] = enabled }
    }
    suspend fun setEffects(enabled: Boolean) {
        context.dataStore.edit { it[EFFECTS_KEY] = enabled }
    }
    suspend fun setCountry(country: String) {
        context.dataStore.edit { it[COUNTRY_KEY] = country }
    }
    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[ONBOARDING_COMPLETE_KEY] = complete }
    }
    suspend fun setListeningActivity(enabled: Boolean) {
        context.dataStore.edit { it[LISTENING_ACTIVITY_KEY] = enabled }
    }
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    data class FollowedArtistLocal(val artistId: String, val name: String, val avatar: String)

    val followedArtistsFlow: Flow<List<FollowedArtistLocal>> = context.dataStore.data.map { prefs ->
        val json = prefs[FOLLOWED_ARTISTS_KEY] ?: "[]"
        try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                FollowedArtistLocal(
                    artistId = obj.optString("artistId", ""),
                    name = obj.optString("name", ""),
                    avatar = obj.optString("avatar", ""),
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun setFollowedArtists(artists: List<FollowedArtistLocal>) {
        val arr = JSONArray()
        artists.forEach { a ->
            arr.put(JSONObject().apply {
                put("artistId", a.artistId)
                put("name", a.name)
                put("avatar", a.avatar)
            })
        }
        context.dataStore.edit { it[FOLLOWED_ARTISTS_KEY] = arr.toString() }
    }
}
