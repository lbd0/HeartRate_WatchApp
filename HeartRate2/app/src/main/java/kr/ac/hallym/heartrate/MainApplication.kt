package kr.ac.hallym.heartrate

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.checkerframework.checker.units.qual.A

@HiltAndroidApp
class MainApplication : Application()

const val TAG = "Measuring Data"