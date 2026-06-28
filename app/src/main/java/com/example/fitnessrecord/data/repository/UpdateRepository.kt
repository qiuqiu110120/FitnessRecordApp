package com.example.fitnessrecord.data.repository

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface UpdateRepository {
    suspend fun getLatestRelease(): AppRelease?
}

class GitHubUpdateRepository(
    private val client: OkHttpClient,
    private val json: Json,
) : UpdateRepository {
    override suspend fun getLatestRelease(): AppRelease? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/qiuqiu110120/FitnessRecordApp/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "FRA-Android")
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            error("GitHub 返回 ${response.code}，请稍后重试。")
        }

        val body = response.body?.string().orEmpty()
        val release = json.decodeFromString<GitHubReleaseResponse>(body)
        if (release.draft || release.prerelease) {
            null
        } else {
            AppRelease(
                tagName = release.tagName,
                name = release.name.ifBlank { release.tagName },
                body = release.body.orEmpty(),
                pageUrl = release.htmlUrl,
                apkUrl = release.assets.firstOrNull { asset ->
                    asset.name.endsWith(".apk", ignoreCase = true)
                }?.browserDownloadUrl
            )
        }
    }
}

data class AppRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val pageUrl: String,
    val apkUrl: String?,
)

@Serializable
data class GitHubReleaseResponse(
    @SerialName("tag_name")
    val tagName: String,
    val name: String = "",
    val body: String? = null,
    @SerialName("html_url")
    val htmlUrl: String,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubReleaseAsset> = emptyList(),
)

@Serializable
data class GitHubReleaseAsset(
    val name: String,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String,
)
