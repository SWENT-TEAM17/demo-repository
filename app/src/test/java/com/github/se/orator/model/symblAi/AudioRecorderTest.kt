package com.github.se.orator.model.symblAi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioRecord
import androidx.core.app.ActivityCompat
import java.io.File
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.*

class AudioRecorderTest {

  private lateinit var context: Context
  private lateinit var audioRecorder: AudioRecorder

  @Before
  fun setUp() {
    context = mock()
    audioRecorder = AudioRecorder(context, isOffline= false)
  }

  @Test
  fun testWriteInt() {
    val header = ByteArray(44)
    val offset = 4
    val value = 36

    audioRecorder.writeInt(header, offset, value)

    assertEquals(36, header[offset].toInt() and 0xff)
    assertEquals(0, header[offset + 1].toInt() and 0xff)
    assertEquals(0, header[offset + 2].toInt() and 0xff)
    assertEquals(0, header[offset + 3].toInt() and 0xff)
  }

  @Test
  fun testWriteShort() {
    val header = ByteArray(44)
    val offset = 20
    val value: Short = 1

    audioRecorder.writeShort(header, offset, value)

    assertEquals(1, header[offset].toInt() and 0xff)
    assertEquals(0, header[offset + 1].toInt() and 0xff)
  }

  @Test
  fun testSaveAsWavFile() {
    val audioData = ByteArray(1024) { 0x55 } // Sample audio data
    val tempFile = File.createTempFile("test_audio", ".wav")
    tempFile.deleteOnExit()

    audioRecorder.saveAsWavFile(audioData, tempFile)

    // Read the file and verify its contents
    val fileBytes = tempFile.readBytes()
    assertTrue(fileBytes.size == 44 + audioData.size) // Header + audio data

    // Verify RIFF header
    assertEquals('R'.code.toByte(), fileBytes[0])
    assertEquals('I'.code.toByte(), fileBytes[1])
    assertEquals('F'.code.toByte(), fileBytes[2])
    assertEquals('F'.code.toByte(), fileBytes[3])

    // Verify WAVE header
    assertEquals('W'.code.toByte(), fileBytes[8])
    assertEquals('A'.code.toByte(), fileBytes[9])
    assertEquals('V'.code.toByte(), fileBytes[10])
    assertEquals('E'.code.toByte(), fileBytes[11])

    // Verify fmt sub-chunk
    assertEquals('f'.code.toByte(), fileBytes[12])
    assertEquals('m'.code.toByte(), fileBytes[13])
    assertEquals('t'.code.toByte(), fileBytes[14])
    assertEquals(' '.code.toByte(), fileBytes[15])

    // Verify data sub-chunk
    assertEquals('d'.code.toByte(), fileBytes[36])
    assertEquals('a'.code.toByte(), fileBytes[37])
    assertEquals('t'.code.toByte(), fileBytes[38])
    assertEquals('a'.code.toByte(), fileBytes[39])

    // Optionally, verify the audio data
    val recordedAudioData = fileBytes.sliceArray(44 until fileBytes.size)
    assertArrayEquals(audioData, recordedAudioData)
  }

  @Test(expected = SecurityException::class)
  fun testStartRecordingWithPermissionDenied() {
    // Mock permission check
    mockStatic(ActivityCompat::class.java).use { activityCompatMock ->
      activityCompatMock
          .`when`<Int> {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
          }
          .thenReturn(PackageManager.PERMISSION_DENIED)

      audioRecorder.startRecording()
    }
  }

  @Test
  fun testStopRecording() {
    // Mock AudioRecord
    val mockAudioRecord = mock<AudioRecord>()
    // Inject mockAudioRecord into audioRecorder
    val audioRecordField = AudioRecorder::class.java.getDeclaredField("audioRecord")
    audioRecordField.isAccessible = true
    audioRecordField.set(audioRecorder, mockAudioRecord)

    // Mock isRecordingAudio to true
    val isRecordingAudioField = AudioRecorder::class.java.getDeclaredField("isRecordingAudio")
    isRecordingAudioField.isAccessible = true
    isRecordingAudioField.setBoolean(audioRecorder, true)

    // Stop recording
    audioRecorder.stopRecording()

    // Verify that AudioRecord stopped and released
    verify(mockAudioRecord).stop()
    verify(mockAudioRecord).release()
  }

  @Test
  fun testSaveOfflineRecordingToFilesDir() {
    val mockFileDir = File("mockFileDir")
    whenever(context.filesDir).thenReturn(mockFileDir)

    val offlineRecorder = AudioRecorder(context, isOffline = true)
    offlineRecorder.startRecording()
    offlineRecorder.stopRecording()

    // Check if the audio file was saved in filesDir
    val audioFileField = AudioRecorder::class.java.getDeclaredField("audioFile")
    audioFileField.isAccessible = true
    val savedFile = audioFileField.get(offlineRecorder) as File

    assertTrue(savedFile.path.startsWith(mockFileDir.path))
  }

  @Test
  fun testSaveOnlineRecordingToCacheDir() {
    val mockCacheDir = File("mockCacheDir")
    whenever(context.cacheDir).thenReturn(mockCacheDir)

    val onlineRecorder = AudioRecorder(context, isOffline = false)
    onlineRecorder.startRecording()
    onlineRecorder.stopRecording()

    // Check if the audio file was saved in cacheDir
    val audioFileField = AudioRecorder::class.java.getDeclaredField("audioFile")
    audioFileField.isAccessible = true
    val savedFile = audioFileField.get(onlineRecorder) as File

    assertTrue(savedFile.path.startsWith(mockCacheDir.path))
  }

  @Test
  fun testFilenameIncludesTimestamp() {
    val recorder = AudioRecorder(context, isOffline = true)
    recorder.startRecording()
    recorder.stopRecording()

    val audioFileField = AudioRecorder::class.java.getDeclaredField("audioFile")
    audioFileField.isAccessible = true
    val savedFile = audioFileField.get(recorder) as File

    assertTrue(savedFile.name.matches(Regex("audio_record_\\d+\\.wav")))
  }

}
