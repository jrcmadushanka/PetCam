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
    fun starterCatalogContainsPlayableSoundForEveryCategory() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val catalog = StarterBundledPetSoundCatalog(
            BundledAudioDurationReader(context)
        )

        val categories = catalog.entries.map { it.sound.category }.toSet()

        assertTrue(PetSoundCategories.values.all { it in categories })
        assertEquals(5, catalog.entries.size)
        assertTrue(catalog.entries.all { it.rawResourceId != 0 })
        assertTrue(catalog.entries.all { it.durationMillis > 0 })
    }
}