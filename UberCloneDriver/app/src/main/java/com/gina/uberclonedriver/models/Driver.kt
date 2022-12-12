package com.gina.uberclonedriver.models


import com.beust.klaxon.*

private val klaxon = Klaxon()

data class Driver (
    val id: String ? = null,
    val name: String ? = null,
    val lastname: String ? = null,
    val email: String ? = null,
    val phone: String ? = null,
    val image: String ? = null,
    val plateNumber: String ? = null,
    val carColor: String ? = null,
    val carBrand: String ? = null
) {


    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<Client>(json)
    }
}