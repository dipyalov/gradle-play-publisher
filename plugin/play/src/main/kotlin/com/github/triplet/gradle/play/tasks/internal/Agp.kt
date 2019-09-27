package com.github.triplet.gradle.play.tasks.internal

import com.android.build.api.artifact.ArtifactType
import com.android.build.gradle.internal.api.InstallableVariantImpl
import com.android.build.gradle.internal.scope.InternalArtifactType
import com.github.triplet.gradle.play.internal.orNull
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFile
import java.io.File

fun PublishTaskBase.findBundleFile(): File? {
    val customDir = extension.config.artifactDir

    return if (customDir == null) {
        val installable = variant as InstallableVariantImpl

        // TODO(#708): remove when AGP 3.6 is the minimum
        fun getFinalArtifactCompat(): Set<File> = try {
            @Suppress("UNCHECKED_CAST") // Incorrect generics
            installable.getFinalArtifact(
                    InternalArtifactType.BUNDLE as ArtifactType<FileSystemLocation>
            ).get().files
        } catch (e: NoSuchMethodError) {
            val artifact = installable.javaClass
                    .getMethod("getFinalArtifact", ArtifactType::class.java)
                    .invoke(installable, InternalArtifactType.BUNDLE)
            @Suppress("UNCHECKED_CAST")
            artifact.javaClass.getMethod("getFiles").apply {
                isAccessible = true
            }.invoke(artifact) as Set<File>
        }

        installable.variantData.scope.artifacts
                .getFinalProduct<RegularFile>(InternalArtifactType.BUNDLE)
                .get().asFile.orNull() ?: getFinalArtifactCompat().singleOrNull()
    } else if (customDir.isFile && customDir.extension == "aab") {
        customDir
    } else {
        val bundles = customDir.listFiles().orEmpty().filter { it.extension == "aab" }
        if (bundles.isEmpty()) {
            logger.warn("Warning: '$customDir' does not yet contain an App Bundle.")
        } else if (bundles.size > 1) {
            logger.warn("Warning: '$customDir' contains multiple App Bundles. Only one is allowed.")
        }
        bundles.singleOrNull()
    }
}