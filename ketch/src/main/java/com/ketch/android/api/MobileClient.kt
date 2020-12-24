package com.ketch.android.api

import android.content.Context
import com.ketch.android.BuildConfig.SERVER_NAME
import com.ketch.android.BuildConfig.SERVER_PORT
import io.grpc.*
import io.grpc.android.AndroidChannelBuilder
import io.grpc.android.BuildConfig
import mobile.MobileGrpc
import java.util.concurrent.TimeUnit

class MobileClient private constructor(channel: Channel) {
    val blockingStub: MobileGrpc.MobileBlockingStub = MobileGrpc
        .newBlockingStub(channel)
        .withInterceptors(TimeoutInterceptor())

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