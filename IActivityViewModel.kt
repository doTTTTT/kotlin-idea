package com.somfy.homeapp.ui

import android.content.Intent
import android.os.Parcelable
import androidx.annotation.AnimRes
import androidx.fragment.app.Fragment
import com.somfy.homeapp.R
import kotlinx.android.parcel.Parcelize

abstract class IActivityViewModel : IViewModel<Intent>() {
    override fun init(intent: Intent?) = Unit

    companion object Actions {
        class PushFragment(
            val fragment: Fragment,
            @AnimRes val inOne: Int = R.anim.fade_in,
            @AnimRes val outOne: Int = R.anim.fade_out,
            @AnimRes val inTwo: Int = R.anim.fade_in,
            @AnimRes val outTwo: Int = R.anim.fade_out,
            val withAnim: Boolean = true
        ) : IAction()

        @Parcelize
        class PopFragment : IAction(), Parcelable
    }
}