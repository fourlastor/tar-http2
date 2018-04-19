package com.example.tarvshttp2

import com.example.tarvshttp2.BuildConfig.CLOUDFRONT_HOST
import com.example.tarvshttp2.CloudFront.IMAGES
import com.facebook.battery.metrics.cpu.CpuMetrics
import com.facebook.battery.metrics.cpu.CpuMetricsCollector
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException

class Repository {
    private val client: OkHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()

    private val collector = CpuMetricsCollector()

    fun fetchTar() = metricsOn(request("tar/edition_2018-04-18_xhdpi.tar"))

    fun fetchImages() = metricsOn(Completable.merge(IMAGES.map { request("images/$it") }))

    private fun metricsOn(original: Completable): Single<CpuMetrics> {
        val initialMetrics = collector.createMetrics()
        val finalMetrics = collector.createMetrics()
        return original
                .doOnSubscribe { collector.getSnapshot(initialMetrics) }
                .doOnComplete { collector.getSnapshot(finalMetrics) }
                .toSingle { finalMetrics - initialMetrics }
    }

    private fun request(path: String) = Completable.create { emitter ->
        val request = Request.Builder()
                .cacheControl(CacheControl.FORCE_NETWORK)
                .url("$CLOUDFRONT_HOST/$path")
                .build()

        val call = client.newCall(request).apply {
            enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    emitter.onError(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.protocol() != Protocol.HTTP_2) {
                        emitter.onError(IllegalStateException("Protocol is not HTTP/2"))
                    } else {
                        emitter.onComplete()
                    }

                    response.body()?.close()
                }

            })
        }

        emitter.setCancellable { call.cancel() }
    }
}

private operator fun CpuMetrics.minus(other: CpuMetrics): CpuMetrics = diff(other)
