package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.model.Warga
import com.example.data.model.MutasiWarga
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileWriteResult(
    val file: File?,
    val uri: Uri?,
    val filename: String
)

object PdfReportGenerator {
    fun generateReportPdf(
        context: Context,
        rt: String,
        rw: String,
        period: String,
        activeWarga: List<Warga>,
        mutasiList: List<MutasiWarga>
    ): FileWriteResult {
        val pdfDocument = PdfDocument()
        
        // Tall canvas (1100 pts) for clean, high-density listing
        val pageInfo = PdfDocument.PageInfo.Builder(595, 1100, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        // White Background Fill
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 595f, 1100f, bgPaint)
        
        // Statistics
        val total = activeWarga.size
        val totalKk = activeWarga.map { it.noKk }.distinct().size
        val laki = activeWarga.count { it.gender == "Laki-laki" }
        val perempuan = activeWarga.count { it.gender == "Perempuan" }
        
        // Exact 8 classifications requested by developer
        val ageBalita = activeWarga.count { it.umur in 0..5 }
        val ageAnak = activeWarga.count { it.umur in 6..11 }
        val ageRemaja = activeWarga.count { it.umur in 12..25 }
        val ageDewasa = activeWarga.count { it.umur in 26..45 }
        val agePraLansia = activeWarga.count { it.umur in 46..59 }
        val ageLansiaMuda = activeWarga.count { it.umur in 60..69 }
        val ageLansiaMadya = activeWarga.count { it.umur in 70..79 }
        val ageLansiaParipurna = activeWarga.count { it.umur >= 80 }
        
        // Disability population
        val disabilitas = activeWarga.filter { it.isDisabilitas }
        
        // Mutations counts
        val mutLahir = mutasiList.count { it.tipe == "Kelahiran" }
        val mutWafat = mutasiList.count { it.tipe == "Kematian" }
        val mutPindahan = mutasiList.count { it.tipe in listOf("Pendatang", "Pindah Keluar") }
        
        var currentY = 55f
        
        // Kop Surat Header
        val titlePaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("PEMERINTAH KELURAHAN SEJAHTERA", 297.5f, currentY, titlePaint)
        currentY += 18f
        
        val subTitlePaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("KANTOR RUKUN TETANGGA (RT-$rt) / RUKUN WARGA (RW-$rw)", 297.5f, currentY, subTitlePaint)
        currentY += 14f
        
        val sysPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Sistem Informasi Kependudukan Desa (SIKD) Digital", 297.5f, currentY, sysPaint)
        currentY += 12f
        
        // Thin double Indonesian KOP line
        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 2.5f
        }
        canvas.drawLine(40f, currentY, 555f, currentY, linePaint)
        currentY += 4f
        linePaint.strokeWidth = 0.8f
        canvas.drawLine(40f, currentY, 555f, currentY, linePaint)
        currentY += 24f
        
        // Report title
        val reportTitlePaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("LAPORAN BULANAN REKAPITULASI DATA PERKEMBANGAN MANDIRI KEPENDUDUKAN", 297.5f, currentY, reportTitlePaint)
        currentY += 16f
        
