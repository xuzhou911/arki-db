
/*
 *
 *     Copyright (c) 2019 Sandeep Virdi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.virdis.search.inmemory

import java.util

import com.virdis.models.PayloadBuffer

trait InMemoryMapSearch[F[_]] {
  // TODO this is not required, if we refactor the design to use min,max key from Map to enforce size
  def putMapInBuffer(map: util.NavigableMap[Long, PayloadBuffer]): F[Unit]
  def searchKey(key: Long): F[Option[PayloadBuffer]]
  def remove(minKey: Long, maxKey: Long): F[Unit]
}
