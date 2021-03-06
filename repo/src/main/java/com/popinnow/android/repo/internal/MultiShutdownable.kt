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

package com.popinnow.android.repo.internal

/**
 * [MultiShutdownable] are like [Shutdownable] interfaces with more fine grained control.
 *
 * An [MultiShutdownable] can shutdown a specific key in its collection of data, or its entire collection.
 *
 * @see Shutdownable
 */
interface MultiShutdownable : Shutdownable, MultiClearable {

  /**
   * Shutdown in flight requests for a given key, and clears the cached data for that key
   *
   * @param key The key for this request.
   * @see shutdown
   */
  fun shutdown(key: String)
}
