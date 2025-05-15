package com.fft.kitchen.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fft.kitchen.printer.AidlPrinterHelper
import com.fft.kitchen.printer.LabelPrinterHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PrinterViewModel : ViewModel() {
    private val _printerStatus = MutableStateFlow(AidlPrinterHelper.PrinterStatus.DISCONNECTED)
    val printerStatus: StateFlow<AidlPrinterHelper.PrinterStatus> = _printerStatus.asStateFlow()

    private val _printingStatus = MutableStateFlow(false)
    val printingStatus: StateFlow<Boolean> = _printingStatus.asStateFlow()

    private val _printResult = MutableStateFlow<PrintResult?>(null)
    val printResult: StateFlow<PrintResult?> = _printResult.asStateFlow()

    private var printerHelper: LabelPrinterHelper? = null

    fun initializePrinter(context: android.content.Context) {
        printerHelper = LabelPrinterHelper(context)
        val callback = object : LabelPrinterHelper.PrinterCallback {
            override fun onConnected() {
                _printerStatus.value = AidlPrinterHelper.PrinterStatus.CONNECTED
            }

            override fun onDisconnected() {
                _printerStatus.value = AidlPrinterHelper.PrinterStatus.DISCONNECTED
            }

            override fun onError(message: String) {
                _printerStatus.value = AidlPrinterHelper.PrinterStatus.ERROR
                _printResult.value = PrintResult.Error(message)
            }

            override fun onCalibrationComplete() {
                _printResult.value = PrintResult.Success("Printer calibrated successfully")
            }
        }
        printerHelper?.setPrinterCallback(callback)
        printerHelper?.bindService(callback)
    }

    fun printLabel(text: String) {
        viewModelScope.launch {
            try {
                _printingStatus.value = true
                printerHelper?.printLabel(text)
                _printResult.value = PrintResult.Success("Label printed successfully")
            } catch (e: Exception) {
                _printResult.value = PrintResult.Error("Failed to print label: ${e.message}")
            } finally {
                _printingStatus.value = false
            }
        }
    }

    fun printMultipleLabels(texts: List<String>) {
        viewModelScope.launch {
            try {
                _printingStatus.value = true
                texts.forEach { text ->
                    printerHelper?.printLabel(text)
                }
                _printResult.value = PrintResult.Success("Labels printed successfully")
            } catch (e: Exception) {
                _printResult.value = PrintResult.Error("Failed to print labels: ${e.message}")
            } finally {
                _printingStatus.value = false
            }
        }
    }

    fun calibratePrinter() {
        viewModelScope.launch {
            try {
                _printingStatus.value = true
                printerHelper?.printerInit()
                _printResult.value = PrintResult.Success("Printer calibrated successfully")
            } catch (e: Exception) {
                _printResult.value = PrintResult.Error("Failed to calibrate printer: ${e.message}")
            } finally {
                _printingStatus.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        printerHelper?.unbindService()
    }

    sealed class PrintResult {
        data class Success(val message: String) : PrintResult()
        data class Error(val message: String) : PrintResult()
    }
} 