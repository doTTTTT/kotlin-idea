import android.os.Bundle

abstract class IFragmentViewModel : IViewModel<Bundle>() {
    override fun init(intent: Bundle?) = Unit
}
