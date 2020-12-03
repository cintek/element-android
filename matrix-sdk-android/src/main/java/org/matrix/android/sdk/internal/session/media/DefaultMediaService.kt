/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.media

import androidx.collection.LruCache
import org.matrix.android.sdk.api.cache.CacheStrategy
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.media.MediaService
import org.matrix.android.sdk.api.session.media.PreviewUrlData
import org.matrix.android.sdk.api.util.JsonDict
import javax.inject.Inject

internal class DefaultMediaService @Inject constructor(
        private val clearPreviewUrlCacheTask: ClearPreviewUrlCacheTask,
        private val getPreviewUrlTask: GetPreviewUrlTask,
        private val getRawPreviewUrlTask: GetRawPreviewUrlTask,
        private val urlsExtractor: UrlsExtractor
) : MediaService {
    // Cache of extracted URLs
    private val extractedUrlsCache = LruCache<String, List<String>>(1_000)

    override fun extractUrls(event: Event): List<String> {
        val cacheKey = event.cacheKey()
        return extractedUrlsCache.get(cacheKey)
                ?: let {
                    urlsExtractor.extract(event)
                            .also { extractedUrlsCache.put(cacheKey, it) }
                }
    }

    private fun Event.cacheKey() = "${eventId ?: ""}-${roomId ?: ""}"

    override suspend fun getRawPreviewUrl(url: String, timestamp: Long?): JsonDict {
        return getRawPreviewUrlTask.execute(GetRawPreviewUrlTask.Params(url, timestamp))
    }

    override suspend fun getPreviewUrl(url: String, timestamp: Long?, cacheStrategy: CacheStrategy): PreviewUrlData {
        return getPreviewUrlTask.execute(GetPreviewUrlTask.Params(url, timestamp, cacheStrategy))
    }

    override suspend fun clearCache() {
        extractedUrlsCache.evictAll()
        clearPreviewUrlCacheTask.execute(Unit)
    }
}
