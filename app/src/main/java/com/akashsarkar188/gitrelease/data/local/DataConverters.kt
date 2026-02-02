package com.akashsarkar188.gitrelease.data.local

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class DataConverters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    private val adapter = moshi.adapter<Map<String, String>>(type)

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? {
        return adapter.toJson(value ?: emptyMap())
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? {
        return if (value == null) emptyMap() else adapter.fromJson(value)
    }
}
