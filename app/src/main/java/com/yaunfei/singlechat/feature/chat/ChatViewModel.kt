package com.yaunfei.singlechat.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaunfei.singlechat.core.chat.ChatEngine
import com.yaunfei.singlechat.core.model.Conversation
import com.yaunfei.singlechat.core.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val engine: ChatEngine,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val conversationId: String = checkNotNull(savedStateHandle["conversationId"])

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    val conversation: StateFlow<Conversation?> = engine.observeConversation(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val messages: StateFlow<List<Message>> = engine.observeMessages(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isTyping: StateFlow<Boolean> = engine.observeTyping(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        engine.setActiveConversation(conversationId)
        viewModelScope.launch { engine.markRead(conversationId) }
    }

    fun onDraftChange(value: String) {
        _draft.value = value.take(MaxMessageLength)
    }

    fun send() {
        val text = _draft.value.trim()
        if (text.isEmpty()) return
        _draft.value = ""
        viewModelScope.launch { engine.sendText(conversationId, text) }
    }

    fun retry(messageId: String) {
        viewModelScope.launch { engine.retry(messageId) }
    }

    override fun onCleared() {
        engine.setActiveConversation(null)
        super.onCleared()
    }

    private companion object {
        const val MaxMessageLength = 2_000
    }
}
