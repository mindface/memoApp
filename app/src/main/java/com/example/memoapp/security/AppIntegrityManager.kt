package com.example.memoapp.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import java.security.MessageDigest

/**
 * Manages the runtime integrity of the application.
 * Detects tampering, unauthorized install sources, and potential backdoors.
 */
object AppIntegrityManager {
    private const val TAG = "AppIntegrity"

    private fun isDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    // SHA-256 Hash of the legitimate signing certificate (Placeholder for engineering audit)
    // Engineers should replace this with the actual release certificate hash.
    private const val VALID_SIGNATURE_HASH = "DEBUG_SIGNATURE_PLACEHOLDER"

    // Trusted installers list
    private val TRUSTED_INSTALLERS = listOf(
        "com.android.vending", // Google Play
        "com.amazon.venezia", // Amazon Appstore
        "com.sec.android.app.samsungapps", // Samsung Galaxy Store
    )

    /**
     * Performs a comprehensive integrity check.
     * Returns true if the environment appears authentic and safe.
     */
    fun checkIntegrity(context: Context): Boolean {
        val isSignatureValid = verifySignature(context)
        val isInstallerTrusted = verifyInstaller(context)
        
        Log.d(TAG, "Integrity Report -> Signature: $isSignatureValid, Installer: $isInstallerTrusted")
        
        // In local debug builds, we may want to bypass installer checks
        if (isDebuggable(context)) return isSignatureValid
        
        return isSignatureValid && isInstallerTrusted
    }

    private fun verifySignature(context: Context): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.signingCertificateHistory
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures != null) {
                for (sig in signatures) {
                    val currentHash = computeSha256(sig.toByteArray())
                    Log.d(TAG, "Current Signature Hash: $currentHash")
                    
                    // For development, we log it. In production, compare with VALID_SIGNATURE_HASH.
                    if (VALID_SIGNATURE_HASH != "DEBUG_SIGNATURE_PLACEHOLDER") {
                        if (currentHash != VALID_SIGNATURE_HASH) return false
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Signature verification failed", e)
            false
        }
    }

    private fun verifyInstaller(context: Context): Boolean {
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
            
            // Allow local ADB installs in debug mode
            if (installer == null) return isDebuggable(context)
            
            TRUSTED_INSTALLERS.contains(installer)
        } catch (e: Exception) {
            false
        }
    }

    private fun computeSha256(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(data)
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}
