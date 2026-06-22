package com.rohit.smartshare.data

import android.content.Context

object AppContainer {

    lateinit var database: AppDatabase

    fun initialize(
        context: Context
    ) {
        database =
            DatabaseProvider.getDatabase(
                context
            )
    }
}