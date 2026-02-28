package moe.memesta.vibeon.widget

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.glance.state.GlanceStateDefinition
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Glance state definition that persists [WidgetPlaybackState] as JSON in a DataStore file.
 */
object WidgetStateDefinition : GlanceStateDefinition<WidgetPlaybackState> {

    private const val FILE_NAME = "vibeon_widget_state_json"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val Context.widgetDataStore: DataStore<WidgetPlaybackState> by dataStore(
        fileName = FILE_NAME,
        serializer = WidgetPlaybackStateSerializer(json)
    )

    override suspend fun getDataStore(
        context: Context,
        fileKey: String
    ): DataStore<WidgetPlaybackState> = context.widgetDataStore

    override fun getLocation(context: Context, fileKey: String): File =
        File(context.filesDir, "datastore/$FILE_NAME")
}

private class WidgetPlaybackStateSerializer(
    private val json: Json
) : Serializer<WidgetPlaybackState> {

    override val defaultValue: WidgetPlaybackState = WidgetPlaybackState()

    override suspend fun readFrom(input: InputStream): WidgetPlaybackState {
        return try {
            val text = input.bufferedReader().use { it.readText() }
            if (text.isBlank()) defaultValue
            else json.decodeFromString<WidgetPlaybackState>(text)
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot read widget state JSON", e)
        }
    }

    override suspend fun writeTo(t: WidgetPlaybackState, output: OutputStream) {
        output.bufferedWriter().use { it.write(json.encodeToString(WidgetPlaybackState.serializer(), t)) }
    }
}
