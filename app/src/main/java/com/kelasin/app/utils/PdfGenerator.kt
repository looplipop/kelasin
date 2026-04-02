package com.kelasin.app.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.kelasin.app.data.entity.AbsensiEntity
import com.kelasin.app.data.entity.MahasiswaEntity
import com.kelasin.app.data.entity.MataKuliahEntity
import com.kelasin.app.data.entity.StatusAbsensi
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {
    fun generateAbsensiPdf(
        context: Context,
        mk: MataKuliahEntity,
        pertemuanKe: Int,
        mhsList: List<MahasiswaEntity>,
        absensiList: List<AbsensiEntity>
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val headerPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val pageWidth = pageInfo.pageWidth.toFloat()
        
        // Output Title
        canvas.drawText("Laporan Absensi Kelas", pageWidth / 2, 50f, titlePaint)
        
        paint.textSize = 12f
        canvas.drawText("Mata Kuliah: ${mk.nama} (${mk.kode})", 50f, 90f, paint)
        canvas.drawText("Dosen Pengampu: ${mk.dosen}", 50f, 110f, paint)
        canvas.drawText("Pertemuan Ke: $pertemuanKe", 50f, 130f, paint)
        val tgl = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())
        canvas.drawText("Tanggal Cetak: $tgl", 50f, 150f, paint)

        // Draw Table Header
        var currentY = 190f
        val colNo = 50f
        val colNama = 90f
        val colHadir = 320f
        val colAlpha = 380f
        val colSakit = 440f
        val colIzin = 500f

        canvas.drawText("No", colNo, currentY, headerPaint)
        canvas.drawText("Nama Mahasiswa", colNama, currentY, headerPaint)
        canvas.drawText("Hadir", colHadir, currentY, headerPaint)
        canvas.drawText("Alpha", colAlpha, currentY, headerPaint)
        canvas.drawText("Sakit", colSakit, currentY, headerPaint)
        canvas.drawText("Izin", colIzin, currentY, headerPaint)
        
        canvas.drawLine(50f, currentY + 10f, 545f, currentY + 10f, paint)
        currentY += 30f

        mhsList.forEachIndexed { i, mhs ->
            if (currentY > 780f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = 50f
            }

            val abs = absensiList.find { it.mahasiswaId == mhs.id }
            val status = abs?.status ?: StatusAbsensi.ALPHA

            canvas.drawText("${i + 1}", colNo, currentY, paint)
            canvas.drawText(mhs.nama.take(35), colNama, currentY, paint)
            
            canvas.drawText(if (status == StatusAbsensi.HADIR) "✓" else "-", colHadir + 10f, currentY, paint)
            canvas.drawText(if (status == StatusAbsensi.ALPHA) "✓" else "-", colAlpha + 10f, currentY, paint)
            canvas.drawText(if (status == StatusAbsensi.SAKIT) "✓" else "-", colSakit + 10f, currentY, paint)
            canvas.drawText(if (status == StatusAbsensi.IZIN) "✓" else "-", colIzin + 10f, currentY, paint)

            canvas.drawLine(50f, currentY + 10f, 545f, currentY + 10f, paint)
            currentY += 25f
        }

        pdfDocument.finishPage(page)

        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "KelasinPDF")
        if (!dir.exists()) dir.mkdirs()
        
        val file = File(dir, "Absensi_${mk.kode}_PertLke${pertemuanKe}.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            pdfDocument.close()
        }
    }
}
