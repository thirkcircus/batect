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

package batect.telemetry

import batect.docker.DockerHttpConfig
import batect.docker.client.DockerConnectivityCheckResult
import batect.execution.CacheManager

class DockerTelemetryCollector(
    private val dockerHttpConfig: DockerHttpConfig,
    private val cacheManager: CacheManager,
    private val telemetrySessionBuilder: TelemetrySessionBuilder
) {
    fun collectTelemetry(result: DockerConnectivityCheckResult.Succeeded) {
        telemetrySessionBuilder.addAttribute("dockerContainerType", result.containerType.toString())
        telemetrySessionBuilder.addAttribute("dockerConnectionType", dockerHttpConfig.connectionType.toString())
        telemetrySessionBuilder.addAttribute("dockerVersion", result.dockerVersion.toString())
        telemetrySessionBuilder.addAttribute("cacheType", cacheManager.cacheType.toString())
    }
}
