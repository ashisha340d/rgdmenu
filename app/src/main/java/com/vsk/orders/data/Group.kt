package com.vsk.orders.data

data class Group(
    val id: String = "",
    val name: String = "",
    val members: List<String> = emptyList(),
    val admins: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Long = 0L
)
