package com.blackboxpro.plugin.command.testframework

import com.blackboxpro.common.protocol.ResponseMessage
import java.util.concurrent.CompletableFuture

data class BlackBoxPrepareResult(
    val skipReason: String? = null
)

data class BlackBoxActionTestResult(
    val case: BlackBoxActionTestCase,
    val status: BlackBoxTestStatus,
    val message: String,
    val response: ResponseMessage? = null
)

data class BlackBoxActionTestCase(
    val actionId: String,
    val displayName: String,
    val category: String,
    val supportedProfiles: Set<BlackBoxLoaderProfile> = BlackBoxLoaderProfile.entries.toSet(),
    val timeoutMs: Long = 5000L,
    val prepare: (BlackBoxTestContext) -> CompletableFuture<BlackBoxPrepareResult> = {
        CompletableFuture.completedFuture(BlackBoxPrepareResult())
    },
    val execute: (BlackBoxTestContext) -> CompletableFuture<ResponseMessage>,
    val verify: (BlackBoxTestContext, ResponseMessage) -> String? = { _, response ->
        if (response.isSuccess) null else (response.message ?: response.status)
    },
    val cleanup: (BlackBoxTestContext) -> CompletableFuture<Unit> = {
        CompletableFuture.completedFuture(Unit)
    }
)
