package com.google.ai.edge.gallery.customtasks.langcalc

import android.content.Context
import com.google.ai.edge.gallery.customtasks.common.CustomTask
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Binds [LangCalcTask] into the app's `Set<CustomTask>` so it's automatically discoverable on the
 * home screen, under the LLM category, as "Voice Calculator". 
 */
@Module
@InstallIn(SingletonComponent::class)
internal object LangCalcModule {
  @Provides
  @IntoSet
  fun provideLangCalcTask(@ApplicationContext context: Context): CustomTask {
    return LangCalcTask(context)
  }
}
