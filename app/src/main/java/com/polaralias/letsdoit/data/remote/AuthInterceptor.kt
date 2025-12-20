package com.polaralias.letsdoit.data.remote

import android.content.SharedPreferences
import com.polaralias.letsdoit.core.util.Constants
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
        val token = sharedPreferences.getString(Constants.KEY_ACCESS_TOKEN, null)
        if (token != null) {
            request.addHeader("Authorization", token)
        }
        return chain.proceed(request.build())
    }
}
