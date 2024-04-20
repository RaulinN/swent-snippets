// Group 11 : example of MVVM + flows
// Group 14 : example dependency injection w/ database

// NOTE: same as before, this time using Hilt

// data layer -> User.kt
data class User(val name: String, val age: Int)

// domain layer -> database interface. You can then use this
// in your view model 
interface IDatabase {
    suspend fun getUser(userId: String): User?
    suspend fun addUser(userId: String, user: User)
}

// data layer. You can use this db in the tests
class InMemoryDatabase: IDatabase {
    private val storage: MutableMap<String, User> = mutableMapOf()

    override fun getUser(userId: String): User? {
        return storage[userId]
    }

    override fun addUser(userId: String, user: User) {
        storage[userId] = user
    }
}

// data layer. You can use this db in the real app
// This is where all the ugly firestore code lies. The
// advantage, as discussed yesterday, is that you iwll
// only have to do this once (in this file I mean)
class NetworkDatabase: IDatabase {
    override fun getUser(userId: String): User? {
        return db.collection('users').get() // incomplete
    }

    override fun addUser(userId: String, user: User) {
        db.collection('users').add() // incomplete
    }
}


/* Now let's say we want to create an App with one screen:
This screen will be called the "OverviewScreen", associated
to "OverviewViewModel" and based on "OverviewState" */

// generally placed in a folder called di
@Module
@InstallIn(SingletonComponent::class) // lifetime of the dependency
object AppModule {
    @Provides
    @Singleton // scope => single instance
    fun provideDatabase(): IDatabase {
        return NetworkDatabase()
    }
}

// presentation layer : model
data class OverviewState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = true,
)

// presentation layer : view model
@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val db: IDatabase
): Viewmodel() {
    private val _state = MutableStateFlow(OverviewState())
    val state = _state.asStateFlow()

    // [Group 11] : this creates a StateFlow (just like
    // state above) that will change and trigger UI re-
    // composition ONLY when state.value.users changed
    val adultsFlow = state.users.filter { 
        user -> user.age >= 18
    }.stateIn(viewModelScope, SharingStarted.Lazily, state.value.users)

    // [Group 11]
    // example : using .update to update multiple fields of
    // the state at once. This will trigger a single
    // UI recomposition instead of n if you update n fields
    fun onFetchUsers() {
        _state.update(it.copy(isLoading = true))

        // [Group 14]
        // here, if you are in the real app, Hilt (or your)
        // manual injection will provide the NetworkDatabase
        // If you are in the tests, it should use the
        // InMemoryDatabase instead
        val users: List<User> = db.getUsers()

        // triggers one recomposition instead of 2
        _state.update { it.copy(
            isLoading = false,
            users = users,
        )}
    }

    fun onButtonClick() { /* ... */ }
}

// presentation layer : view
@Composable
fun OverviewScreen(
    state: OverviewState,
    onButtonClick: () -> Unit,
) {
    // ...
}

// using the view (e.g. in MainActivity.kt > in NavHost)
composable(Destinations.OVERVIEW) {
    // Notice that you don't need to pass the db
    val vm = hiltViewModel<OverviewViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()

    OverviewScreen(state, vm::onButtonClick)
}
