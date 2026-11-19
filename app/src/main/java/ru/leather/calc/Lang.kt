package ru.leather.calc

import androidx.compose.runtime.mutableStateOf

/**
 * Простой слой локализации: строки задаются парами «русский / английский» прямо на месте.
 * Это позволяет переводить приложение постепенно, без переработки всего кода на ключи.
 */
object Lang {
    private val current = mutableStateOf("ru")

    fun set(code: String) { current.value = if (code == "en") "en" else "ru" }
    fun code(): String = current.value
    fun isEn(): Boolean = current.value == "en"

    private val docEnState = mutableStateOf(false)

    fun setDoc(en: Boolean) { docEnState.value = en }
    fun docIsEn(): Boolean = docEnState.value

    fun t(ru: String, en: String): String = if (isEn()) en else ru

    /** Строки документов (PDF, CSV) — зависят от языка документа, а не интерфейса. */
    fun doc(ru: String, en: String): String = if (docEnState.value) en else ru
}
