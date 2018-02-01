package bloozzle.co.uk.overflowspike

import android.arch.lifecycle.*
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import kotlinx.android.synthetic.main.activity_overflow.*
import kotlinx.android.synthetic.main.layout_list_view.*
import kotlinx.android.synthetic.main.layout_list_item.view.*
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch


class OverflowActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overflow)
        setSupportActionBar(toolbar)



        val router = object : DetailsPageRouter {

            override fun navigate(id: String) {
                val intent = Intent(this@OverflowActivity, DetailsPageActivity::class.java)
                intent.putExtra("id", id)
                startActivity(intent)
            }

        }
        val overflowViewModelFactory = OverflowViewModelFactory(OverflowParameters( "watching", OverflowType.USER), router)
        val overflowViewModel = ViewModelProviders.of(this, overflowViewModelFactory).get(OverflowViewModel::class.java);
        overflowViewModel.overflowViewState.observe(this, object : Observer<OverflowViewState> {
//          override fun showLoading() {
//            view_switcher.showPrevious()
//        }
//
//                override fun showList(items: List<OverflowUIItem>) {
//            val view = layoutInflater.inflate(R.layout.layout_list_view, null)
//
//
//            view_switcher.addView(view)
//            recycler_view.layoutManager = GridLayoutManager(this@OverflowActivity, 2)
//            recycler_view.adapter = OverflowListAdapter(items) {
//                viewListener?.itemSelected(items.indexOf(it))
//            }
//            view_switcher.showNext()
//        }
//
//                override fun showError(message: String) {
//            val view =  layoutInflater.inflate(R.layout.layout_error_view, null )
//            view_switcher.addView(view)
//            view_switcher.showNext()
//        }
        })


        overflowViewModel


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

interface DetailsPageRouter {
    fun navigate(id: String)

}

class OverflowListAdapter(val items : List<OverflowUIItem>, val listener : (OverflowUIItem) -> Unit) : RecyclerView.Adapter<OverflowListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: OverflowUIItem, listener: (OverflowUIItem) -> Unit) = with(itemView) {
            heading1.text = item.heading1
            heading2.text = item.heading2
            setOnClickListener { listener(item) }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder  = ViewHolder(parent.inflate(R.layout.layout_list_item))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position], listener)


}



private fun ViewGroup.inflate(layoutResource: Int): View {
    return LayoutInflater.from(context).inflate(layoutResource, this, false)
}

class OverflowController(val dataInteractor: DataInteractor, val viewModel: OverflowModel, val router : DetailsPageRouter ) : Observer<OverflowViewState>, OverflowViewListener {

    private lateinit var overflowItems: List<OverflowItem>

    override fun onChanged(overflowViewState: OverflowViewState?) {
        when(overflowViewState) {
            is Success -> overflowItems = overflowViewState.items


        }
    }

    fun loadOverflowItems() {
        viewModel.getOverflowItems()

    }

    override fun itemSelected(position: Int) {
       //TODO it might be better to query the model rather than observe changes?, e.g   viewModel.getItemAt(position)
        val overflowItem = overflowItems.get(position)
        router.navigate(overflowItem.id )
    }

}

class OverflowViewModelFactory(val overflowParameters: OverflowParameters, val router : DetailsPageRouter) : ViewModelProvider.Factory {

    override fun <T: ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(OverflowViewModel::class.java) -> create()
            else -> throw IllegalArgumentException("Unknown model class")
        } as T
    }

    fun  create(): OverflowViewModel {
        val presenter = OverflowPresenter()
        val model = OverflowModel(presenter)

        val repository = WatchingRepository()
//        when(overflowParameters.id == "watching" && overflowParameters.overflowType == OverflowType.USER ){
//
//            repository = WatchingRepository()
//
//        }


        val dataInteractor = DataInteractor(repository, model)
        val controller = OverflowController(dataInteractor, model, router)

        val viewModel = OverflowViewModel(controller)
        presenter.addOverflowView(viewModel)



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

class OverflowPresenter() : OverflowModelListener {

    private var view: OverflowView? = null

    fun addOverflowView(overflowView: OverflowView) {
        view = overflowView
    }

    override fun onDataLoaded(data: List<OverflowItem>) {
        view?.showList(data.transformToUIItems())
    }

    override fun onError(message: String) {
        view?.showError(message)
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
    fun addViewListener( overflowViewListener : OverflowViewListener)
    fun showList(items: List<OverflowUIItem>)
    fun showError(message: String)



}

interface OverflowViewListener {
    fun itemSelected(position: Int)

}

class OverflowViewModel(val overflowController : OverflowController) : OverflowView, ViewModel() {

    val overflowViewState =  MutableLiveData<OverflowViewState>()

    override fun addViewListener(overflowViewListener: OverflowViewListener) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun showList(items: List<OverflowUIItem>) {
        overflowViewState.value = Success(items)
    }

    override fun showError(message: String) {
        overflowViewState.value = Error(message)
    }



    fun onReady() : LiveData<OverflowViewState> {
        overflowViewState.value = Loading()
        launch {
            overflowController.loadOverflowItems()
        }
        return overflowViewState

    }
}

class OverflowModel(val modelListener: OverflowModelListener) : Output<List<OverflowItem>> {
    override fun onSuccess(result: List<OverflowItem>) {

        modelListener.onDataLoaded(result)
    }

    override fun onFailure(message: String) {
        modelListener.onError(message)
    }

}

interface OverflowModelListener {
    fun onDataLoaded(data: List<OverflowItem>)
    fun onError(message: String)

}

class DataInteractor(val watchingRepository: WatchingRepository, val output : Output<List<OverflowItem>>) {


    fun fetch() {
        launch() {
            delay(10000L)
            val watchingResponse = watchingRepository.getOverflowItems()
            when(watchingResponse) {
                is WatchingSuccess -> output.onSuccess(watchingResponse.items.transformToOverflowModel())
                is WatchingError -> output.onFailure(watchingResponse.message)
            }
    }
}
}

interface Output<in T> {
    fun onSuccess(result: T)
    fun onFailure(message: String)
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
sealed class OverflowViewState
class Loading : OverflowViewState()
data class Success(val items : List<OverflowUIItem>) : OverflowViewState()

data class Error(val message : String) : OverflowViewState()
data class OverflowParameters(val id : String, val overflowType : OverflowType)

data class OverflowUIItem(val heading1 : String, val heading2 : String)
