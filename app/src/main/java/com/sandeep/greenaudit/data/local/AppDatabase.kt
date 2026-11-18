package com.sandeep.greenaudit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sandeep.greenaudit.data.local.converter.EnumConverters
import com.sandeep.greenaudit.data.local.converter.PolygonConverter
import com.sandeep.greenaudit.data.local.dao.ParkDao
import com.sandeep.greenaudit.data.local.dao.PhotoDao
import com.sandeep.greenaudit.data.local.dao.SurveyDao
import com.sandeep.greenaudit.data.local.entity.ParkEntity
import com.sandeep.greenaudit.data.local.entity.PhotoEntity
import com.sandeep.greenaudit.data.local.entity.SurveyEntity

@Database(
    entities = [ParkEntity::class, SurveyEntity::class, PhotoEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(PolygonConverter::class, EnumConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun parkDao(): ParkDao
    abstract fun surveyDao(): SurveyDao
    abstract fun photoDao(): PhotoDao
}
