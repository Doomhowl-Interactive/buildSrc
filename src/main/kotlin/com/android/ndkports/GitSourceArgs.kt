package com.android.ndkports

import java.io.Serializable

data class GitSourceArgs(
    val url: String,
    val branch: String
): Serializable