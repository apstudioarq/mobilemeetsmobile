package com.ingevent.data.repository

import com.ingevent.data.model.MapContent
import com.ingevent.data.remote.INGEventApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MapContentRepository(
    private val api: INGEventApi,
) {
    fun getMapContent(): Flow<MapContent> = flow {
        emit(api.getMapContent())
    }
}
