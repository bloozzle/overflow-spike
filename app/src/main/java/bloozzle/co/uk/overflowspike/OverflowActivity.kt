package bloozzle.co.uk.overflowspike

import android.arch.lifecycle.*
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*

import kotlinx.android.synthetic.main.activity_overflow.*
import kotlinx.android.synthetic.main.layout_list_view.*
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import android.widget.TextView
import kotlinx.android.synthetic.main.layout_overflow_list_item.view.*
import org.w3c.dom.Text


class OverflowActivity : AppCompatActivity(), OverflowView {
    private lateinit var overflowController: OverflowController

    override fun showLoading() {
        view_switcher.showPrevious()
    }

    override fun showList(items: List<OverflowUIItem>) {
        val view = layoutInflater.inflate(R.layout.layout_list_view, null)


        view_switcher.addView(view)
        recycler_view.layoutManager = GridLayoutManager(this, 2)
        recycler_view.adapter = OverflowListAdapter(items) {
            overflowController.itemSelected(items.indexOf(it))
        }
        view_switcher.showNext()
    }

    override fun showError(message: String) {
       val view =  layoutInflater.inflate(R.layout.layout_error_view, null )
        view_switcher.addView(view)
        view_switcher.showNext()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overflow)
        setSupportActionBar(toolbar)

        val overflowPresenter = OverflowPresenter(this )
        val overflowViewModelFactory = OverflowViewModelFactory(OverflowParameters( "watching", OverflowType.USER))
        val overflowViewModel = ViewModelProviders.of(this, overflowViewModelFactory).get(OverflowViewModel::class.java);
        overflowController = OverflowController(overflowViewModel)


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

class OverflowListAdapter(val items : List<OverflowUIItem>, val listener : (OverflowUIItem) -> Unit) : RecyclerView.Adapter<OverflowListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: OverflowUIItem, listener: (OverflowUIItem) -> Unit) = with(itemView) {
            heading1.text = item.heading1
            heading2.text = item.heading2
            setOnClickListener { listener(item) }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder  = ViewHolder(parent.inflate(R.layout.layout_overflow_list_item))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position], listener)


}



private fun ViewGroup.inflate(layoutResource: Int): View {
    return LayoutInflater.from(context).inflate(layoutResource, this, false)
}

class OverflowController(val viewModel: OverflowViewModel) {

    fun loadOverflowItems(lifecycleOwner: LifecycleOwner, observer: Observer<OverflowDataState>) {
        viewModel.getOverflowItems().observe(lifecycleOwner, observer)
    }

    fun itemSelected(position: Int) {

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