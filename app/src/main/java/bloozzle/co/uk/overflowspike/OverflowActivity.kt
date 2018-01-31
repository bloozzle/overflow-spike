package bloozzle.co.uk.overflowspike

import android.arch.lifecycle.*
import android.opengl.Visibility
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View

import kotlinx.android.synthetic.main.activity_overflow.*


class OverflowActivity : AppCompatActivity(), OverflowView {
    override fun showLoading() {
        loading.visibility = View.VISIBLE
    }

    override fun showList(items: List<OverflowUiItem>) {
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

        overflowViewModel.getOverflowItems().observe(this, overflowPresenter)

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
        Thread.sleep(10000)
        return WatchingSuccess(listOf(
                WatchingItem("item1", "title 1", "subtitle 1", 10 ),
                WatchingItem("item2", "title 2", "subtitle 2", 12 ),
                WatchingItem("item3", "title 3", "subtitle 3", 5 )))
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
            is Success -> overflowView.showList(overflowDataState.items)
            is Error -> overflowView.showError(overflowDataState.message)

        }
    }


}

interface OverflowView {
    fun showLoading()
    fun showList(items: List<OverflowUiItem>)
    fun showError(message: String)
}


class OverflowViewModel(val watchingRepository: WatchingRepository) : ViewModel() {

    private val overflowDataState =  MutableLiveData<OverflowDataState>()

    fun getOverflowItems() : LiveData<OverflowDataState> {
        overflowDataState.value = Loading()
        val watchingResponse = watchingRepository.getOverflowItems()
        when(watchingResponse) {
            is WatchingSuccess -> overflowDataState.value = Success(watchingResponse.items.transformToUIModel())
            is WatchingError -> overflowDataState.value = Error(watchingResponse.message)
        }
        return overflowDataState

    }

}

private fun List<WatchingItem>.transformToUIModel(): List<OverflowUiItem> {
    var uiItems = ArrayList<OverflowUiItem>()
    for (item in this) {
       uiItems.add(item.transformToUiItem())
    }
    return uiItems
}

private fun WatchingItem.transformToUiItem(): OverflowUiItem {
    return OverflowUiItem(title, subtitle)
}


sealed class OverflowDataState
class Loading : OverflowDataState()
data class Success(val items : List<OverflowUiItem>) : OverflowDataState()
data class Error(val message : String) : OverflowDataState()

data class OverflowParameters(val id : String, val overflowType : OverflowType)
data class OverflowUiItem(val heading1 : String, val heading2 : String)