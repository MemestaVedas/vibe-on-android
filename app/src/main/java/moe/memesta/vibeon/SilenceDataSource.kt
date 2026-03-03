package moe.memesta.vibeon

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import kotlin.math.min

class SilenceDataSource : DataSource {
    private var bytesRemaining: Long = 0
    private var opened: Boolean = false
    private var uri: Uri? = null

    class Factory : DataSource.Factory {
        override fun createDataSource(): DataSource = SilenceDataSource()
    }

    override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) {}

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        opened = true
        // Allow massive length to prevent reaching EOF, effectively infinite silence.
        // 44100 Hz * 2 channels * 2 bytes = 176,400 bytes/sec
        // 1000 hours = ~6.3 GB, easily fits in Long and provides essentially indefinite silence
        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            63504000000L 
        }
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val bytesToRead = min(length.toLong(), bytesRemaining).toInt()
        // Fill with zeros (silence)
        for (i in 0 until bytesToRead) {
            buffer[offset + i] = 0
        }
        bytesRemaining -= bytesToRead
        return bytesToRead
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        opened = false
    }
}
