package com.yaunfei.singlechat.feature.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaunfei.singlechat.core.chat.ChatEngine
import com.yaunfei.singlechat.core.model.Contact
import com.yaunfei.singlechat.core.model.GroupInfo
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    val filteredContacts: StateFlow<List<Contact>> = combine(_contacts, _searchQuery) { list, keyword ->
        if (keyword.isBlank()) {
            list
        } else {
            list.filter { contact ->
                contact.name.contains(keyword, ignoreCase = true) ||
                    contact.id.contains(keyword, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    // 常用联系人（取前 4 个）
    val frequentlyUsed: StateFlow<List<Contact>> = _contacts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    // 群聊（演示数据）
    private val _groups = MutableStateFlow<List<GroupInfo>>(emptyList())
    val groups: StateFlow<List<GroupInfo>> = _groups.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = engine.availableContacts()
            _contacts.value = loaded
            // 模拟群聊数据
            _groups.value = listOf(
                GroupInfo("g1", "产品设计组", 8),
                GroupInfo("g2", "设计灵感", 12),
            )
        }
    }

    fun setSearchQuery(value: String) {
        _searchQuery.value = value
    }

    fun setQuery(value: String) {
        _searchQuery.value = value
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

    fun addContactById(rawPeerId: String) {
        val peerId = rawPeerId.trim()
        if (peerId.isEmpty()) return
        viewModelScope.launch {
            val newContact = Contact(id = peerId, name = peerId, bio = "新的联系人")
            if (_contacts.value.none { it.id == peerId }) {
                _contacts.update { it + newContact }
            }
        }
    }
}
