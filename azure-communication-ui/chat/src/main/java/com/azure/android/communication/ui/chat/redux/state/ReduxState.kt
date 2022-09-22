// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.android.communication.ui.chat.redux.state

internal interface ReduxState {
    var chatState: ChatState
    var participantState: ParticipantsState
    var lifecycleState: LifecycleState
    var errorState: ErrorState
    var navigationState: NavigationState
}
