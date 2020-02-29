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

package batect.ui

import batect.logging.LogMessageBuilder
import batect.logging.Logger
import batect.os.HostEnvironmentVariables
import batect.os.NativeMethods
import batect.os.OperatingSystem
import batect.os.SystemInfo
import batect.os.data
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.internal.StringSerializer

@Serializable(with = ConsoleInfo.Companion::class)
class ConsoleInfo(
    private val nativeMethods: NativeMethods,
    private val systemInfo: SystemInfo,
    private val environment: HostEnvironmentVariables,
    private val logger: Logger
) {
    val stdinIsTTY: Boolean by lazy {
        val result = nativeMethods.determineIfStdinIsTTY()

        logger.info {
            message("Called 'isatty' to determine if STDIN is a TTY.")
            data("result", result)
        }

        result
    }

    val stdoutIsTTY: Boolean by lazy {
        val result = nativeMethods.determineIfStdoutIsTTY()

        logger.info {
            message("Called 'isatty' to determine if STDOUT is a TTY.")
            data("result", result)
        }

        result
    }

    val supportsInteractivity: Boolean by lazy {
        logger.info {
            message("Checking if terminal supports interactivity.")
            data("stdoutIsTTY", stdoutIsTTY)
            data("terminalType", terminalType)
            data("isTravis", isTravis)
            data("operatingSystem", systemInfo.operatingSystem)
        }

        stdoutIsTTY && !isTravis && terminalType != "dumb" && (systemInfo.operatingSystem == OperatingSystem.Windows || terminalType != null)
    }

    val terminalType: String? = environment["TERM"]
    private val isTravis: Boolean = environment["TRAVIS"] == "true"

    companion object : KSerializer<ConsoleInfo> {
        private const val stdinIsTTYFieldName = "stdinIsTTY"
        private const val stdoutIsTTYFieldName = "stdoutIsTTY"
        private const val supportsInteractivityFieldName = "supportsInteractivity"
        private const val terminalTypeFieldName = "terminalType"

        override val descriptor: SerialDescriptor = object : SerialClassDescImpl("Configuration") {
            init {
                addElement(stdinIsTTYFieldName)
                addElement(stdoutIsTTYFieldName)
                addElement(supportsInteractivityFieldName)
                addElement(terminalTypeFieldName)
            }
        }

        private val stdinIsTTYFieldIndex = descriptor.getElementIndex(stdinIsTTYFieldName)
        private val stdoutIsTTYFieldIndex = descriptor.getElementIndex(stdoutIsTTYFieldName)
        private val supportsInteractivityFieldIndex = descriptor.getElementIndex(supportsInteractivityFieldName)
        private val terminalTypeFieldIndex = descriptor.getElementIndex(terminalTypeFieldName)

        override fun deserialize(decoder: Decoder): ConsoleInfo = throw UnsupportedOperationException()

        override fun serialize(encoder: Encoder, obj: ConsoleInfo) {
            val output = encoder.beginStructure(descriptor)
            output.encodeBooleanElement(descriptor, stdinIsTTYFieldIndex, obj.stdinIsTTY)
            output.encodeBooleanElement(descriptor, stdoutIsTTYFieldIndex, obj.stdoutIsTTY)
            output.encodeBooleanElement(descriptor, supportsInteractivityFieldIndex, obj.supportsInteractivity)
            output.encodeNullableSerializableElement(descriptor, terminalTypeFieldIndex, StringSerializer, obj.terminalType)
            output.endStructure(descriptor)
        }
    }
}

fun LogMessageBuilder.data(key: String, value: ConsoleInfo) = this.data(key, value, ConsoleInfo.serializer())
