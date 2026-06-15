package com.example.appmoneego.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.appmoneego.data.entity.Dompet
import com.example.appmoneego.data.entity.Transaksi
import java.io.File
import java.io.FileWriter
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportUtils {

    fun exportTransaksiToCsv(
        context: Context,
        transaksiList: List<Transaksi>,
        dompetList: List<Dompet>
    ): String? {
        // Timestamp unik agar file tidak tertimpa
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "transaksi_moneego_$timestamp.csv"

        val dompetMap = dompetList.associateBy { it.id }
        val csvContent = buildCsv(transaksiList, dompetMap)

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveCsvViaMediaStore(context, fileName, csvContent)
            } else {
                saveCsvLegacy(fileName, csvContent)
            }
            fileName
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun buildCsv(
        transaksiList: List<Transaksi>,
        dompetMap: Map<Int, Dompet>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("ID,Jenis,Kategori,Nominal,Dompet,Tanggal,Catatan")

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        transaksiList.forEach { t ->
            val namaDompet = dompetMap[t.dompetId]?.nama ?: "-"
            val tanggalStr = sdf.format(Date(t.tanggal))
            val catatan = t.catatan.replace("\"", "\"\"")
            sb.appendLine("${t.id},${t.jenis},${t.kategori},${t.nominal},$namaDompet,$tanggalStr,\"$catatan\"")
        }

        return sb.toString()
    }

    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.Q)
    private fun saveCsvViaMediaStore(context: Context, fileName: String, content: String) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            val outputStream: OutputStream? = resolver.openOutputStream(it)
            outputStream?.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
            }
            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(it, contentValues, null, null)
        }
    }

    private fun saveCsvLegacy(fileName: String, content: String) {
        val downloadDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        if (!downloadDir.exists()) downloadDir.mkdirs()
        val file = File(downloadDir, fileName)
        FileWriter(file).use { writer ->
            writer.write(content)
        }
    }
}