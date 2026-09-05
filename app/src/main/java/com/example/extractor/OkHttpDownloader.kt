package com.example.extractor

import com.example.data.local.SecurePreferences
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Custom Downloader implementation for NewPipeExtractor backed by OkHttp.
 * Automatically injects YouTube user-agent and user authentication cookies
 * (SAPISID, SSID, HSID, LOGIN_INFO) stored in SecurePreferences.
 */
class OkHttpDownloader(
    private val securePreferences: SecurePreferences? = null
) : Downloader() {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val reqBuilder = okhttp3.Request.Builder()
            .url(url)

        // Set default User-Agent if not provided in request headers
        var hasUserAgent = false
        var hasCookie = false

        if (headers != null) {
            for ((headerName, headerValues) in headers) {
                if (headerName.equals("User-Agent", ignoreCase = true)) {
                    hasUserAgent = true
                }
                if (headerName.equals("Cookie", ignoreCase = true)) {
                    hasCookie = true
                }
                for (value in headerValues) {
                    reqBuilder.addHeader(headerName, value)
                }
            }
        }

        if (!hasUserAgent) {
            reqBuilder.header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
            )
        }

        // Attach stored authentication session cookie if not explicitly provided
        if (!hasCookie && securePreferences != null) {
            val cookieHeader = securePreferences.getCookieHeader()
            if (cookieHeader.isNotEmpty()) {
                reqBuilder.header("Cookie", cookieHeader)
            }
        }

        // Handle HTTP method & body
        when (httpMethod.uppercase()) {
            "GET" -> reqBuilder.get()
            "HEAD" -> reqBuilder.head()
            "POST" -> {
                val body = dataToSend?.toRequestBody(null) ?: ByteArray(0).toRequestBody(null)
                reqBuilder.post(body)
            }
            else -> {
                val body = dataToSend?.toRequestBody(null)
                reqBuilder.method(httpMethod, body)
            }
        }

        val okResponse = client.newCall(reqBuilder.build()).execute()
        val responseCode = okResponse.code
        val responseMessage = okResponse.message

        if (responseCode == 429) {
            okResponse.close()
            throw ReCaptchaException("YouTube rate limit / CAPTCHA encountered (HTTP 429)", url)
        }

        val responseBodyString = okResponse.body?.string().orEmpty()
        val latestUrl = okResponse.request.url.toString()

        val responseHeaders = mutableMapOf<String, List<String>>()
        for (name in okResponse.headers.names()) {
            responseHeaders[name] = okResponse.headers.values(name)
        }

        return Response(
            responseCode,
            responseMessage,
            responseHeaders,
            responseBodyString,
            latestUrl
        )
    }
}
