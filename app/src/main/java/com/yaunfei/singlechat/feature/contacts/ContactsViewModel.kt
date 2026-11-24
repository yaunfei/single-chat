package com.yaunfei.singlechat.feature.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaunfei.singlechat.core.chat.ChatEngine
import com.yaunfei.singlechat.core.model.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val query: String = "",
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val engine: ChatEngine,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val contacts = MutableStateFlow<List<Contact>>(emptyList())

    val uiState: StateFlow<ContactsUiState> = combine(contacts, query) { list, keyword ->
        if (keyword.isBlank()) {
            ContactsUiState(list, keyword)
        } else {
            ContactsUiState(
                contacts = list.filter { contact ->
                    contact.name.contains(keyword, ignoreCase = true) ||
                        contact.id.contains(keyword, ignoreCase = true)
                },
                query = keyword,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ContactsUiState(),
    )

    init {
        viewModelScope.launch { contacts.value = engine.availableContacts() }
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun createConversation(contact: Contact, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            onCreated(engine.createConversation(contact))
        }
    }

    fun createConversationById(rawPeerId: String, onCreated: (String) -> Unit) {
        val peerId = rawPeerId.trim()
        if (peerId.isEmpty()) return
        viewModelScope.launch {
            onCreated(
                engine.createConversation(
                    Contact(id = peerId, name = peerId, bio = "通过用户 ID 发起"),
                ),
            )
        }
    }
}
