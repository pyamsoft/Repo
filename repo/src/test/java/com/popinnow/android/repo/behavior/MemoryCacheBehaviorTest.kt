/*
 * Copyright (C) 2019 POP Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.popinnow.android.repo.behavior

import com.popinnow.android.repo.MemoryCache
import com.popinnow.android.repo.impl.Logger
import com.popinnow.android.repo.impl.MemoryCacheImpl
import com.popinnow.android.repo.logger.SystemLogger
import com.popinnow.android.repo.startNow
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS

class MemoryCacheBehaviorTest : BaseBehaviorTest() {

  private var _cache: MemoryCache<String>? = null
  private val cache: MemoryCache<String>
    get() = requireNotNull(_cache)

  private fun shutdown() {
    _cache?.clear()
    _cache = null
  }

  @Before
  fun before() {
    shutdown()
  }

  @After
  fun after() {
    shutdown()
  }

  private fun createCache(
    tag: String,
    time: Long
  ) {
    _cache = MemoryCacheImpl(Logger.create(tag, true, SystemLogger), time, SECONDS)
  }

  private fun assertCacheIsEmpty(cache: MemoryCache<String>) {
    cache.get()
        .startNow()
        .test()
        .assertNoValues()
        .assertNoErrors()
        .assertComplete()
  }

  private fun assertCacheSingleValue(
    cache: MemoryCache<String>,
    value: String
  ) {
    cache.get()
        .startNow()
        .test()
        .assertValue(value)
        .assertNoErrors()
        .assertComplete()
  }

  private fun assertCacheValues(
    cache: MemoryCache<String>,
    vararg values: String
  ) {
    cache.get()
        .startNow()
        .test()
        .assertValueSequence(arrayListOf(*values))
        .assertNoErrors()
        .assertComplete()
  }

  private fun fillCacheWithDummyData(
    cache: MemoryCache<String>,
    size: Int
  ) {
    for (i in 1..size) {
      cache.add("data$i")
    }
  }

  /**
   * Starts empty by default. If nothing puts into the cache, it returns no data.
   */
  @Test
  fun `MemoryCacheBehaviorTest is empty by default`() {
    createCache("is empty default", 30)
    assertCacheIsEmpty(cache)
  }

  /**
   * Does it actually work
   */
  @Test
  fun `MemoryCacheBehaviorTest maintains cache`() {
    createCache("maintain cache", 30)

    // Putting data into the cache fills it
    cache.add(DEFAULT_EXPECT)

    // So it returns data on query
    val now = System.currentTimeMillis()
    while (now + 1000 > System.currentTimeMillis()) {
      assertCacheSingleValue(cache, DEFAULT_EXPECT)
    }
  }

  /**
   * When you invalidate the cache, the old data is gone immediately
   */
  @Test
  fun `MemoryCacheBehaviorTest invalidates`() {
    createCache("invalidates", 30)
    // Putting data into the cache fills it
    cache.add(DEFAULT_EXPECT)

    // So it returns data on query
    assertCacheSingleValue(cache, DEFAULT_EXPECT)

    // Invalidating the cache then clears it
    cache.clear()

    // So it does not return
    assertCacheIsEmpty(cache)
  }

  /**
   * When you clear the cache, the old data is gone immediately
   */
  @Test
  fun `MemoryCacheBehaviorTest clears`() {
    createCache("clears", 30)
    // Putting data into the cache fills it
    cache.add(DEFAULT_EXPECT)

    // So it returns data on query
    assertCacheSingleValue(cache, DEFAULT_EXPECT)

    // Put some more data
    fillCacheWithDummyData(cache, 5)

    // Invalidating the cache then clears it
    cache.clear()

    // So it does not return
    assertCacheIsEmpty(cache)
  }

  /**
   * Once the time expires, cache will not expose its data even if it has some
   */
  @Test
  fun `MemoryCacheBehaviorTest times out`() {
    createCache("times out", 1)

    cache.add(DEFAULT_EXPECT)
    assertCacheSingleValue(cache, DEFAULT_EXPECT)

    // After time out passes, cache does not return value
    Observable.just("")
        .delay(2, SECONDS)
        .startNow()
        .blockingSubscribe { assertCacheIsEmpty(cache) }
  }

  /**
   * Adding multiple elements will track all of them internally until it is invalidated
   */
  @Test
  fun `MemoryCacheBehaviorTest tracks history`() {
    createCache("tracks history", 30)
    val values = ArrayList<String>()
    for (i in 0 until 100) {
      val random = UUID.randomUUID()
          .toString()

      cache.add(random)
      values.add(random)
      assertCacheValues(cache, *values.toTypedArray())
    }

  }

  /**
   * Calling put in the middle of a stream from the cache does not modify the output of the stream
   */
  @Test
  fun `MemoryCacheBehaviorTest put does not modify stream`() {
    createCache("put does not modify", 30)
    val extraPuts = "Extra"
    val expect1 = "Testing"
    val expect2 = "Puts"

    cache.add(DEFAULT_EXPECT)
    cache.get()
        .doOnSubscribe { cache.add(expect1) }
        .startNow()
        .test()
        // The put happens after the cache is accessed, so it should not appear
        .assertValue(DEFAULT_EXPECT)
        .assertValueCount(1)
        .assertComplete()

    cache.add(extraPuts)
    cache.get()
        .doOnSubscribe { cache.add(expect2) }
        .startNow()
        .test()
        // The put happens after the cache is accessed, so it should not appear
        // but the first access plus the first put should
        .assertValueSequence(arrayListOf(DEFAULT_EXPECT, expect1, extraPuts))
        .assertValueCount(3)
        .assertComplete()

    // Finally, everything should be in cache on next get
    assertCacheValues(cache, DEFAULT_EXPECT, expect1, extraPuts, expect2)
  }

  companion object {

    private const val DEFAULT_EXPECT = "Hello World"
  }
}
