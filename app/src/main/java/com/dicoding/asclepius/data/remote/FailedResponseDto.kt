package com.dicoding.asclepius.data.remote

import com.google.gson.annotations.SerializedName

data class FailedResponseDto(

	@field:SerializedName("code")
	val code: String? = null,

	@field:SerializedName("message")
	val message: String? = null,

	@field:SerializedName("status")
	val status: String? = null
)
