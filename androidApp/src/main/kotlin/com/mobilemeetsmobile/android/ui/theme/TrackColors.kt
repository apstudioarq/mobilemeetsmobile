package com.mobilemeetsmobile.android.ui.theme

import androidx.compose.ui.graphics.Color
import com.mobilemeetsmobile.data.model.Track

fun Track.accentColor(): Color = when (this) {
    Track.AI_ML -> TrackAiMl
    Track.ANDROID -> TrackAndroid
    Track.IOS -> Color(0xFF111111)
    Track.GENERIC -> Color(0xFF5F6368)
    Track.WEB -> TrackWeb
    Track.CLOUD -> TrackCloud
    Track.FIREBASE -> TrackFirebase
    Track.FLUTTER -> TrackFlutter
    Track.DESIGN -> TrackDesign
}

fun Track.bgColor(): Color = when (this) {
    Track.AI_ML -> TrackAiMlBg
    Track.ANDROID -> TrackAndroidBg
    Track.IOS -> Color(0xFFEFEFEF)
    Track.GENERIC -> Color(0xFFF1F3F4)
    Track.WEB -> TrackWebBg
    Track.CLOUD -> TrackCloudBg
    Track.FIREBASE -> TrackFirebaseBg
    Track.FLUTTER -> TrackFlutterBg
    Track.DESIGN -> TrackDesignBg
}
