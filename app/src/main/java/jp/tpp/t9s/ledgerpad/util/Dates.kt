package jp.tpp.t9s.ledgerpad.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Dates {
    fun formatDate(timeMillis: Long): String =
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timeMillis))

    fun formatDateTime(timeMillis: Long): String =
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timeMillis))

    fun formatShortDate(timeMillis: Long): String =
        SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timeMillis))

    fun isToday(timeMillis: Long): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance().apply { timeInMillis = timeMillis }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun startOfDay(timeMillis: Long): Long {
        val cal = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun endOfDay(timeMillis: Long): Long {
        val cal = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    /**
     * Converts local timestamp to UTC midnight for Material 3 DatePicker initial state.
     */
    fun localDateToUtcMidnight(localMillis: Long): Long {
        val localCal = Calendar.getInstance().apply { timeInMillis = localMillis }
        val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, localCal.get(Calendar.YEAR))
            set(Calendar.MONTH, localCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, localCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return utcCal.timeInMillis
    }

    /**
     * Converts Material 3 DatePicker's selected UTC midnight timestamp to local timestamp,
     * preserving local hour/minute/second so entries don't all look like 00:00 or shift a day.
     */
    fun utcMidnightToLocalMillis(utcMidnightMillis: Long, preserveLocalTimeFrom: Long = System.currentTimeMillis()): Long {
        val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMidnightMillis
        }
        val localTimeCal = Calendar.getInstance().apply {
            timeInMillis = preserveLocalTimeFrom
        }
        val resultCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
            set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, localTimeCal.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, localTimeCal.get(Calendar.MINUTE))
            set(Calendar.SECOND, localTimeCal.get(Calendar.SECOND))
            set(Calendar.MILLISECOND, localTimeCal.get(Calendar.MILLISECOND))
        }
        return resultCal.timeInMillis
    }
}
