package com.civdevops.petcam.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GeneratePetSoundCatalogTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val rawDirectory: DirectoryProperty

    @get:Input
    abstract val namespace: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val directory = rawDirectory.get().asFile
        val supportedExtensions = setOf("mp3", "ogg", "wav")
        val namePattern = Regex("^(dog|cat|whistle|toy|other)_[a-z0-9]+(?:_[a-z0-9]+)*$")

        val files = directory.listFiles()
            ?.filter { it.isFile }
            ?.sortedBy { it.name }
            .orEmpty()

        if (files.isEmpty()) {
            throw GradleException("Pet Cam requires at least one bundled attention sound in data/audio/src/main/res/raw.")
        }

        val entries = files.map { file ->
            val extension = file.extension.lowercase()

            if (extension !in supportedExtensions) {
                throw GradleException(
                    "Unsupported bundled pet sound '${file.name}'. Supported extensions: mp3, ogg, wav."
                )
            }

            val assetKey = file.nameWithoutExtension

            if (!namePattern.matches(assetKey)) {
                throw GradleException(
                    "Invalid pet sound '$assetKey'. Expected: dog_*, cat_*, whistle_*, toy_* or other_* using lowercase letters, digits and underscores."
                )
            }

            GeneratedEntry(
                assetKey = assetKey,
                category = categoryFor(assetKey.substringBefore('_')),
                displayName = displayNameFor(assetKey)
            )
        }

        val duplicates = entries.groupBy { it.assetKey }.filterValues { it.size > 1 }.keys

        if (duplicates.isNotEmpty()) {
            throw GradleException(
                "Duplicate bundled pet sound IDs: ${
                    duplicates.sorted().joinToString()
                }"
            )
        }

        writeCatalog(entries)
    }

    private fun categoryFor(prefix: String): String {
        return when (prefix) {
            "dog" -> "dogs"
            "cat" -> "cats"
            "whistle" -> "whistles"
            "toy" -> "toys"
            "other" -> "other"
            else -> error("Validated prefix expected.")
        }
    }

    private fun displayNameFor(assetKey: String): String {
        return assetKey.substringAfter('_')
            .split('_')
            .joinToString(" ") { token ->
                token.replaceFirstChar { char -> char.uppercaseChar() }
            }
    }

    private fun writeCatalog(entries: List<GeneratedEntry>) {
        val packageName = "${namespace.get()}.generated"
        val outputRoot = outputDirectory.get().asFile

        outputRoot.deleteRecursively()

        val packageDirectory = outputRoot.resolve(packageName.replace('.', '/'))
        packageDirectory.mkdirs()

        val entriesSource = if (entries.isEmpty()) {
            "emptyList()"
        } else {
            entries.joinToString(
                prefix = "listOf(\n",
                postfix = "\n    )",
                separator = ",\n"
            ) { entry ->
                """        GeneratedBundledPetSound("${entry.assetKey}", "${entry.category}", "${entry.displayName}", R.raw.${entry.assetKey})"""
            }
        }

        packageDirectory.resolve("GeneratedBundledPetSounds.kt").writeText(
            """
            package $packageName

            import ${namespace.get()}.R

            internal data class GeneratedBundledPetSound(
                val assetKey: String,
                val category: String,
                val displayName: String,
                val rawResourceId: Int
            )

            internal object GeneratedBundledPetSounds {
                val entries: List<GeneratedBundledPetSound> = $entriesSource
            }
            """.trimIndent()
        )
    }

    private data class GeneratedEntry(
        val assetKey: String,
        val category: String,
        val displayName: String
    )
}