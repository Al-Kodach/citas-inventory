package com.citacomsys.core.database

import app.cash.sqldelight.ColumnAdapter
import kotlin.time.Instant

inline fun <reified F : Any, reified T : Any> primitiveColumnAdapter(
    crossinline encode: (F) -> T,
    crossinline decode: (T) -> F
): ColumnAdapter<F, T> {
    return object : ColumnAdapter<F, T> {
        override fun encode(value: F): T = encode(value)
        override fun decode(databaseValue: T): F = decode(databaseValue)
    }
}

val LongAsIntAdapter =
    primitiveColumnAdapter<Int, Long>(
        encode = { it.toLong() },
        decode = { it.toInt() }
    )

val BooleanAsLongAdapter =
    primitiveColumnAdapter<Boolean, Long>(
        encode = { if (it) 1L else 0L },
        decode = { it == 1L }
    )

val InstantAsLongAdapter =
    primitiveColumnAdapter<Instant, Long>(
        encode = { it.toEpochMilliseconds() },
        decode = { Instant.fromEpochMilliseconds(it) }
    )

val InstantAsStringAdapter = primitiveColumnAdapter<Instant, String>(
    encode = { it.toString() },
    decode = { Instant.parse(it) }
)

inline fun <reified E : Enum<E>> enumerationByName(): ColumnAdapter<E, String> =
    primitiveColumnAdapter(
        encode = { it.name },
        decode = { enumValueOf<E>(it) }
    )
