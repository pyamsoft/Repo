/*
 * Copyright (C) 2018 POP Inc.
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

@file:Suppress("NOTHING_TO_INLINE")

package com.popinnow.android.repo

import androidx.annotation.CheckResult
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.lang.reflect.Type

@CheckResult
internal inline fun <T : Any> Observable<T>.startNow(): Observable<T> {
  return this.compose {
    return@compose it
        .subscribeOn(Schedulers.trampoline())
        .observeOn(Schedulers.trampoline())
  }
}

@CheckResult
internal inline fun <T : Any> Single<T>.startNow(): Single<T> {
  return this.compose {
    return@compose it
        .subscribeOn(Schedulers.trampoline())
        .observeOn(Schedulers.trampoline())
  }
}

private val gson = GsonBuilder()
    .setPrettyPrinting()
    .serializeNulls()
    .create()

@CheckResult
internal inline fun <reified T : Any> typedList(): Type {
  val type = TypeToken.get(T::class.java)
      .type
  return TypeToken.getParameterized(ArrayList::class.java, type)
      .type
}

@CheckResult
internal inline fun <reified T : Any> ArrayList<T>.toJson(): String {
  return gson.toJson(this)
}

@CheckResult
internal inline fun <reified T : Any> String.toListOfObjects(): ArrayList<T> {
  return gson.fromJson(this, typedList<T>())
}
