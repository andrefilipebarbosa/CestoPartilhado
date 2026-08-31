package pt.cestopartilhado.app.util

import android.content.Context
import pt.cestopartilhado.app.R
import java.util.Date
import java.util.concurrent.TimeUnit

/** Texto simples "há X horas/dias" — só as escalas que aparecem nos mockups. */
fun relativeTimeText(context: Context, date: Date?): String {
    if (date == null) return ""
    val diffMs = System.currentTimeMillis() - date.time
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)
    return when {
        hours < 1 -> context.getString(R.string.time_just_now)
        hours < 24 -> context.getString(R.string.time_hours_ago, hours.toInt())
        days < 2 -> context.getString(R.string.time_yesterday)
        else -> context.getString(R.string.time_days_ago, days.toInt())
    }
}
