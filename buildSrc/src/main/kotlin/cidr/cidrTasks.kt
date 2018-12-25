/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cidr

import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import java.util.regex.Pattern

fun Zip.includePatched(fileToMarkers: Map<String, List<String>>) {
    val notDone = mutableSetOf<Pair<String, String>>()
    fileToMarkers.forEach { (path, markers) ->
        for (marker in markers) {
            notDone += path to marker
        }
    }

    eachFile {
        val markers = fileToMarkers[this.sourcePath] ?: return@eachFile
        this.filter {
            var data = it
            for (marker in markers) {
                val newData = data.replace(("^(.*" + Pattern.quote(marker) + ".*)$").toRegex(), "<!-- $1 -->")
                data = newData
                notDone -= path to marker
            }
            data
        }
    }
    doLast {
        check(notDone.size == 0) {
            "Filtering failed for: " +
                    notDone.joinToString(separator = "\n") { (file, marker) -> "file=$file, marker=`$marker`" }
        }
    }
}

fun Zip.includePatchedJavaXmls() {
    val javaPsiXmlPath = "META-INF/JavaPsiPlugin.xml"
    val javaPluginXmlPath = "META-INF/JavaPlugin.xml"

    includePatched(
        mapOf(
            javaPsiXmlPath to listOf("implementation=\"org.jetbrains.uast.java.JavaUastLanguagePlugin\""),
            javaPluginXmlPath to listOf("implementation=\"com.intellij.spi.SPIFileTypeFactory\"",
                                        "implementationClass=\"com.intellij.lang.java.JavaDocumentationProvider\"")
        )
    )
}

fun Copy.applyCidrVersionRestrictions(
    productVersion: String,
    strictProductVersionLimitation: Boolean,
    pluginVersion: String
) {
    val dotsCount = productVersion.count { it == '.' }
    check(dotsCount >= 1 && dotsCount <= 2) {
        "Wrong CIDR product version format: $productVersion"
    }

    val sinceBuild = if (dotsCount == 1)
        productVersion
    else
        productVersion.substringBeforeLast('.')

    val untilBuild = if (strictProductVersionLimitation)
        // if strict then restrict plugin to the same single version of CLion or AppCode
        sinceBuild + ".*"
    else
        productVersion.substringBefore('.') + ".*"

    filter {
        it
            .replace("<!--idea_version_placeholder-->",
                     "<idea-version since-build=\"$sinceBuild\" until-build=\"$untilBuild\"/>")
            .replace("<!--version_placeholder-->",
                     "<version>$pluginVersion</version>")
    }
}
