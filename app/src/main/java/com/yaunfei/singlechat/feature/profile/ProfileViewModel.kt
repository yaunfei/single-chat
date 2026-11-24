package com.yaunfei.singlechat.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaunfei.singlechat.core.chat.ChatEngine
import com.yaunfei.singlechat.core.model.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: Contact? = null,
    val stats: ProfileStats? = null,
)

data class ProfileStats(
    val friends: Int = 0,
    val groups: Int = 0,
    val moments: Int = 0,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val engine: ChatEngine,
) : ViewModel() {

    private val _profile = MutableStateFlow<Contact?>(null)
    private val _stats = MutableStateFlow(ProfileStats())

    val uiState: StateFlow<ProfileUiState> = combine(_profile, _stats) { profile, stats ->
        ProfileUiState(profile, stats)
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(),
    )

    init {
        viewModelScope.launch {
            val contacts = engine.availableContacts()
            _profile.value = Contact(
                id = "u_me",
                name = "我",
                bio = "这个人很懒，什么都没留下",
                signature = "这个人很懒，什么都没留下",
            )
            _stats.value = ProfileStats(
                friends = contacts.size,
                groups = 2,
                moments = 0,
            )
        }
    }
}
