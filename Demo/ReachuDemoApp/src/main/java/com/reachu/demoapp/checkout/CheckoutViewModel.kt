package com.reachu.demoapp.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.reachu.VioUI.Components.VioCheckoutOverlayController
import io.reachu.VioUI.Managers.CartManager
import io.reachu.VioUI.Managers.resetCartAndCreateNew
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val step: VioCheckoutOverlayController.CheckoutStep = VioCheckoutOverlayController.CheckoutStep.OrderSummary,
    val selectedMethod: VioCheckoutOverlayController.PaymentMethod = VioCheckoutOverlayController.PaymentMethod.Stripe,
    val allowedMethods: List<VioCheckoutOverlayController.PaymentMethod> = emptyList(),
    val isProcessing: Boolean = false,
    val error: String? = null,
)

class CheckoutViewModel(
    private val cartManager: CartManager,
    private val controller: VioCheckoutOverlayController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                allowedMethods = controller.allowedPaymentMethods,
                selectedMethod = controller.selectedPaymentMethod,
            )
        }
    }

    fun selectMethod(method: VioCheckoutOverlayController.PaymentMethod) {
        controller.selectPaymentMethod(method)
        _uiState.update { it.copy(selectedMethod = method) }
    }

    fun goToStep(step: VioCheckoutOverlayController.CheckoutStep) {
        controller.goToStep(step)
        _uiState.update { it.copy(step = step) }
    }

    fun proceedToPayment() {
        if (_uiState.value.isProcessing) return
        _uiState.update { it.copy(isProcessing = true, error = null) }
        controller.proceedToPayment(advanceToReview = false) { result ->
            _uiState.update { it.copy(isProcessing = false) }
            result.onSuccess {
                _uiState.update { state -> state.copy(step = VioCheckoutOverlayController.CheckoutStep.Review) }
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(step = VioCheckoutOverlayController.CheckoutStep.Error, error = throwable.message)
                }
            }
        }
    }

    fun completeCheckout(status: String? = null) {
        viewModelScope.launch {
            controller.updateCheckout(
                paymentMethod = _uiState.value.selectedMethod,
                advanceToSuccess = true,
                status = status,
            ) { result ->
                result.onSuccess {
                    _uiState.update { it.copy(step = VioCheckoutOverlayController.CheckoutStep.Success) }
                    cartManager.resetCartAndCreateNew()
                }.onFailure { throwable ->
                    _uiState.update { it.copy(step = VioCheckoutOverlayController.CheckoutStep.Error, error = throwable.message) }
                }
            }
        }
    }
}

class CheckoutViewModelFactory(
    private val cartManager: CartManager,
    private val controller: VioCheckoutOverlayController,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CheckoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CheckoutViewModel(cartManager, controller) as T
        }
        throw IllegalArgumentException("Unknown CheckoutViewModel class")
    }
}
