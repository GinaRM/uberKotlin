package com.gina.uberclone.models
import com.beust.klaxon.*

class Price {



    private val klaxon = Klaxon()

    data class Price (
        val km: Double? = null,
        val min: Double? = null,

        @Json(name = "min_value")
        val minValue: Double? = null,

        val difference: Double? = null
    ) {
        public fun toJson() = klaxon.toJsonString(this)

        companion object {
            public fun fromJson(json: String) = klaxon.parse<Price>(json)
        }
    }

}