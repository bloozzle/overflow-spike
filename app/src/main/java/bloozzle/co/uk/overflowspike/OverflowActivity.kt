package bloozzle.co.uk.overflowspike

import android.arch.lifecycle.*
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View

import kotlinx.android.synthetic.main.activity_overflow.*
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch


class OverflowActivity : AppCompatActivity(), OverflowView {

    override fun showLoading() {
        loading.visibility = View.VISIBLE
    }

    override fun showList(items: List<OverflowUIItem>) {
        loading.visibility = View.GONE
        list.visibility = View.VISIBLE
    }

    override fun showError(message: String) {
        loading.visibility = View.GONE
        list.visibility = View.GONE
        error.visibility = View.VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overflow)
        setSupportActionBar(toolbar)

        val overflowPresenter = OverflowPresenter(this )
        val overflowViewModelFactory = OverflowViewModelFactory(OverflowParameters( "watching", OverflowType.USER))
        val overflowViewModel = ViewModelProviders.of(this, overflowViewModelFactory).get(OverflowViewModel::class.java);
        val overflowController = OverflowController(overflowViewModel)


        overflowController.loadOverflowItems(this, overflowPresenter)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_overflow, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}

class OverflowController(val viewModel: OverflowViewModel) {
    fun loadOverflowItems(lifecycleOwner: LifecycleOwner, observer: Observer<OverflowDataState>) {
        viewModel.getOverflowItems().observe(lifecycleOwner, observer)
    }

}

class OverflowViewModelFactory(val overflowParameters: OverflowParameters) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {

      if(overflowParameters.id == "watching" && overflowParameters.overflowType == OverflowType.USER ){
           if (modelClass.isAssignableFrom(OverflowViewModel::class.java)) {
               return OverflowViewModel(WatchingRepository() ) as T
           }
       }
        throw IllegalArgumentException("Unknown ViewModel class");

    }

}



class WatchingRepository {
    fun getOverflowItems(): WatchingResponse {

        return WatchingSuccess(listOf(
                         WatchingItem("item1", "title 1", "subtitle 1", 10),
                         WatchingItem("item2", "title 2", "subtitle 2", 12),
                         WatchingItem("item3", "title 3", "subtitle 3", 5)))

             }
        }




sealed class WatchingResponse
    data class WatchingSuccess(val items: List<WatchingItem>) : WatchingResponse()
    data class WatchingError(val message: String) : WatchingResponse()

data class WatchingItem(val id: String, val title: String, val subtitle : String, val progress: Int)

enum class OverflowType {
    USER

}

class OverflowPresenter(val overflowView: OverflowView) : Observer<OverflowDataState> {
    override fun onChanged(overflowDataState: OverflowDataState?) {
        when(overflowDataState) {
            is Loading -> overflowView.showLoading()
            is Success -> overflowView.showList(overflowDataState.items.transformToUIItems())
            is Error -> overflowView.showError(overflowDataState.message)

        }
    }


}

private fun List<OverflowItem>.transformToUIItems(): List<OverflowUIItem> {
    var overflowUIItems = ArrayList<OverflowUIItem>()
    for (item in this) {
        overflowUIItems.add(item.transformToOverflowUiItem())
    }
    return overflowUIItems

}

private fun OverflowItem.transformToOverflowUiItem(): OverflowUIItem {
    return OverflowUIItem(title, subtitle)
}

interface OverflowView {
    fun showLoading()
    fun showList(items: List<OverflowUIItem>)
    fun showError(message: String)
}


class OverflowViewModel(val watchingRepository: WatchingRepository) : ViewModel() {

    private val overflowDataState =  MutableLiveData<OverflowDataState>()

    fun getOverflowItems() : LiveData<OverflowDataState> {
        overflowDataState.value = Loading()
        launch {
            delay(10000L)
            val watchingResponse = watchingRepository.getOverflowItems()
            when(watchingResponse) {
                is WatchingSuccess -> overflowDataState.postValue(Success(watchingResponse.items.transformToOverflowModel()))
                is WatchingError -> overflowDataState.postValue(Error(watchingResponse.message))
            }
        }
        return overflowDataState

    }

}

private fun List<WatchingItem>.transformToOverflowModel(): List<OverflowItem> {
    var overflowItems = ArrayList<OverflowItem>()
    for (item in this) {
       overflowItems.add(item.transformToOverflowItem())
    }
    return overflowItems
}

private fun WatchingItem.transformToOverflowItem(): OverflowItem {
    return OverflowItem(id, title, subtitle)
}

data class OverflowItem(val id : String, val title: String, val subtitle: String)

sealed class OverflowDataState
class Loading : OverflowDataState()
data class Success(val items : List<OverflowItem>) : OverflowDataState()
data class Error(val message : String) : OverflowDataState()

data class OverflowParameters(val id : String, val overflowType : OverflowType)
data class OverflowUIItem(val heading1 : String, val heading2 : String)