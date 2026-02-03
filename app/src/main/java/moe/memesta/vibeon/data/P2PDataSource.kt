package moe.memesta.vibeon.data

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue

class P2PDataSource(
    private val repository: StreamRepository,
    private val scope: CoroutineScope
) : BaseDataSource(/* isNetwork= */ true) {

    private var uri: Uri? = null
    private var bytesRemaining: Long = C.LENGTH_UNSET.toLong()
    private val chunkQueue = LinkedBlockingQueue<ByteArray>()
    private var currentChunk: ByteArray? = null
    private var currentChunkOffset = 0
    private var isOpened = false

    init {
        scope.launch(Dispatchers.IO) {
            repository.streamEvents
                .filterIsInstance<StreamEvent.ChunkReceived>()
                .collect { event ->
                    chunkQueue.put(event.data)
                }
        }
    }

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        uri = dataSpec.uri
        
        // In a real implementation, we'd wait for the Header to get the file size
        // For now, we signal open and start consumption
        isOpened = true
        transferStarted(dataSpec)
        
        return C.LENGTH_UNSET.toLong()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        
        // Wait for data if queue is empty
        if (currentChunk == null || currentChunkOffset >= currentChunk!!.size) {
            currentChunk = chunkQueue.poll(5, java.util.concurrent.TimeUnit.SECONDS) ?: return C.RESULT_END_OF_INPUT
            currentChunkOffset = 0
        }

        val remainingInChunk = currentChunk!!.size - currentChunkOffset
        val bytesToRead = minOf(length, remainingInChunk)
        
        System.arraycopy(currentChunk!!, currentChunkOffset, buffer, offset, bytesToRead)
        currentChunkOffset += bytesToRead
        
        bytesTransferred(bytesToRead)
        return bytesToRead
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        if (isOpened) {
            isOpened = false
            transferEnded()
        }
        uri = null
    }
}
