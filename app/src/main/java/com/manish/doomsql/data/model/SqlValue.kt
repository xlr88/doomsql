package com.manish.doomsql.data.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface SqlValue {
    @Serializable
    data object Null : SqlValue {
        override fun toString(): String = "NULL"
    }

    @Serializable
    data class Integer(val value: Long) : SqlValue {
        override fun toString(): String = value.toString()
    }

    @Serializable
    data class Real(val value: Double) : SqlValue {
        override fun toString(): String =
            if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }

    @Serializable
    data class Text(val value: String) : SqlValue {
        override fun toString(): String = value
    }
}
