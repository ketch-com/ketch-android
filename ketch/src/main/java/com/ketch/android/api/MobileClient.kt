package com.ketch.android.api

import android.content.Context
import io.grpc.*
import io.grpc.android.AndroidChannelBuilder
import mobile.MobileGrpc
import java.util.concurrent.TimeUnit

class MobileClient private constructor(channel: Channel) {
    companion object {
        private const val SERVER_NAME = "mobile.dev.b10s.io"
        private const val SERVER_PORT = 443;
    }

    val blockingStub = MobileGrpc.newBlockingStub(channel).withInterceptors(TimeoutInterceptor())

    constructor(context: Context) : this(
        AndroidChannelBuilder.forAddress(SERVER_NAME, SERVER_PORT).context(context).build()
    )

    class TimeoutInterceptor : ClientInterceptor {
        override fun <ReqT : Any, RespT : Any> interceptCall(
            method: MethodDescriptor<ReqT, RespT>,
            callOptions: CallOptions,
            next: Channel
        ): ClientCall<ReqT, RespT> {
            return next.newCall(method, callOptions.withDeadlineAfter(20, TimeUnit.SECONDS));
        }
    }

}