        val reportPeriodPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Periode : $period | Diunduh: ${SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())}", 297.5f, currentY, reportPeriodPaint)
        currentY += 28f
        
        // Section header I
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("I. RINGKASAN DATA UMUM PENDUDUK", 40f, currentY, headerPaint)
        currentY += 10f
        
        val normalPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        
        val tableWidth = 515f
        val colWidth = tableWidth / 2f
        
        fun drawTableRow(lbl1: String, val1: String, lbl2: String, val2: String) {
            val cellPaint = Paint().apply {
                color = Color.LTGRAY
                style = Paint.Style.STROKE
                strokeWidth = 0.4f
            }
            canvas.drawRect(40f, currentY, 40f + colWidth, currentY + 20f, cellPaint)
            canvas.drawRect(40f + colWidth, currentY, 40f + tableWidth, currentY + 20f, cellPaint)
            
            canvas.drawText(lbl1, 48f, currentY + 13f, normalPaint)
            canvas.drawText(val1, 40f + colWidth - 80f, currentY + 13f, Paint(normalPaint).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
            
            canvas.drawText(lbl2, 48f + colWidth, currentY + 13f, normalPaint)
            canvas.drawText(val2, 40f + tableWidth - 80f, currentY + 13f, Paint(normalPaint).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
            
            currentY += 20f
        }
        
        drawTableRow("Jumlah Penduduk Aktif:", "$total Jiwa", "Jumlah Kepala Keluarga:", "$totalKk KK")
        val lPct = if (total > 0) (laki.toFloat() / total * 100) else 0f
        val pPct = if (total > 0) (perempuan.toFloat() / total * 100) else 0f
        drawTableRow("Laki-Laki:", "$laki (${String.format(Locale.getDefault(), "%.1f", lPct)}%)", "Perempuan:", "$perempuan (${String.format(Locale.getDefault(), "%.1f", pPct)}%)")
        
        currentY += 22f
        
        // Section header II
        canvas.drawText("II. DISTRIBUSI KELOMPOK UMUR DETIL", 40f, currentY, headerPaint)
        currentY += 12f
        
        val ageCols = listOf(35f, 160f, 185f, 65f, 70f)
        val agePositions = mutableListOf<Float>()
        var curX = 40f
        ageCols.forEach {
            agePositions.add(curX)
            curX += it
        }
        
        val thBgPaint = Paint().apply {
            color = 0xFFECEFF1.toInt()
            style = Paint.Style.FILL
        }
        val defBorderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        
        canvas.drawRect(40f, currentY, 555f, currentY + 20f, thBgPaint)
        canvas.drawRect(40f, currentY, 555f, currentY + 20f, defBorderPaint)
        for (i in 1 until agePositions.size) {
            canvas.drawLine(agePositions[i], currentY, agePositions[i], currentY + 20f, defBorderPaint)
        }
        
        val thPaint = Paint().apply {
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        canvas.drawText("No", agePositions[0] + 5f, currentY + 13f, thPaint)
        canvas.drawText("Rentang Umur", agePositions[1] + 8f, currentY + 13f, thPaint)
        canvas.drawText("Klasifikasi Teknis", agePositions[2] + 8f, currentY + 13f, thPaint)
        canvas.drawText("Jumlah", agePositions[3] + 8f, currentY + 13f, thPaint)
        canvas.drawText("Persen", agePositions[4] + 8f, currentY + 13f, thPaint)
        currentY += 20f
        
        fun drawAgeRow(no: String, range: String, cls: String, count: Int) {
            canvas.drawRect(40f, currentY, 555f, currentY + 18f, defBorderPaint)
            for (i in 1 until agePositions.size) {
                canvas.drawLine(agePositions[i], currentY, agePositions[i], currentY + 18f, defBorderPaint)
            }
            
            val rText = Paint().apply {
                color = Color.BLACK
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val pct = if (total > 0) (count.toFloat() / total * 100) else 0f
            
            canvas.drawText(no, agePositions[0] + 5f, currentY + 12f, rText)
            canvas.drawText(range, agePositions[1] + 8f, currentY + 12f, rText)
            canvas.drawText(cls, agePositions[2] + 8f, currentY + 12f, rText)
            canvas.drawText("$count Jiwa", agePositions[3] + 8f, currentY + 12f, Paint(rText).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
            canvas.drawText("${String.format(Locale.getDefault(), "%.1f", pct)}%", agePositions[4] + 8f, currentY + 12f, rText)
            currentY += 18f
        }
        
        drawAgeRow("1", "0 - 5 Tahun", "Balita (Bawah Lima Tahun)", ageBalita)
        drawAgeRow("2", "5 - 11 Tahun", "Anak-anak", ageAnak)
        drawAgeRow("3", "12 - 25 Tahun", "Remaja", ageRemaja)
        drawAgeRow("4", "26 - 45 Tahun", "Dewasa", ageDewasa)
        drawAgeRow("5", "45 - 59 Tahun", "Pra-Lansia (Lansia Dini)", agePraLansia)
        drawAgeRow("6", "60 - 69 Tahun", "Lansia Muda", ageLansiaMuda)
        drawAgeRow("7", "70 - 79 Tahun", "Lansia Madya", ageLansiaMadya)
        drawAgeRow("8", "80 Tahun ke Atas", "Lansia Paripurna (Sangat Tua)", ageLansiaParipurna)
        
        currentY += 22f
        
        // Section header III
        canvas.drawText("III. REKAPITULASI PENYANDANG DISABILITAS", 40f, currentY, headerPaint)
        currentY += 12f
        
        val pinkBg = Paint().apply {
            color = 0xFFFFF0F2.toInt()
            style = Paint.Style.FILL
        }
        val pinkBorder = Paint().apply {
            color = 0xFFD81B60.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        canvas.drawRect(40f, currentY, 555f, currentY + 60f, pinkBg)
        canvas.drawRect(40f, currentY, 555f, currentY + 60f, pinkBorder)
        
        val pinkText = Paint().apply {
            color = 0xFF880E4F.toInt()
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("• Total Penyandang Disabilitas di Wilayah:", 50f, currentY + 18f, pinkText)
        canvas.drawText("${disabilitas.size} Jiwa Penduduk Aktif", 280f, currentY + 18f, Paint(pinkText).apply { color = Color.BLACK })
        
        val disStatement = if (disabilitas.isNotEmpty()) {
            val mapDis = disabilitas.groupBy { it.jenisDisabilitas.trim().lowercase().replaceFirstChar { c -> c.uppercase() }.ifEmpty { "Umum" } }
            mapDis.entries.joinToString(", ") { "${it.key}: ${it.value.size} Warga" }
        } else {
            "Tidak ada rincian rekap warga disabilitas yang tercatat aktif dalam basis data SIKD saat ini."
        }
        
        canvas.drawText("• Rincian Kategori Terdata:", 50f, currentY + 36f, Paint(pinkText).apply { textSize = 9f })
        
        val descPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        if (disStatement.length > 76) {
            canvas.drawText(disStatement.substring(0, 76) + "...", 280f, currentY + 36f, descPaint)
            val remainderStr = if (disStatement.length > 150) disStatement.substring(76, 150) else disStatement.substring(76)
            canvas.drawText(remainderStr, 280f, currentY + 48f, descPaint)
        } else {
            canvas.drawText(disStatement, 280f, currentY + 36f, descPaint)
        }
        
        currentY += 78f
        
        // Section header IV
        canvas.drawText("IV. AKTIVITAS PERUBAHAN MUTASI BULAN INI", 40f, currentY, headerPaint)
        currentY += 12f
        
        val bW = tableWidth / 3f
        fun drawMutBox(lbl: String, count: Int, icon: String, startX: Float) {
            val innerBg = Paint().apply {
                color = 0xFFF9FBFD.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawRect(startX, currentY, startX + bW - 10f, currentY + 38f, innerBg)
            canvas.drawRect(startX, currentY, startX + bW - 10f, currentY + 38f, defBorderPaint)
            
            canvas.drawText("$icon $lbl", startX + 8f, currentY + 15f, Paint(normalPaint).apply { textSize = 8.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
            canvas.drawText("$count Orang", startX + 8f, currentY + 30f, Paint(normalPaint).apply { textSize = 10.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = 0xFF0D47A1.toInt() })
        }
        
        drawMutBox("Kelahiran", mutLahir, "👶", 40f)
        drawMutBox("Kematian", mutWafat, "✝️", 40f + bW)
        drawMutBox("Mutasi Pindahan", (mutLahir + mutWafat + mutPindahan), "✈️", 40f + bW * 2)
        
        currentY += 65f
        
        // Signature
        val sY = currentY
        val signText = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Petugas Pembuat,", 40f, sY, signText)
        canvas.drawText("Sistem Aplikasi SIKD Kelurahan", 40f, sY + 14f, Paint(signText).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
        canvas.drawText("Operator SIKD RT $rt", 40f, sY + 62f, signText)
        canvas.drawLine(40f, sY + 74f, 150f, sY + 74f, Paint().apply { color = Color.BLACK; strokeWidth = 0.8f })
        
        canvas.drawText("Mengetahui,", 400f, sY, signText)
        canvas.drawText("Ketua Rukun Tetangga (RT-$rt)", 400f, sY + 14f, Paint(signText).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
        canvas.drawText("Ketua RW $rw", 400f, sY + 62f, signText)
        canvas.drawLine(400f, sY + 74f, 530f, sY + 74f, Paint().apply { color = Color.BLACK; strokeWidth = 0.8f })
        
        // footer copyright text
        val footnoteTextPaint = Paint().apply {
            color = Color.GRAY
            textSize = 7.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Berkas dicetak terenkripsi secara otomatis melalui aplikasi dashboard RT/RW SIKD.", 297.5f, 1045f, footnoteTextPaint)
        canvas.drawText("Hak Cipta © 2026 Pemerintah Kelurahan Sejahtera Mandiri.", 297.5f, 1060f, footnoteTextPaint)
        
        pdfDocument.finishPage(page)
        
        val filename = "Laporan_Kependudukan_RT_${rt}_RW_${rw}_${period.replace(" ", "_")}.pdf"
        var createdFile: File? = null
        var createdUri: Uri? = null
        
        try {
            // Write to local files dir
            val downloadsFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsFolder, filename)
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            createdFile = file
            createdUri = Uri.fromFile(file)
            
            // Push to public MediaStore device standard downloads directory
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val destinationUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (destinationUri != null) {
                    resolver.openOutputStream(destinationUri)?.use { out ->
                        pdfDocument.writeTo(out)
                    }
                    createdUri = destinationUri
                }
            } else {
                val standardDownloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (standardDownloadsPath.exists() || standardDownloadsPath.mkdirs()) {
                    val publicTargetFile = File(standardDownloadsPath, filename)
                    FileOutputStream(publicTargetFile).use { out ->
                        pdfDocument.writeTo(out)
                    }
                    createdFile = publicTargetFile
                    createdUri = Uri.fromFile(publicTargetFile)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
        
        return FileWriteResult(createdFile, createdUri, filename)
    }
}
