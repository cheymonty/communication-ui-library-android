// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.android.communication.ui.presentation.fragment.calling.controlbar

import com.azure.android.communication.ui.helper.MainCoroutineRule
import com.azure.android.communication.ui.presentation.fragment.common.audiodevicelist.AudioDeviceListViewModel
import com.azure.android.communication.ui.redux.AppStore
import com.azure.android.communication.ui.redux.action.LocalParticipantAction
import com.azure.android.communication.ui.redux.state.AppReduxState
import com.azure.android.communication.ui.redux.state.AudioDeviceSelectionStatus
import com.azure.android.communication.ui.redux.state.AudioOperationalStatus
import com.azure.android.communication.ui.redux.state.AudioState
import com.azure.android.communication.ui.redux.state.CallingState
import com.azure.android.communication.ui.redux.state.CallingStatus
import com.azure.android.communication.ui.redux.state.CameraDeviceSelectionStatus
import com.azure.android.communication.ui.redux.state.CameraOperationalStatus
import com.azure.android.communication.ui.redux.state.CameraState
import com.azure.android.communication.ui.redux.state.CameraTransmissionStatus
import com.azure.android.communication.ui.redux.state.LocalUserState
import com.azure.android.communication.ui.redux.state.PermissionState
import com.azure.android.communication.ui.redux.state.PermissionStatus
import com.azure.android.communication.ui.redux.state.ReduxState
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@RunWith(MockitoJUnitRunner::class)
internal class ControlBarViewModelUnitTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Test
    fun controlBarViewModel_turnMicOn_then_dispatchTurnMicOn() {
        val appState = AppReduxState("")
        appState.localParticipantState = LocalUserState(
            CameraState(CameraOperationalStatus.PAUSED, CameraDeviceSelectionStatus.FRONT, CameraTransmissionStatus.LOCAL),
            AudioState(AudioOperationalStatus.PENDING, AudioDeviceSelectionStatus.SPEAKER_SELECTED),
            videoStreamID = null,
            displayName = "username"
        )
        val audioDeviceViewModel = mock<AudioDeviceListViewModel>()

        val mockAppStore = mock<AppStore<ReduxState>> {
            on { dispatch(any()) } doAnswer { }
        }

        val callingViewModel = ControlBarViewModel(mockAppStore::dispatch, audioDeviceViewModel)
        callingViewModel.turnMicOn()

        verify(mockAppStore, times(1)).dispatch(
            argThat { action ->
                action is LocalParticipantAction.MicOnTriggered
            }
        )
    }

    @Test
    fun controlBarViewModel_turnMicOn_then_dispatchTurnMicOff() {
        val appState = AppReduxState("")
        appState.localParticipantState = LocalUserState(
            CameraState(CameraOperationalStatus.PAUSED, CameraDeviceSelectionStatus.FRONT, CameraTransmissionStatus.LOCAL),
            AudioState(AudioOperationalStatus.PENDING, AudioDeviceSelectionStatus.SPEAKER_SELECTED),
            videoStreamID = null,
            displayName = "username"
        )
        val audioDeviceViewModel = mock<AudioDeviceListViewModel>()

        val mockAppStore = mock<AppStore<ReduxState>> {
            on { dispatch(any()) } doAnswer { }
        }

        val callingViewModel = ControlBarViewModel(mockAppStore::dispatch, audioDeviceViewModel)
        callingViewModel.turnMicOff()

        verify(mockAppStore, times(1)).dispatch(
            argThat { action ->
                action is LocalParticipantAction.MicOffTriggered
            }
        )
    }

    @Test
    fun controlBarViewModel_update_then_audioStateFlowReflectsUpdate() {
        mainCoroutineRule.testDispatcher.runBlockingTest {

            val permissionState = PermissionState(PermissionStatus.DENIED, PermissionStatus.DENIED)
            val cameraState = CameraState(CameraOperationalStatus.OFF, CameraDeviceSelectionStatus.FRONT, CameraTransmissionStatus.REMOTE)
            val audioDeviceState = AudioDeviceSelectionStatus.RECEIVER_SELECTED
            val callingState = CallingState(CallingStatus.CONNECTED)

            val appStore = mock<AppStore<ReduxState>> { }
            val audioDeviceListViewModel = mock<AudioDeviceListViewModel>()
            val callingViewModel = ControlBarViewModel(appStore::dispatch, audioDeviceListViewModel)
            callingViewModel.init(
                permissionState,
                cameraState,
                AudioState(AudioOperationalStatus.OFF, audioDeviceState),
                callingState,
            )

            val expectedAudioOperationalStatus1 = AudioOperationalStatus.ON
            val expectedAudioOperationalStatus2 = AudioOperationalStatus.OFF

            val audioState1 = AudioState(expectedAudioOperationalStatus1, audioDeviceState)
            val audioState2 = AudioState(expectedAudioOperationalStatus2, audioDeviceState)

            val resultListFromAudioStateFlow = mutableListOf<AudioOperationalStatus>()
            val flowJob = launch {
                callingViewModel.getAudioOperationalStatusStateFlow().toList(resultListFromAudioStateFlow)
            }

            // act
            callingViewModel.update(
                permissionState,
                cameraState,
                audioState1,
                callingState
            )
            callingViewModel.update(
                permissionState,
                cameraState,
                audioState2,
                callingState
            )

            // assert

            Assert.assertEquals(expectedAudioOperationalStatus1, resultListFromAudioStateFlow[1])
            Assert.assertEquals(expectedAudioOperationalStatus2, resultListFromAudioStateFlow[2])

            flowJob.cancel()
        }
    }

    @Test
    fun controlBarViewModel_update_then_cameraPermissionStateFlowReflectsUpdate() {
        mainCoroutineRule.testDispatcher.runBlockingTest {

            val expectedCameraPermissionState1 = PermissionStatus.DENIED
            val expectedCameraPermissionState2 = PermissionStatus.GRANTED

            val permissionState1 = PermissionState(
                PermissionStatus.DENIED,
                expectedCameraPermissionState1
            )

            val permissionState2 = PermissionState(
                PermissionStatus.DENIED,
                expectedCameraPermissionState2
            )

            val cameraState = CameraState(CameraOperationalStatus.OFF, CameraDeviceSelectionStatus.FRONT, CameraTransmissionStatus.REMOTE)
            val audioDeviceState = AudioDeviceSelectionStatus.RECEIVER_SELECTED
            val callingState = CallingState(CallingStatus.CONNECTED)

            val resultListFromCameraPermissionStateFlow =
                mutableListOf<ControlBarViewModel.CameraModel>()

            val appStore = mock<AppStore<ReduxState>>()
            val audioDeviceListViewModel = mock<AudioDeviceListViewModel>()
            val callingViewModel = ControlBarViewModel(appStore::dispatch, audioDeviceListViewModel)
            val initialPermissionState = PermissionState(
                PermissionStatus.UNKNOWN, PermissionStatus.UNKNOWN
            )

            callingViewModel.init(
                initialPermissionState,
                cameraState,
                AudioState(AudioOperationalStatus.OFF, audioDeviceState),
                callingState,
            )

            val flowJob = launch {
                callingViewModel.getCameraStateFlow()
                    .toList(resultListFromCameraPermissionStateFlow)
            }

            // act
            callingViewModel.update(
                permissionState1,
                cameraState,
                AudioState(AudioOperationalStatus.OFF, audioDeviceState),
                callingState,
            )
            callingViewModel.update(
                permissionState2,
                cameraState,
                AudioState(AudioOperationalStatus.OFF, audioDeviceState),
                callingState,
            )

            // assert
            Assert.assertEquals(
                expectedCameraPermissionState1,
                resultListFromCameraPermissionStateFlow[1].cameraPermissionState
            )

            Assert.assertEquals(
                expectedCameraPermissionState2,
                resultListFromCameraPermissionStateFlow[2].cameraPermissionState
            )

            flowJob.cancel()
        }
    }

    @Test
    fun controlBarViewModel_update_then_cameraStateFlowReflectsUpdate() {
        mainCoroutineRule.testDispatcher.runBlockingTest {
            // arrange
            val appStore = mock<AppStore<ReduxState>>()
            val audioDeviceListViewModel = mock<AudioDeviceListViewModel>()
            val callingViewModel = ControlBarViewModel(appStore::dispatch, audioDeviceListViewModel)

            val permissionState = PermissionState(
                PermissionStatus.GRANTED,
                PermissionStatus.GRANTED
            )

            val audioDeviceState = AudioDeviceSelectionStatus.RECEIVER_SELECTED
            val callingState = CallingState(CallingStatus.CONNECTED)
            val cameraDeviceSelectionStatus = CameraDeviceSelectionStatus.FRONT
            val cameraTransmissionStatus = CameraTransmissionStatus.REMOTE

            val expectedCameraState1 = CameraOperationalStatus.ON
            val expectedCameraState2 = CameraOperationalStatus.OFF

            val cameraState1 = CameraState(expectedCameraState1, cameraDeviceSelectionStatus, cameraTransmissionStatus)

            val cameraState2 = CameraState(expectedCameraState2, cameraDeviceSelectionStatus, cameraTransmissionStatus)

            val initialCameraState = CameraState(CameraOperationalStatus.OFF, cameraDeviceSelectionStatus, cameraTransmissionStatus)
            callingViewModel.init(
                permissionState,
                initialCameraState,
                AudioState(AudioOperationalStatus.OFF, audioDeviceState),
                callingState
            )

            val resultListFromCameraStateFlow = mutableListOf<ControlBarViewModel.CameraModel>()
            val flowJob = launch {
                callingViewModel.getCameraStateFlow().toList(resultListFromCameraStateFlow)
            }

            // act
            callingViewModel.update(
                permissionState,
                cameraState1,
                AudioState(AudioOperationalStatus.OFF, audioDeviceState),
                callingState,
            )
            callingViewModel.update(
                permissionState,
                cameraState2,
                AudioState(AudioOperationalStatus.OFF, audioDeviceState),
                callingState,
            )

            // assert
            Assert.assertEquals(
                expectedCameraState1,
                resultListFromCameraStateFlow[1].cameraState.operation
            )

            Assert.assertEquals(
                expectedCameraState2,
                resultListFromCameraStateFlow[2].cameraState.operation
            )

            flowJob.cancel()
        }
    }
}