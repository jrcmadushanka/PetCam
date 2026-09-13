package com.civdevops.petcam.data.audio.catalog

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.civdevops.petcam.core.model.audio.PetSoundCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StarterBundledPetSoundCatalogTest {

    @Test
    fun generatedCatalogContainsValidBundledSounds() {
        val context =
            ApplicationProvider.getApplicationContext<android.content.Context>()

        val catalog = StarterBundledPetSoundCatalog(
            BundledAudioDurationReader(context)
        )

        assertTrue(catalog.entries.isNotEmpty())
        assertTrue(catalog.entries.all { it.rawResourceId != 0 })
        assertTrue(catalog.entries.all { it.durationMillis > 0 })
        assertTrue(catalog.entries.all {
            it.sound.id.rawValue.startsWith("${catalog.pack.id.rawValue}:")
        })

        val ids = catalog.entries.map { it.sound.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun starterCatalogContainsExpectedCategories() {
        val context =
            ApplicationProvider.getApplicationContext<android.content.Context>()

        val catalog = StarterBundledPetSoundCatalog(
            BundledAudioDurationReader(context)
        )

        val categories = catalog.entries.map { it.sound.category }.toSet()

        assertTrue(PetSoundCategories.Dogs in categories)
        assertTrue(PetSoundCategories.Cats in categories)
    }
}