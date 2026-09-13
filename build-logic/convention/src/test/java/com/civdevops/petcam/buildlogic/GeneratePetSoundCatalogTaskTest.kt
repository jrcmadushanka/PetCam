package com.civdevops.petcam.buildlogic

import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GeneratePetSoundCatalogTaskTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `valid sound filename generates catalogue entry`() {
        val rawDirectory = temporaryFolder.newFolder("valid-raw")
        val outputDirectory = temporaryFolder.newFolder("valid-output")

        rawDirectory.resolve("dog_excited_bark.mp3").writeText("test")

        val task = createTask(rawDirectory, outputDirectory)
        task.generate()

        val generatedFile = generatedFile(outputDirectory)
        val generatedSource = generatedFile.readText()

        assertContains(
            generatedSource,
            """GeneratedBundledPetSound("dog_excited_bark", "dogs", "Excited Bark", R.raw.dog_excited_bark)"""
        )
        assertContains(generatedSource, "R.raw.dog_excited_bark")
    }

    @Test
    fun `invalid sound filename fails with clear error`() {
        val rawDirectory = temporaryFolder.newFolder("invalid-raw")
        val outputDirectory = temporaryFolder.newFolder("invalid-output")

        rawDirectory.resolve("Dog Bark.mp3").writeText("test")

        val failure = assertFailsWith<GradleException> {
            createTask(rawDirectory, outputDirectory).generate()
        }

        assertContains(failure.message.orEmpty(), "Invalid pet sound")
        assertContains(failure.message.orEmpty(), "lowercase letters, digits and underscores")
    }

    @Test
    fun `unsupported sound extension fails with clear error`() {
        val rawDirectory = temporaryFolder.newFolder("unsupported-raw")
        val outputDirectory = temporaryFolder.newFolder("unsupported-output")

        rawDirectory.resolve("dog_bark.aac").writeText("test")

        val failure = assertFailsWith<GradleException> {
            createTask(rawDirectory, outputDirectory).generate()
        }

        assertContains(failure.message.orEmpty(), "Unsupported bundled pet sound")
        assertContains(failure.message.orEmpty(), "mp3, ogg, wav")
    }

    @Test
    fun `duplicate sound identifiers fail with clear error`() {
        val rawDirectory = temporaryFolder.newFolder("duplicate-raw")
        val outputDirectory = temporaryFolder.newFolder("duplicate-output")

        rawDirectory.resolve("cat_meow.mp3").writeText("test")
        rawDirectory.resolve("cat_meow.wav").writeText("test")

        val failure = assertFailsWith<GradleException> {
            createTask(rawDirectory, outputDirectory).generate()
        }

        assertContains(failure.message.orEmpty(), "Duplicate bundled pet sound IDs")
        assertContains(failure.message.orEmpty(), "cat_meow")
    }

    @Test
    fun `empty raw directory fails with clear error`() {
        val rawDirectory = temporaryFolder.newFolder("empty-raw")
        val outputDirectory = temporaryFolder.newFolder("empty-output")

        val failure = assertFailsWith<GradleException> {
            createTask(rawDirectory, outputDirectory).generate()
        }

        assertContains(failure.message.orEmpty(), "at least one bundled attention sound")
    }

    private fun createTask(
        rawDirectory: File,
        outputDirectory: File
    ): GeneratePetSoundCatalogTask {
        val project = ProjectBuilder.builder().build()

        return project.tasks.create(
            "generatePetSoundCatalog",
            GeneratePetSoundCatalogTask::class.java
        ).apply {
            this.rawDirectory.set(rawDirectory)
            this.outputDirectory.set(outputDirectory)
            namespace.set("com.civdevops.petcam.data.audio")
        }
    }

    private fun generatedFile(outputDirectory: File): File {
        return outputDirectory.resolve(
            "com/civdevops/petcam/data/audio/generated/GeneratedBundledPetSounds.kt"
        )
    }
}