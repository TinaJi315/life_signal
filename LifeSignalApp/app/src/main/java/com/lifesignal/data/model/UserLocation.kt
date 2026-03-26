package com.lifesignal.data.model

import com.google.firebase.firestore.GeoPoint
import java.util.Date

/**
 * 用户位置数据模型 (扩展)
 * 存储在 Firestore 中 users/{uid} 文档的附加字段
 * 配合 Google Maps SDK 在地图上显示好友位置
 *
 * Firestore 字段说明:
 *   geoPoint: GeoPoint          — 经纬度坐标 (用于地图 Marker)
 *   location: String            — 简短位置名 (如 "Home", "Office")
 *   lastLocationUpdate: Date    — 最后一次位置更新时间
 */
data class UserLocation(
    val uid: String = "",
    val name: String = "",
    val geoPoint: GeoPoint? = null,
    val location: String = "",
    val lastLocationUpdate: Date? = null,
    val status: String = "safe",       // 用于在地图 Marker 上显示颜色
    val profileImageUrl: String = ""   // 用于在地图 Marker 上显示头像
)
