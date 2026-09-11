import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.civdevops.petcam.buildlogic.GeneratePetSoundCatalogTask

pluginManager.withPlugin("com.android.library") {
    val androidComponents = extensions.getByType<LibraryAndroidComponentsExtension>()

    androidComponents.onVariants(androidComponents.selector().all()) { variant ->
        val variantName = variant.name.replaceFirstChar { it.uppercaseChar() }

        val taskProvider = tasks.register<GeneratePetSoundCatalogTask>(
            "generate${variantName}PetSoundCatalog"
        ) {
            rawDirectory.set(layout.projectDirectory.dir("src/main/res/raw"))
            namespace.set("com.civdevops.petcam.data.audio")
        }

        requireNotNull(variant.sources.kotlin)
            .addGeneratedSourceDirectory(taskProvider) { it.outputDirectory }
    }
}