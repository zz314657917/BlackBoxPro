package com.blackboxpro.common.action

data class ActionDefinition(
    val id: String,
    val params: List<String> = emptyList()
)
