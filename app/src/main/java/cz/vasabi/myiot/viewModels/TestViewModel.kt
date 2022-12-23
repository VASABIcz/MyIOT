package cz.vasabi.myiot.viewModels

import androidx.lifecycle.ViewModel
import cz.vasabi.myiot.SingleState

class TestViewModel: ViewModel() {
    companion object {
        var counter: Int = 0
    }
    override fun onCleared() {
        SingleState.events.add("ViewModel $counter")
        counter += 1
    }
}