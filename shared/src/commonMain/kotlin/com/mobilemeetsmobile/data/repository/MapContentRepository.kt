package com.mobilemeetsmobile.data.repository

import com.mobilemeetsmobile.data.model.MapContent
import com.mobilemeetsmobile.data.remote.MobileMeetsMobileApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MapContentRepository(
    private val api: MobileMeetsMobileApi,
) {
    fun getMapContent(): Flow<MapContent> = flow {
        emit(api.getMapContent())
    }
}
