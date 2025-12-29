package core

sealed class Result<out T> {
    data class Ok<T>(val value: T) : Result<T>()
    data class Err(val message: String, val throwable: Throwable? = null) : Result<Nothing>()
}
