import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.doOnLayout
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import com.modulotech.epos.agent.EPOSAgent
import com.modulotech.epos.manager.PollManager
import com.somfy.homeapp.BR
import com.somfy.homeapp.HomeApp
import com.somfy.homeapp.R
import com.somfy.homeapp.extensions.dialog
import com.somfy.homeapp.extensions.popLastFragment
import com.somfy.homeapp.extensions.replaceFragment
import com.somfy.homeapp.extensions.start
import com.somfy.homeapp.managers.ConnectManager
import com.somfy.homeapp.views.toast.ToastService
import kotlin.reflect.KClass

abstract class IActivity<B : ViewDataBinding, VM : IActivityViewModel>(
    @LayoutRes private val resId: Int,
    @MenuRes private val menuResId: Int = 0,
    private val viewModelClazz: KClass<VM>,
    private val enablePolling: Boolean = true
) : AppCompatActivity(), Observer<IViewModel.IAction> {
    open lateinit var binding: B
    protected lateinit var viewModel: VM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, resId)

        viewModel = ViewModelProviders.of(this)[viewModelClazz.java].also {
            it.init(intent)
            it.actions.observeForever(this@IActivity)
        }

        binding.lifecycleOwner = this
        binding.setVariable(BR.viewModel, viewModel)

        findViewById<Toolbar>(R.id.toolbar)?.let {
            setSupportActionBar(it)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        if (::viewModel.isInitialized)
            viewModel.registerListener()

        binding.root.doOnLayout {
            viewModel.onViewDisplayed()
        }

        initView()
    }

    open fun initView() = Unit
    open fun handleAction(action: IViewModel.IAction) = Unit

    override fun onChanged(action: IViewModel.IAction?) {
        action ?: return

        when (action) {
            is IViewModel.Actions.Activity -> start(action.clazz, action.intent)
            is IViewModel.Actions.ActivityForResult -> startActivityForResult(
                Intent(this, action.clazz.java).apply(action.intent),
                action.requestCode
            )
            is IViewModel.Actions.Dialog -> dialog(action)
            is IViewModel.Actions.Close -> {
                setResult(action.code, Intent().apply(action.intent))
                finish()
            }
            is IActivityViewModel.Actions.PushFragment -> replaceFragment(action)
            is IActivityViewModel.Actions.PopFragment -> popLastFragment()
            else -> handleAction(action)
        }
    }

    override fun onDestroy() {
        if (this::viewModel.isInitialized) {
            viewModel.disposable.dispose()
            viewModel.unregisterListener()
        }

        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (menuResId != 0)
            menuInflater.inflate(menuResId, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            if (viewModel.onItemMenuSelected(it.itemId)) {
                when (it.itemId) {
                    android.R.id.home -> supportFinishAfterTransition()
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (!::viewModel.isInitialized || !viewModel.isLoading.get())
            supportFinishAfterTransition()
    }

    interface OnItemMenuSelected {
        fun onItemMenuSelected(@IdRes id: Int): Boolean
    }
}
