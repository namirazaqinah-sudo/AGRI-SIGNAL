package com.example

import com.example.core.FarmerExcelExporter
import com.example.core.PhoneValidator
import com.example.data.local.FarmerAccountEntity
import com.example.data.remote.WeatherCodeMapper
import com.example.data.remote.WeatherVisualKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

class ExampleUnitTest {
    @Test
    fun phoneValidator_acceptsIndonesianFormats() {
        assertNull(PhoneValidator.validateName("Budi Santoso"))
        assertNull(PhoneValidator.validatePhone("081234567890"))
        assertNull(PhoneValidator.validatePhone("+62 812-3456-7890"))
        assertEquals("081234567890", PhoneValidator.normalizePhone("+62 812-3456-7890"))
    }

    @Test
    fun phoneValidator_rejectsInvalidInputs() {
        assertNotNull(PhoneValidator.validateName("   "))
        assertNotNull(PhoneValidator.validatePhone("12345"))
        assertNotNull(PhoneValidator.validatePhone("0812abc"))
    }

    @Test
    fun weatherCodeMapper_mapsIndonesianWeather() {
        val sunny = WeatherCodeMapper.map(0, isDay = true)
        assertEquals("Cerah", sunny.label)
        assertEquals(WeatherVisualKind.SUNNY, sunny.visualKind)

        val rain = WeatherCodeMapper.map(63, isDay = true)
        assertEquals("Hujan Ringan - Sedang", rain.label)
        assertEquals(WeatherVisualKind.RAIN, rain.visualKind)
    }

    @Test
    fun farmerExcelExporter_buildsValidXlsxArchive() {
        val farmers = listOf(
            FarmerAccountEntity(
                phoneNumber = "081234567890",
                fullName = "Pak Budi & Ibu Sari",
                createdAtMillis = 1700000000000L
            )
        )
        val bytes = FarmerExcelExporter.buildWorkbookBytes(farmers)
        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = String(zis.readBytes(), StandardCharsets.UTF_8)
                entry = zis.nextEntry
            }
        }

        assertTrue(entries.containsKey("[Content_Types].xml"))
        assertTrue(entries.containsKey("xl/workbook.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
        val sheet = entries.getValue("xl/worksheets/sheet1.xml")
        assertTrue(sheet.contains("Pak Budi &amp; Ibu Sari"))
        assertTrue(sheet.contains("081234567890"))
        assertEquals("namirazaqinah@gmail.com", FarmerExcelExporter.TARGET_EMAIL)
    }
}
