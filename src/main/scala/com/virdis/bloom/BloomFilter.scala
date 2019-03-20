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

package com.virdis.bloom

import java.nio.{ByteBuffer, ByteOrder}

import com.virdis.models.GeneratedKey
import com.virdis.utils.{Config, Constants}
import net.jpountz.xxhash.{XXHash64, XXHashFactory}
import scodec.bits.BitVector

trait BloomFilter[A] {
  def byteBuffer: ByteBuffer
  def add(generatedKey: GeneratedKey)
  def contains(generatedKey: GeneratedKey): Boolean

  def hashes: Int      = config.bloomFilterHashes
  def bits: Int        = config.bloomFilterBits
  def hasher: XXHash64 = XXHashFactory.fastestInstance().hash64()
  val config: Config   = new Config()
}

object BloomFilter {

  def apply[A](implicit eve: BloomFilter[A]): BloomFilter[A] = eve

  implicit val bitVectorBloom: BloomFilter[BitVector] = new BloomFilter[BitVector] {

    override def byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(bits/8) // allocate bytes

    private var bitVector: BitVector = BitVector.view(byteBuffer)

    override def add(generatedKey: GeneratedKey): Unit = {
      val hash1 = hasher.hash(generatedKey.toBuffer, Constants.BLOOM_SEED)
      val bb = ByteBuffer.allocate(Constants.LONG_SIZE_IN_BYTES).order(ByteOrder.BIG_ENDIAN)
      bb.putLong(hash1)
      bb.flip()
      val hash2 = hasher.hash(bb, Constants.BLOOM_SEED)
      var i = 0
      while(i < hashes) {
        val bitToSet = ((hash1 + (i * hash2)) & Long.MaxValue) % bits
        bitVector = bitVector.update(bitToSet, true)
        i += 1
      }

    }

    override def contains(generatedKey: GeneratedKey): Boolean = {
      val hash1 = hasher.hash(generatedKey.toBuffer, Constants.BLOOM_SEED)
      val bb = ByteBuffer.allocate(Constants.LONG_SIZE_IN_BYTES).order(ByteOrder.BIG_ENDIAN)
      bb.putLong(hash1)
      bb.flip()
      val hash2 = hasher.hash(bb, Constants.BLOOM_SEED)
      var i = 0
      var result = true
      while(i < hashes) {
        val bitToCheck: Long = ((hash1 + (i * hash2)) & Long.MaxValue) % bits
        val bitValue = bitVector.get(bitToCheck)
        result = result && bitValue // maybe return early
        i += 1
      }
      result
    }


  }

  implicit val byteBufferBloom: BloomFilter[ByteBuffer] = new BloomFilter[ByteBuffer]{

    override final val byteBuffer = ByteBuffer.allocateDirect(bits * 8)

    override def add(generatedKey: GeneratedKey): Unit = {
      val hash = hasher.hash(generatedKey.toBuffer, Constants.BLOOM_SEED)
      val bb = ByteBuffer.allocate(Constants.LONG_SIZE_IN_BYTES).order(ByteOrder.BIG_ENDIAN)
      bb.putLong(hash)
      bb.flip()
      val hash2 = hasher.hash(bb, Constants.BLOOM_SEED)
      var i = 0
      while(i < hashes) {
        val bitToSet: Long = ((hash + (i * hash2)) & Long.MaxValue) % bits
        byteBuffer.put(bitToSet.toInt, Constants.TRUE_BYTES)
        i += 1
      }
    }

    override def contains(generatedKey: GeneratedKey): Boolean = {
      val hash = hasher.hash(generatedKey.toBuffer, Constants.BLOOM_SEED)
      val bb = ByteBuffer.allocate(Constants.LONG_SIZE_IN_BYTES).order(ByteOrder.BIG_ENDIAN)
      bb.putLong(hash)
      bb.flip()
      val hash2 = hasher.hash(bb, Constants.BLOOM_SEED)
      var i = 0
      var result = 1
      while(i < hashes) {
        val bitToCheck: Long = ((hash + (i * hash2)) & Long.MaxValue) % bits
        val bitValue: Byte = byteBuffer.get(bitToCheck.toInt)
        result = result & bitValue
        i += 1
      }
      result == 1
    }
  }
}