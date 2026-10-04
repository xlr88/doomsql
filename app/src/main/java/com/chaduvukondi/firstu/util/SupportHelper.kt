package com.chaduvukondi.firstu.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.browser.customtabs.CustomTabsIntent
import com.google.android.play.core.review.ReviewManagerFactory
import com.chaduvukondi.firstu.BuildConfig
import com.chaduvukondi.firstu.config.AppLinks

object SupportHelper {

    fun shareApp(context: Context, onNoAppFound: () -> Unit) {
        try {
            val shareText = "I'm practicing SQL interview questions on DoomSQL — Easy to Hard, works offline. Try it: ${AppLinks.PLAY_STORE_URL}"
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            val chooser = Intent.createChooser(sendIntent, "Share DoomSQL").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            onNoAppFound()
        }
    }

    fun openCustomTabOrBrowser(
        context: Context,
        url: String,
        onNoAppFound: () -> Unit = {}
    ) {
        try {
            val uri = Uri.parse(url)
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(context, uri)
        } catch (e: Exception) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
            } catch (e2: Exception) {
                onNoAppFound()
            }
        }
    }

    fun openPlayStoreListing(
        context: Context,
        onNoAppFound: (() -> Unit)? = null
    ) {
        val packageName = context.packageName
        val marketUri = Uri.parse("market://details?id=$packageName")
        val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")

        // 1. Try launching Google Play Store app directly
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, marketUri).apply {
                setPackage("com.android.vending")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(marketIntent)
            return
        } catch (_: Exception) {}

        // 2. Try generic market intent
        try {
            val genericIntent = Intent(Intent.ACTION_VIEW, marketUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(genericIntent)
            return
        } catch (_: Exception) {}

        // 3. Fallback to web browser
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        } catch (e: Exception) {
            onNoAppFound?.invoke()
        }
    }

    fun requestInAppReview(
        activity: Activity,
        onFallbackToPlayStore: () -> Unit = { openPlayStoreListing(activity) }
    ) {
        try {
            val manager = ReviewManagerFactory.create(activity)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    val flow = manager.launchReviewFlow(activity, reviewInfo)
                    flow.addOnCompleteListener {
                        // The flow has finished. In-App Review API provides no indication of whether the user reviewed or not.
                    }
                } else {
                    // Fall back to opening Play Store
                    onFallbackToPlayStore()
                }
            }
        } catch (e: Exception) {
            onFallbackToPlayStore()
        }
    }

    fun buildEmailBody(extraHeader: String? = null): String {
        val header = if (!extraHeader.isNullOrBlank()) "$extraHeader\n\n" else ""
        return "${header}Describe the issue:\n\n\n\nSteps to reproduce:\n\n\n---\nApp version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\nAndroid: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\nDevice: ${Build.MANUFACTURER} ${Build.MODEL}"
    }

    fun sendSupportEmail(
        context: Context,
        subject: String,
        body: String,
        onNoEmailApp: (email: String) -> Unit
    ) {
        val targetEmail = AppLinks.SUPPORT_EMAIL
        try {
            // mailto URI with encoded parameters
            val encodedSubject = Uri.encode(subject)
            val encodedBody = Uri.encode(body)
            val mailtoUri = Uri.parse("mailto:$targetEmail?subject=$encodedSubject&body=$encodedBody")

            val emailIntent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
                // Setting EXTRA_EMAIL ensures Gmail and all mail apps populate the 'To' field
                putExtra(Intent.EXTRA_EMAIL, arrayOf(targetEmail))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(emailIntent)
        } catch (e: Exception) {
            try {
                // Secondary fallback: generic ACTION_SENDTO with mailto:
                val fallbackIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(targetEmail))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                onNoEmailApp(targetEmail)
            }
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Email") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
    }
}
