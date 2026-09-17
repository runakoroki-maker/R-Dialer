package com.example.util

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

data class DeviceEnvironmentInfo(
    val isEmulator: Boolean,
    val environmentName: String,
    val callMode: String,
    val telecomStatus: String,
    val cellularStatus: String,
    val matchedIndicators: List<String>,
    val deviceModel: String,
    val manufacturer: String,
    val hardware: String,
    val product: String,
    val fingerprint: String
)

object EmulatorDetector {

    // Testing override hook (for unit tests / manual diagnostics toggling)
    private var testOverride: Boolean? = null

    private val _environmentInfo = MutableStateFlow(evaluateEnvironment(null))
    val environmentInfo: StateFlow<DeviceEnvironmentInfo> = _environmentInfo.asStateFlow()

    fun setTestOverride(isEmulator: Boolean?) {
        testOverride = isEmulator
        _environmentInfo.value = evaluateEnvironment(null)
    }

    fun isEmulator(): Boolean {
        return _environmentInfo.value.isEmulator
    }

    fun refresh(context: Context? = null): DeviceEnvironmentInfo {
        val result = evaluateEnvironment(context)
        _environmentInfo.value = result
        return result
    }

    private fun evaluateEnvironment(context: Context?): DeviceEnvironmentInfo {
        val fingerprint = Build.FINGERPRINT.orEmpty()
        val model = Build.MODEL.orEmpty()
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val hardware = Build.HARDWARE.orEmpty()
        val product = Build.PRODUCT.orEmpty()
        val board = Build.BOARD.orEmpty()
        val brand = Build.BRAND.orEmpty()
        val device = Build.DEVICE.orEmpty()

        val matchedIndicators = mutableListOf<String>()

        // 1. Check Build Fingerprint
        val fpLower = fingerprint.lowercase(Locale.ROOT)
        if (fpLower.startsWith("generic") || fpLower.startsWith("unknown") ||
            fpLower.contains("google_sdk") || fpLower.contains("emulator") ||
            fpLower.contains("android sdk built for") || fpLower.contains("vbox86p") ||
            fpLower.contains("genymotion") || fpLower.contains("robolectric")
        ) {
            matchedIndicators.add("Fingerprint: $fingerprint")
        }

        // 2. Check Build Model
        val modelLower = model.lowercase(Locale.ROOT)
        if (modelLower.contains("google_sdk") || modelLower.contains("emulator") ||
            modelLower.contains("android sdk built for") || modelLower.contains("sdk_gphone") ||
            modelLower.contains("simulator") || modelLower.contains("virtualbox")
        ) {
            matchedIndicators.add("Model: $model")
        }

        // 3. Check Hardware
        val hwLower = hardware.lowercase(Locale.ROOT)
        if (hwLower.contains("goldfish") || hwLower.contains("ranchu") ||
            hwLower.contains("vbox86") || hwLower.contains("qemu")
        ) {
            matchedIndicators.add("Hardware: $hardware")
        }

        // 4. Check Product
        val prodLower = product.lowercase(Locale.ROOT)
        if (prodLower.contains("sdk") || prodLower.contains("google_sdk") ||
            prodLower.contains("sdk_gphone") || prodLower.contains("sdk_x86") ||
            prodLower.contains("vbox86p") || prodLower.contains("emulator") ||
            prodLower.contains("simulator")
        ) {
            matchedIndicators.add("Product: $product")
        }

        // 5. Check Board
        val boardLower = board.lowercase(Locale.ROOT)
        if (boardLower.contains("goldfish") || boardLower.contains("ranchu")) {
            matchedIndicators.add("Board: $board")
        }

        // 6. Check Manufacturer
        val mfgLower = manufacturer.lowercase(Locale.ROOT)
        if (mfgLower.contains("genymotion") || (mfgLower.contains("unknown") && modelLower.contains("emulator"))) {
            matchedIndicators.add("Manufacturer: $manufacturer")
        }

        // 7. Check Brand & Device generic combination
        val brandLower = brand.lowercase(Locale.ROOT)
        val devLower = device.lowercase(Locale.ROOT)
        if (brandLower.startsWith("generic") && devLower.startsWith("generic")) {
            matchedIndicators.add("Generic Brand/Device: $brand / $device")
        }

        // 8. Check Virtualization / QEMU Device Pipes
        try {
            if (File("/dev/socket/qemud").exists() || File("/dev/qemu_pipe").exists()) {
                matchedIndicators.add("QEMU Virtualization pipe detected")
            }
        } catch (_: Exception) {
            // Permission or security exception ignored
        }

        // 9. Check Telephony operator if available (AVD default operator "310260" or "Android")
        if (context != null) {
            try {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                val opName = tm?.networkOperatorName.orEmpty()
                val opCode = tm?.networkOperator.orEmpty()
                if (opName.equals("Android", ignoreCase = true) || opCode == "310260") {
                    matchedIndicators.add("Emulator Test Operator: $opName ($opCode)")
                }
            } catch (_: Exception) {
                // Ignore telephony check failure
            }
        }

        // Evaluate boolean status: requires at least one definitive emulator indicator
        val detectedAsEmulator = testOverride ?: matchedIndicators.isNotEmpty()

        return if (detectedAsEmulator) {
            DeviceEnvironmentInfo(
                isEmulator = true,
                environmentName = "Android Emulator",
                callMode = "Demo",
                telecomStatus = "Emulated / Limited",
                cellularStatus = "Not verified / Simulated",
                matchedIndicators = matchedIndicators,
                deviceModel = model.ifBlank { "Emulator Device" },
                manufacturer = manufacturer.ifBlank { "Android AVD" },
                hardware = hardware.ifBlank { "Virtual QEMU" },
                product = product.ifBlank { "Virtual SDK" },
                fingerprint = fingerprint.ifBlank { "Generic Emulator" }
            )
        } else {
            DeviceEnvironmentInfo(
                isEmulator = false,
                environmentName = "Physical Android Device",
                callMode = "Real Device",
                telecomStatus = "Android Telecom Framework",
                cellularStatus = "Available / Not verified",
                matchedIndicators = emptyList(),
                deviceModel = model.ifBlank { "Android Phone" },
                manufacturer = manufacturer.ifBlank { "Android OEM" },
                hardware = hardware.ifBlank { "Physical SOC" },
                product = product.ifBlank { "Android Device" },
                fingerprint = fingerprint.ifBlank { "Signed ROM" }
            )
        }
    }
}
