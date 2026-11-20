package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AGRI SIGNAL", appName)
    val siteKeyId = context.resources.getIdentifier("recaptcha_site_key", "string", context.packageName)
    org.junit.Assert.assertTrue(siteKeyId != 0)
    org.junit.Assert.assertTrue(context.getString(siteKeyId).isNotBlank())
  }

  @Test
  fun `leafDiagnosisService parses diseased and healthy json`() {
    val service = com.example.data.remote.LeafDiagnosisService()
    val diseasedJson = """
        {
          "plantName": "Cabai",
          "isHealthy": false,
          "diseaseName": "Bercak Daun Cercospora",
          "confidencePercent": 92,
          "cause": "Daun kurang sehat dengan bercak cokelat bermata pucat akibat jamur Cercospora capsici.",
          "treatmentSteps": ["Pangkas daun terinfeksi", "Semprot fungisida merata"],
          "recommendedProducts": ["Antracol 70 WP (Propineb)", "Amistartop 325 SC"],
          "organicSolutions": ["Semprot ekstrak bawang putih"],
          "preventionTips": ["Jaga jarak tanam"]
        }
    """.trimIndent()

    val result = service.parseDiagnosisResponse(diseasedJson)
    assertEquals("Cabai", result.plantName)
    assertEquals(false, result.isHealthy)
    assertEquals("Bercak Daun Cercospora", result.diseaseName)
    assertEquals(92, result.confidencePercent)
    assertEquals(2, result.treatmentSteps.size)
    assertEquals("Antracol 70 WP (Propineb)", result.recommendedProducts.first())
  }

  @Test
  fun `registrationEmailDispatcher builds resend payload with xlsx attachment`() {
    val farmer = com.example.data.local.FarmerAccountEntity(
      phoneNumber = "081234567890",
      fullName = "Pak Budi",
      createdAtMillis = 1700000000000L
    )
    val xlsxBytes = com.example.core.FarmerExcelExporter.buildWorkbookBytes(listOf(farmer))
    val payload = com.example.core.RegistrationEmailDispatcher.buildResendPayloadJson(
      xlsxBytes = xlsxBytes,
      latestFarmer = farmer,
      totalRegistered = 1
    )

    assertEquals("namirazaqinah@gmail.com", payload.getJSONArray("to").getString(0))
    assertEquals("Data Pendaftar Baru AGRI SIGNAL - Pak Budi", payload.getString("subject"))
    val attachment = payload.getJSONArray("attachments").getJSONObject(0)
    assertEquals("data_pendaftar_agri_signal.xlsx", attachment.getString("filename"))
    org.junit.Assert.assertTrue(attachment.getString("content").isNotBlank())
  }

  @Test
  fun `leafDiagnosisService detects rate limit and transient errors for fallback`() {
    val service = com.example.data.remote.LeafDiagnosisService()
    org.junit.Assert.assertTrue(
      service.isRateLimitOrTransient(IllegalStateException("HTTP 429 RESOURCE_EXHAUSTED: Quota exceeded"))
    )
    org.junit.Assert.assertTrue(
      service.isRateLimitOrTransient(RuntimeException("wrapper", IllegalStateException("503 Service Unavailable")))
    )
    org.junit.Assert.assertFalse(
      service.isRateLimitOrTransient(IllegalArgumentException("Unsupported image format"))
    )
    org.junit.Assert.assertTrue(
      service.isAppCheckOrAuthError(IllegalStateException("HTTP 403 App Check token is invalid"))
    )
    org.junit.Assert.assertFalse(
      service.isAppCheckOrAuthError(IllegalStateException("HTTP 429 Quota exceeded"))
    )
  }
}
