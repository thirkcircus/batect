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

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.builtins.serializer

object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    private val formatter = DateTimeFormatter.ISO_DATE_TIME

    override val descriptor: SerialDescriptor = String.serializer().descriptor
    override fun deserialize(decoder: Decoder): ZonedDateTime = ZonedDateTime.parse(decoder.decodeString(), formatter)
    override fun serialize(encoder: Encoder, value: ZonedDateTime) = encoder.encodeString(value.format(formatter))
}
