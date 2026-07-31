package com.example.healthcheckin.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.healthcheckin.R

fun openExternalLink(
    context: Context,
    url: String,
    onLinkClicked: ((String) -> Unit)? = null,
) {
    onLinkClicked?.invoke(url)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        Toast.makeText(
            context,
            context.getString(R.string.external_link_no_browser, url),
            Toast.LENGTH_LONG,
        ).show()
    }
}

fun copyToClipboard(context: Context, label: String, text: String, toastRes: Int = R.string.common_copied) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, context.getString(toastRes), Toast.LENGTH_SHORT).show()
}
