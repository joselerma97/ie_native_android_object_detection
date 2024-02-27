package com.ie.real_time_object_detection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.converter.gson.GsonConverterFactory

class ScoreViewModel: ViewModel() {

    var name: String? = null
    var score: Float? = null

    val _tips = MutableLiveData<String>()
    val tips: LiveData<String> = _tips

    private val tipsService = createRetrofitService()

    fun resetTips(){
        viewModelScope.launch(Dispatchers.IO) {
            _tips.postValue("")
        }
    }

    fun hasNameAndScore(): Boolean {
        return !name.isNullOrBlank() && score != null
    }

    private fun createRetrofitService(): TipsService {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://dermatechserver.cloud/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(TipsService::class.java)
    }

    fun fetchTips(type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response: Call<TipsResponse> = tipsService.getTips("How can I recycle: $type")
                _tips.postValue(response.await().tips)
            } catch (e: Exception) {
                e.printStackTrace()
                _tips.postValue("Error fetching tips")
            }
        }
    }

}