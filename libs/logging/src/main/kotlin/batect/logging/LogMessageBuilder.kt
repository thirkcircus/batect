/*
   Copyright 2017-2020 Charles Korn.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package batect.logging

import java.nio.file.Path
import java.time.ZonedDateTime
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer

class LogMessageBuilder(val severity: Severity, val loggerAdditionalData: Map<String, Jsonable> = emptyMap()) {
    private var message: String = ""
    private val data = HashMap<String, Jsonable>()

    fun message(value: String): LogMessageBuilder {
        message = value
        return this
    }

    fun exception(e: Throwable): LogMessageBuilder = exception("exception", e)
    fun exception(key: String, e: Throwable): LogMessageBuilder = data(key, e.toDetailedString(), String.serializer())

    fun <T> data(key: String, value: T, serializer: SerializationStrategy<T>): LogMessageBuilder {
        require(!key.startsWith('@')) { "Cannot add additional data with the key '$key': keys may not start with '@'." }

        data[key] = JsonableObject(value, serializer)
        return this
    }

    fun build(timestampSource: () -> ZonedDateTime, standardAdditionalDataSource: StandardAdditionalDataSource): LogMessage {
        val additionalData = loggerAdditionalData + standardAdditionalDataSource.getAdditionalData() + data

        return LogMessage(severity, message, timestampSource(), additionalData)
    }

    fun data(key: String, value: String) = data(key, value, String.serializer())
    fun data(key: String, value: Int) = data(key, value, Int.serializer())
    fun data(key: String, value: Long) = data(key, value, Long.serializer())
    fun data(key: String, value: Boolean) = data(key, value, Boolean.serializer())
    fun data(key: String, value: ZonedDateTime) = data(key, value, ZonedDateTimeSerializer)
    fun data(key: String, value: Path) = data(key, value.toString())
    fun data(key: String, value: Iterable<String>) = data(key, value.toList(), String.serializer().list)
    fun data(key: String, value: Map<String, String>) = data(key, value, MapSerializer(String.serializer(), String.serializer()))

    @JvmName("nullableData")
    fun data(key: String, value: String?) = data(key, value, String.serializer().nullable)

    @JvmName("nullableData")
    fun data(key: String, value: ZonedDateTime?) = data(key, value, ZonedDateTimeSerializer.nullable)
}
