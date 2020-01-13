package com.somfy.homeapp.ui

import android.content.Intent
import android.os.Parcelable
import android.view.View
import androidx.annotation.StringRes
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.modulotech.epos.models.Action
import com.somfy.homeapp.managers.ConnectManager
import com.somfy.homeapp.models.DeviceWrapper
import com.somfy.homeapp.models.GroupFamily
import com.somfy.homeapp.views.dialog.bottom.BottomDialog
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import kotlin.reflect.KClass

abstract class IViewModel<T> : ViewModel(),
    ConnectManager.Listener,
    IActivity.OnItemMenuSelected {
    val isLoading: ObservableBoolean by lazy { ObservableBoolean(false) }
    val disposable: CompositeDisposable by lazy { CompositeDisposable() }
    val actions: MutableLiveData<IAction> by lazy { MutableLiveData<IAction>() }

    abstract fun init(intent: T?)

    open fun onViewDisplayed() = Unit
    open fun registerListener() = Unit
    open fun unregisterListener() = Unit

    override fun onItemMenuSelected(id: Int): Boolean = true

    override fun onConnected() = Unit
    override fun onDisconnected() = Unit

    abstract class IAction

    class Actions private constructor() {
        data class Activity(
            val clazz: KClass<out android.app.Activity>,
            val intent: Intent.() -> Unit
        ) : IAction()

        data class ActivityForResult(
            val clazz: KClass<out android.app.Activity>,
            val requestCode: Int,
            val intent: Intent.() -> Unit
        ) : IAction()

        data class Dialog(
            @get:StringRes val titleRes: Int,
            @get:StringRes val descriptionRes: Int,
            @get:StringRes val positiveRes: Int,
            @get:StringRes val negativeRes: Int,
            val callback: () -> Unit
        ) : IAction()

        data class BottomDialogAction(
            val builder: BottomDialog.Builder
        ) : IAction()

        data class ActionDevice(
            val view: View,
            val wrapper: DeviceWrapper,
            val requestCode: Int,
            val action: Action? = null
        ) : IAction()

        data class GroupAction(
            val family: GroupFamily,
            val actions: List<Action>,
            val requestCode: Int
        ) : IAction()

        data class Close(
            val code: Int = android.app.Activity.RESULT_OK,
            val intent: Intent.() -> Unit = {}
        ) : IAction()
    }
}