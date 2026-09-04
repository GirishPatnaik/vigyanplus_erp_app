package com.vigyan.juniorcollege.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.viewModelFactory

inline fun <reified VM : ViewModel> simpleFactory(crossinline create: () -> VM) =
    viewModelFactory {
        initializer { create() }
    }
