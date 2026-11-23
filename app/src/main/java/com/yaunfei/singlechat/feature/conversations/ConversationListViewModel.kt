package com.yaunfei.singlechat.feature.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaunfei.singlechat.core.chat.ChatConnectionState
import com.yaunfei.singlechat.core.chat.ChatEngine
import com.yaunfei.singlechat.core.model.Contact
import com.yaunfei.singlechat.core.model.Conversation
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

data class ConversationListUiState(
    val isLoading: Boolean = true,
    val conversations: List<Conversation> = emptyList(),
    val query: String = "",
)

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val engine: ChatEngine,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val _uiState = MutableStateFlow(ConversationListUiState())
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    val connectionState: StateFlow<ChatConnectionState> = engine.observeConnectionState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ChatConnectionState.CONNECTING,
        )

    init {
        viewModelScope.launch {
            engine.ensureSeeded()
            _contacts.value = engine.availableContacts()
            _uiState.update { it.copy(isLoading = false) }
        }
        viewModelScope.launch {
            combine(engine.observeConversations(), query) { conversations, keyword ->
                conversations to keyword
            }.collect { (conversations, keyword) ->
                val filtered = if (keyword.isBlank()) {
                    conversations
                } else {
                    conversations.filter { conversation ->
                        conversation.title.contains(keyword, ignoreCase = true) ||
                            conversation.lastMessage.contains(keyword, ignoreCase = true)
                    }
                }
                _uiState.update { it.copy(conversations = filtered) }
            }
        }
    }

    fun setQuery(value: String) {
        query.value = value
        _uiState.update { it.copy(query = value) }
    }

    fun markRead(conversationId: String) {
        viewModelScope.launch { engine.markRead(conversationId) }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch { engine.deleteConversation(conversationId) }
    }

    fun createConversation(contact: Contact, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val conversationId = engine.createConversation(contact)
            _contacts.value = engine.availableContacts()
            onCreated(conversationId)
        }
    }

    fun createConversationById(rawPeerId: String, onCreated: (String) -> Unit) {
        val peerId = rawPeerId.trim()
        if (peerId.isEmpty()) return
        viewModelScope.launch {
            val conversationId = engine.createConversation(
                Contact(id = peerId, name = peerId, bio = "通过用户 ID 发起"),
            )
            _contacts.value = engine.availableContacts()
            onCreated(conversationId)
        }
    }
}
