package com.stacktivity.yandeximagesearchengine.data.model.api

import com.stacktivity.yandeximagesearchengine.data.model.ImageData
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("search")
    fun getJSONSearchResult(@Query("format") format: String = "json",
                @Query("request") add: String = "{\"blocks\":[{\"block\":\"serp-list_infinite_yes\"" +
                            ",\"params\":{\"initialPageNum\":0},\"version\":2}]" +
                        ",\"bmt\":{\"lb\":\"70xek^jjDN({yjI=52Fx\"}" +
                        ",\"amt\":{\"las\":\"justifier-height=1;thumb-underlay=1;justifier-setheight=1;fitimages-height=1;justifier-fitincuts=1\"}}",
                @Query("yu") yu: String = "1778190371562362282",
                @Query("p") page: Int = 0,
                @Query("text") search: String
    ): Call<ImageData>
}