package com.example.indianbloom.data.nfc

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import java.nio.charset.Charset

object NfcHelper {

    /**
     * Converts a byte array to its hex string representation.
     */
    fun bytesToHex(bytes: ByteArray?): String? {
        if (bytes == null) return null
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Extracts the hardware UID from an NFC intent.
     */
    fun extractUid(intent: Intent): String? {
        val rawId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)
        return bytesToHex(rawId)
    }

    /**
     * Extracts the first NDEF text record payload from an NFC intent.
     */
    fun extractNdefPayload(intent: Intent): String? {
        val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES) ?: return null
        if (rawMsgs.isEmpty()) return null

        val ndefMessage = rawMsgs[0] as NdefMessage
        val records = ndefMessage.records ?: return null
        if (records.isEmpty()) return null

        val record = records[0]
        val payload = record.payload ?: return null

        // NDEF Text Record format contains status byte at offset 0
        // Language code length is in bit 5..0 of status byte
        if (payload.size < 2) return null
        val status = payload[0].toInt()
        val languageCodeLength = status and 0x3F
        if (payload.size <= 1 + languageCodeLength) return null

        return String(
            payload,
            1 + languageCodeLength,
            payload.size - 1 - languageCodeLength,
            Charset.forName("UTF-8")
        )
    }

    /**
     * Writes the signature to the NFC Tag's NDEF storage.
     */
    fun writeNdefPayload(intent: Intent, payloadText: String): Boolean {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return false
        val ndef = Ndef.get(tag) ?: return false

        try {
            ndef.connect()
            if (!ndef.isWritable) return false

            // Create NDEF Text Record
            val langBytes = "en".toByteArray(Charset.forName("US-ASCII"))
            val textBytes = payloadText.toByteArray(Charset.forName("UTF-8"))
            val payload = ByteArray(1 + langBytes.size + textBytes.size)

            payload[0] = langBytes.size.toByte() // Status byte (UTF-8 encoding & lang length)
            System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
            System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)

            val record = NdefRecord(
                NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT,
                ByteArray(0),
                payload
            )

            val message = NdefMessage(arrayOf(record))
            
            if (ndef.maxSize < message.byteArrayLength) return false
            ndef.writeNdefMessage(message)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try {
                ndef.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
