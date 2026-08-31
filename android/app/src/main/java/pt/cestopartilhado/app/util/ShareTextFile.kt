package pt.cestopartilhado.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Escreve [text] para um ficheiro temporário e abre o menu de partilha
 * nativo do Android (Intent.ACTION_SEND) para o utilizador escolher para onde enviar.
 */
fun shareTextAsFile(context: Context, text: String, fileName: String) {
    val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(exportsDir, fileName)
    file.writeText(text)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
