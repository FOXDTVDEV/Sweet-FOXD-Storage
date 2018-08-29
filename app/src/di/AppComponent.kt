package fr.rhaz.ipfs.sweet.di

import dagger.Component
import fr.rhaz.ipfs.sweet.activities.DetailsActivity
import fr.rhaz.ipfs.sweet.activities.HashTextAndBarcodeActivity
import fr.rhaz.ipfs.sweet.activities.MainActivity
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {
    fun inject(mainActivity: MainActivity)

    fun inject(addIPFSContent: HashTextAndBarcodeActivity)

    fun inject(detailsActivity: DetailsActivity)

}
