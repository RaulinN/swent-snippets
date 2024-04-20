// Group 14 : What is dependency injection?

// data layer. Assume we have an Interface `Animal`
interface Animal {}
// data layer. and a class `Cat` (which is an animal)
data class Cat(val name: String): Animal
data class Dog(val name: String): Animal

// presentation layer. and a view model (that for some
// reason takes an animal)
class MyViewModel(
    animal: Animal,
): ViewModel() {
    // ...
}



/* ----------- what is dependency injection -------------- */

val name: String = "Silver"
val cat: Cat = Cat(name) // 1

val vm: MyViewModel = viewModel {
    MyViewModel(cat) // 2
}

// You just performed dependency injection twice. In your
// case, you want to inject database implementations, but
// the idea is similar. This is called manual dependency 
// injection:
// - 1 : injected the string "Silver" in the Cat constructor
// - 2 : injected the cat in the viewmodel. You can do that
//          with androidx.lifecycle:lifecycle-viewmodel-compose
//          if your version is recent (>= 2.6.2)



/* ------- automated dependency injection using dagger hilt ----- */

// Alternatively, you may also use Hilt for dependency 
// injection. Here's how it could look like

@Module
@InstalledIn(SingletonComponent::class) // lifetime of the dependency
object AppModule {
    
    // Explains to Hilt how to create an instance of an "Animal". Every
    // time you tell Hilt to inject an animal, it will inject a cat
    // called Midna
    @Provides
    fun provideAnimal(): Animal {
        // Yes I am a fan of Twilight Princess, deal with it ^^ 
        return Cat("Midna")
    }
}

// Use the @Inject annotation when you want Hilt to inject somehting
// and @HiltViewModel for view models specifically
@HiltViewModel
class MyViewModel @Inject constructor(
    private val animal: Animal
): ViewModel() {
    // ...
}

// Now, in your app, you can simply use the following notation to
// create a viewmodel
val vm = hiltViewModel<MyViewModel>() // the animal is a cat called midna



/* ---------------------- App vs Tests -------------------- */

// Note: other important type annotations you may find useful : 
// @Singleton, @Named, and @Binds

interface IDatabase {}

class NetworkDatabase: IDatabase {}
class InMemoryDatabase: IDatabase {}

@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: IDatabase
): Viewmodel() { /* ... */ }


// Use multiple modules depending on the context, for example

@Module
@InstalledIn(ApplicationComponent::class)
abstract class AppModule {
    
    @Binds
    abstract fun bindDatabase(impl: NetworkDatabase): IDatabase

    @Provides
    @Singleton
    fun provideNetworkDatabase(): NetworkDatabase {
        return NetworkDatabase()
    }
}

@Module
@InstalledIn(ApplicationComponent::class)
abstract class TestModule {
    
    @Binds
    abstract fun bindDatabase(impl: InMemoryDatabase): IDatabase

    @Provides
    @Singleton
    fun provideInMemoryDatabase(): InMemoryDatabase {
        return InMemoryDatabase()
    }
}
