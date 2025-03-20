package com.dicoding.asclepius.presentation.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityCancerNewsWebBinding
import com.dicoding.asclepius.presentation.utils.showToast

class CancerNewsWebActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCancerNewsWebBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityCancerNewsWebBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initViews() {
        val url = intent.getStringExtra(EXTRA_URL)
        if(url == null){
            showToast(getString(R.string.sorry_something_went_wrong_pleasy_try_again))
            finish()
            return
        }

        binding.apply {
            tvWebUrl.text = url

            ibClose.setOnClickListener {
                finish()
            }

            webView.webViewClient = object : WebViewClient(){
                override fun doUpdateVisitedHistory(
                    view: WebView?,
                    url: String?,
                    isReload: Boolean
                ) {
                    if(url != tvWebUrl.text){
                        tvWebUrl.text = url
                    }
                    super.doUpdateVisitedHistory(view, url, isReload)
                }

            }

            webView.settings.javaScriptEnabled = true
            webView.webChromeClient = object : WebChromeClient(){
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    progressBar.progress = newProgress

                    val isProgressBarVisible = newProgress != 100
                    if(isProgressBarVisible != progressBar.isVisible){
                        progressBar.isVisible = isProgressBarVisible
                    }
                }
            }
            webView.loadUrl(url)

        }
    }


    companion object{
        const val EXTRA_URL = "extra_url"
    }
}