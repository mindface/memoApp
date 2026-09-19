package com.example.memoapp

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Static security audit that scans the codebase for anomalies.
 * Fails the build if unauthorized URLs or high-risk API patterns are found.
 */
class SecurityAuditTest {

    // List of domains allowed for production communication.
    // Any URL found in the source code not matching these will trigger a failure.
    private val URL_WHITELIST = listOf(
        "google.com",
        "firebaseio.com",
        "firebase.google.com",
        "github.com",
        "android.com",
        "schemas.android.com",
        "example.com" // Local dev
    )

    private val PROJECT_ROOT: File by lazy {
        // Try to find src/main from current directory
        var current = File(".").absoluteFile
        while (current != null) {
            val srcMain = File(current, "src/main")
            if (srcMain.exists()) return@lazy srcMain
            
            // Check if we are in the module dir
            val appSrcMain = File(current, "app/src/main")
            if (appSrcMain.exists()) return@lazy appSrcMain
            
            current = current.parentFile
        }
        File("src/main") // Fallback
    }

    @Test
    fun auditSourceCodeForAnomalies() {
        val anomalies = mutableListOf<String>()
        
        if (!PROJECT_ROOT.exists()) {
            println("Skipping audit: Project root not found at ${PROJECT_ROOT.absolutePath}")
            return
        }

        PROJECT_ROOT.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java" || it.extension == "xml") }
            .forEach { file ->
                val content = file.readText()
                if (content.contains("@Suppress(\"SecurityAnomalies\")")) return@forEach

                // 1. Detect suspicious URLs
                findSuspiciousUrls(content, file, anomalies)

                // 2. Detect Reflection (unless it's in a library or specifically allowed)
                if (content.contains("java.lang.reflect") || content.contains(".getDeclaredMethod")) {
                    anomalies.add("Reflection detected in ${file.name}: Potential hidden API access.")
                }

                // 3. Detect Shell/Process execution
                if (content.contains("Runtime.getRuntime().exec") || content.contains("ProcessBuilder")) {
                    anomalies.add("Shell execution detected in ${file.name}: Potential backdoor command execution.")
                }
            }

        if (anomalies.isNotEmpty()) {
            val report = anomalies.joinToString("\n")
            println("Security Audit Failed:\n$report")
            assertTrue("Security anomalies found in codebase. Check console for details.", anomalies.isEmpty())
        }
    }

    private fun findSuspiciousUrls(content: String, file: File, anomalies: MutableList<String>) {
        val urlRegex = Regex("https?://[a-zA-Z0-9./?=%&_-]+")
        urlRegex.findAll(content).forEach { match ->
            val url = match.value
            val isWhitelisted = URL_WHITELIST.any { url.contains(it) }
            
            if (!isWhitelisted) {
                anomalies.add("Unauthorized URL found in ${file.name}: $url")
            }
        }
    }
}
