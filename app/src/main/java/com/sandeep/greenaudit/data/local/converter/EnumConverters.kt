package com.sandeep.greenaudit.data.local.converter

import androidx.room.TypeConverter
import com.sandeep.greenaudit.data.local.entity.Cleanliness
import com.sandeep.greenaudit.data.local.entity.ParkStatus
import com.sandeep.greenaudit.data.local.entity.ParkType
import com.sandeep.greenaudit.data.local.entity.SyncState

/** Stores each entity enum as its [Enum.name] text column. */
class EnumConverters {

    @TypeConverter
    fun fromParkType(value: ParkType): String = value.name

    @TypeConverter
    fun toParkType(value: String): ParkType = ParkType.valueOf(value)

    @TypeConverter
    fun fromParkStatus(value: ParkStatus): String = value.name

    @TypeConverter
    fun toParkStatus(value: String): ParkStatus = ParkStatus.valueOf(value)

    @TypeConverter
    fun fromCleanliness(value: Cleanliness): String = value.name

    @TypeConverter
    fun toCleanliness(value: String): Cleanliness = Cleanliness.valueOf(value)

    @TypeConverter
    fun fromSyncState(value: SyncState): String = value.name

    @TypeConverter
    fun toSyncState(value: String): SyncState = SyncState.valueOf(value)
}
