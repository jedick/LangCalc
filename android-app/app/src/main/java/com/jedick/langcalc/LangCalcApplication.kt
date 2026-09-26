package com.jedick.langcalc

import android.app.Application
import com.jedick.langcalc.data.AppSettings
import com.jedick.langcalc.data.ModelRepository
import com.jedick.langcalc.engine.LangCalcEngine

class LangCalcApplication : Application() {
  lateinit var settings: AppSettings
    private set

  lateinit var modelRepository: ModelRepository
    private set

  val engine: LangCalcEngine = LangCalcEngine()

  override fun onCreate() {
    super.onCreate()
    settings = AppSettings(this)
    modelRepository = ModelRepository(this, settings)
  }
}
