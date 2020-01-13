package com.somfy.homeapp.ui

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.*
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.core.view.doOnLayout
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.somfy.homeapp.BR
import com.somfy.homeapp.extensions.dialog
import com.somfy.homeapp.extensions.start
import com.somfy.homeapp.managers.ConnectManager
import com.somfy.homeapp.util.TransitionHelper
import com.somfy.homeapp.views.activity.groupaction.GroupActionActivity
import com.somfy.homeapp.views.activity.steering.action.ActionActivity
import kotlin.reflect.KClass

abstract class IFragment<B: ViewDataBinding, VM: IFragmentViewModel>(
    @LayoutRes private val resId: Int,
    @MenuRes private val menuId: Int = 0,
    private val clazz: KClass<VM>
) : Fragment(), Observer<IViewModel.IAction> {
    lateinit var binding: B
    protected open lateinit var viewModel: VM

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, resId, container, false)
        viewModel = ViewModelProviders.of(this)[clazz.java].also {
            it.init(arguments)
            it.actions.observeForever(this)
        }

        binding.lifecycleOwner = this
        binding.setVariable(BR.viewModel, viewModel)

        if (menuId != 0)
            setHasOptionsMenu(true)

        viewModel.registerListener()
        ConnectManager.registerListener(viewModel)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()

        binding.root.doOnLayout {
            viewModel.onViewDisplayed()
        }
    }

    override fun onDestroy() {
        if (this::viewModel.isInitialized) {
            viewModel.disposable.clear()
            ConnectManager.unregisterListener(viewModel)
            viewModel.unregisterListener()
        }

        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (menuId != 0)
            inflater.inflate(menuId, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return viewModel.onItemMenuSelected(item.itemId)
    }

    open fun initView() = Unit
    open fun handleAction(action: IViewModel.IAction) = Unit

    fun isViewModelInitialized(): Boolean = this::viewModel.isInitialized

    override fun onChanged(action: IViewModel.IAction?) {
        action ?: return

        when (action) {
            is IViewModel.Actions.Activity -> start(action.clazz, action.intent)
            is IViewModel.Actions.ActivityForResult -> startActivityForResult(
                Intent(requireContext(), action.clazz.java).apply(action.intent),
                    action.requestCode
                )
            is IViewModel.Actions.Dialog -> dialog(action)
            is IViewModel.Actions.ActionDevice -> requestAction(action)
            is IViewModel.Actions.GroupAction -> requestAction(action)
            is IViewModel.Actions.BottomDialogAction -> action.builder
                .build(requireContext())
                .show()
            is IViewModel.Actions.Close -> requireActivity().apply {
                setResult(action.code, Intent().apply(action.intent))
                finish()
            }
            else -> handleAction(action)
        }
    }

    private fun requestAction(action: IViewModel.Actions.ActionDevice) {
        val intent: Intent = Intent(requireContext(), ActionActivity::class.java).apply {
            putStringArrayListExtra(ActionActivity.EXTRA_URLS, arrayListOf(action.wrapper.deviceURL))
            putExtra(ActionActivity.EXTRA_ACTION, action.action as? Parcelable)
        }
        val pairs: Array<Pair<View, String>> = TransitionHelper.createSafeTransitionParticipants(
            requireActivity(),
            false,
            Pair(action.view, "card")
        )
        val options: ActivityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
            requireActivity(),
            *pairs
        )

        startActivityForResult(intent, action.requestCode, options.toBundle())
    }

    private fun requestAction(action: IViewModel.Actions.GroupAction) {
        val intent: Intent = Intent(requireContext(), GroupActionActivity::class.java).apply {
            putExtra(GroupActionActivity.EXTRA_FAMILY, action.family)
            putParcelableArrayListExtra(
                GroupActionActivity.EXTRA_ACTIONS,
                action.actions.toCollection(ArrayList())
            )
        }

        startActivityForResult(
            intent,
            action.requestCode
        )
    }
}