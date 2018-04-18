package com.example.tarvshttp2

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.facebook.battery.metrics.cpu.CpuMetrics
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val subscriptions = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val repository = Repository()

        button_start_tar.setOnClickListener {
            subscriptions += measure(repository.fetchTar(), loading_tar, result_tar)
        }

        button_start_images.setOnClickListener {
            subscriptions += measure(repository.fetchImages(), loading_images, result_images)
        }
    }

    private fun measure(source: Single<CpuMetrics>, progressBar: ProgressBar, textView: TextView): Disposable {
        return source
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { progressBar.visibility = View.VISIBLE }
                .subscribe { metrics ->
                    progressBar.visibility = View.GONE
                    textView.text = metrics.toString()
                }
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.clear()
    }
}

private operator fun CompositeDisposable.plusAssign(subscription: Disposable) {
    add(subscription)
}
