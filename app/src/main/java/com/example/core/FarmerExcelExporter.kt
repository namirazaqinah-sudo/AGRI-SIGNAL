package com.example.core

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.FarmerAccountEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Generates genuine Office Open XML (.xlsx) Excel workbooks of registered farmer accounts
 * and builds Android intents to email them directly to the admin address.
 */
object FarmerExcelExporter {

    const val TARGET_EMAIL = "namirazaqinah@gmail.com"
    const val XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    private const val EXPORT_FILENAME = "data_pendaftar_agri_signal.xlsx"

    fun exportToFile(context: Context, farmers: List<FarmerAccountEntity>): File {
        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val outputFile = File(exportDir, EXPORT_FILENAME)
        val bytes = buildWorkbookBytes(farmers)
        FileOutputStream(outputFile).use { stream ->
            stream.write(bytes)
            stream.flush()
        }
        return outputFile
    }

    fun launchEmailWithExcel(
        context: Context,
        excelFile: File,
        latestFarmer: FarmerAccountEntity,
        totalRegistered: Int
    ): Boolean {
        return runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                excelFile
            )
            val dateText = formatTimestamp(latestFarmer.createdAtMillis)
            val body = buildString {
                appendLine("Data Pendaftar Baru Aplikasi AGRI SIGNAL")
                appendLine()
                appendLine("Nama Lengkap   : ${latestFarmer.fullName}")
                appendLine("Nomor Handphone: ${latestFarmer.phoneNumber}")
                appendLine("Waktu Daftar   : $dateText")
                appendLine("Total Pendaftar: $totalRegistered akun")
                appendLine()
                append("Terlampir file Excel (.xlsx) berisi rekap seluruh data pendaftar.")
            }

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = XLSX_MIME_TYPE
                putExtra(Intent.EXTRA_EMAIL, arrayOf(TARGET_EMAIL))
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Data Pendaftar AGRI SIGNAL - ${latestFarmer.fullName}"
                )
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("Data Pendaftar AGRI SIGNAL", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(
                sendIntent,
                "Kirim Data Excel ke $TARGET_EMAIL"
            ).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(chooser)
            true
        }.getOrElse { false }
    }

    fun buildWorkbookBytes(farmers: List<FarmerAccountEntity>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos, StandardCharsets.UTF_8).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", contentTypesXml())
            writeZipEntry(zip, "_rels/.rels", rootRelsXml())
            writeZipEntry(zip, "xl/workbook.xml", workbookXml())
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml())
            writeZipEntry(zip, "xl/styles.xml", stylesXml())
            writeZipEntry(zip, "xl/worksheets/sheet1.xml", sheetXml(farmers))
        }
        return baos.toByteArray()
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    private fun sheetXml(farmers: List<FarmerAccountEntity>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("<cols>")
        sb.append("""<col min="1" max="1" width="8" customWidth="1"/>""")
        sb.append("""<col min="2" max="2" width="30" customWidth="1"/>""")
        sb.append("""<col min="3" max="3" width="22" customWidth="1"/>""")
        sb.append("""<col min="4" max="4" width="24" customWidth="1"/>""")
        sb.append("</cols>")
        sb.append("<sheetData>")

        // Header Row (row 1, style s="1")
        sb.append("""<row r="1">""")
        sb.append(inlineStringCell("A1", "No", styleIndex = 1))
        sb.append(inlineStringCell("B1", "Nama Lengkap", styleIndex = 1))
        sb.append(inlineStringCell("C1", "Nomor Handphone", styleIndex = 1))
        sb.append(inlineStringCell("D1", "Tanggal Daftar", styleIndex = 1))
        sb.append("</row>")

        farmers.forEachIndexed { idx, farmer ->
            val rowNum = idx + 2
            sb.append("""<row r="$rowNum">""")
            sb.append(numberCell("A$rowNum", idx + 1, styleIndex = 2))
            sb.append(inlineStringCell("B$rowNum", farmer.fullName, styleIndex = 2))
            sb.append(inlineStringCell("C$rowNum", farmer.phoneNumber, styleIndex = 2))
            sb.append(inlineStringCell("D$rowNum", formatTimestamp(farmer.createdAtMillis), styleIndex = 2))
            sb.append("</row>")
        }

        sb.append("</sheetData>")
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun inlineStringCell(ref: String, value: String, styleIndex: Int): String {
        val escaped = escapeXml(value)
        return """<c r="$ref" t="inlineStr" s="$styleIndex"><is><t>$escaped</t></is></c>"""
    }

    private fun numberCell(ref: String, value: Int, styleIndex: Int): String {
        return """<c r="$ref" s="$styleIndex"><v>$value</v></c>"""
    }

    private fun escapeXml(input: String): String {
        val cleaned = input.filter { ch ->
            ch == '\t' || ch == '\n' || ch == '\r' || ch.code >= 0x20
        }
        return buildString(cleaned.length) {
            cleaned.forEach { ch ->
                when (ch) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&apos;")
                    else -> append(ch)
                }
            }
        }
    }

    private fun formatTimestamp(millis: Long): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID"))
        return formatter.format(Date(millis))
    }

    private fun contentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private fun rootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Data Pendaftar" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

    private fun workbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun stylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><color rgb="FF1B241C"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF2E7D32"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFD0D7CE"/></left>
      <right style="thin"><color rgb="FFD0D7CE"/></right>
      <top style="thin"><color rgb="FFD0D7CE"/></top>
      <bottom style="thin"><color rgb="FFD0D7CE"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="3">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1"/>
  </cellXfs>
</styleSheet>"""
}
