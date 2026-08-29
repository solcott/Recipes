// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.scottolcott.recipe.domain.circuit

import co.touchlab.kermit.Logger
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.animation.AnimatedScreenTransform
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.serialization.CircuitSerializerRegistration
import com.slack.circuit.serialization.SerializableCircuitSaver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KClass

@OptIn(
  ExperimentalCircuitApi::class // For AnimatedScreenTransform
)
@ContributesTo(AppScope::class)
interface CircuitProviders {

  /**
   * Screens are persisted with kotlinx-serialization; [registrations] is the multibound set Circuit
   * codegen emits for every `@CircuitSerializable` screen.
   *
   * Saving an unregistered screen throws, but restoring one only returns null and the nav stack
   * silently drops that record, so the restore-error callback is wired to the logger to make it
   * visible.
   */
  @Provides
  @SingleIn(AppScope::class)
  fun provideCircuitSaver(
    registrations: Set<CircuitSerializerRegistration>,
    logger: Logger,
  ): CircuitSaver =
    SerializableCircuitSaver(
      registrations,
      onRestoreError = {
        logger.e(it) { "Failed to restore a Circuit screen; dropping the record" }
      },
    )

  @Multibinds(allowEmpty = true)
  fun animatedScreenTransforms(): Map<KClass<out Screen>, AnimatedScreenTransform>

  @SingleIn(AppScope::class)
  @Provides
  fun provideCircuit(
    presenterFactories: Set<Presenter.Factory>,
    uiFactories: Set<Ui.Factory>,
    animatedScreenTransforms:
      @JvmSuppressWildcards
      Map<KClass<out Screen>, AnimatedScreenTransform>,
    circuitSaver: CircuitSaver,
  ): Circuit {
    return Circuit.Builder()
      .addPresenterFactories(presenterFactories)
      .addUiFactories(uiFactories)
      .addAnimatedScreenTransforms(animatedScreenTransforms)
      .setCircuitSaver(circuitSaver)
      .build()
  }
}
