package ru.netology.nework.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.db.AppDb
import ru.netology.nework.dto.Coords
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.dto.User
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.enumeration.EventType
import ru.netology.nework.error.AppError
import ru.netology.nework.model.AttachmentModel
import ru.netology.nework.model.FeedModel
import ru.netology.nework.model.FeedModelState
import ru.netology.nework.repository.EventRepository
import ru.netology.nework.repository.EventRepositoryImpl
import ru.netology.nework.util.AndroidUtils
import ru.netology.nework.util.SingleLiveEvent
import java.io.File
import java.util.Calendar

private val empty = Event(
    id= 0,
    authorId= 0,
    author= "",
    authorJob="",
    authorAvatar= "",
    datetime = AndroidUtils.calendarToUTCDate(Calendar.getInstance()),
    published = "",
    content="",
    likeOwnerIds = emptyList(),
    likedByMe = false,
    participantsIds = emptyList(),
    participatedByMe = false,
    speakerIds = emptyList(),
    type = EventType.ONLINE,
    users = emptyMap()
)
private val noAttachment: AttachmentModel? = null
private const val emptyDateTime = ""
private val defaultType = EventType.ONLINE

class EventViewModel(application: Application) : AndroidViewModel(application){
    private val repository: EventRepository =
        EventRepositoryImpl(AppDb.getInstance(context = application).eventDao())

    val data: LiveData<FeedModel<Event>> = AppAuth.getInstance()
        .authStateFlow
        .flatMapLatest {auth ->
            repository.data.map{events ->
                FeedModel(
                    events.map { it.copy(ownedByMe = auth.id == it.authorId) },
                    events.isEmpty()
                )
            }
        }.asLiveData(Dispatchers.Default)

    val newerCount: LiveData<Int> = data.switchMap {
        repository.getNewerCount(it.data.firstOrNull()?.id ?: 0L)
            .catch { e -> throw AppError.from(e) }
            .asLiveData(Dispatchers.Default)
    }

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val _likers = MutableLiveData<List<User>>(emptyList())
    val likers: LiveData<List<User>>
        get() = _likers

    private val _likersLoaded = SingleLiveEvent<Unit>()
    val likersLoaded: LiveData<Unit>
        get() = _likersLoaded

    private val _speakers = MutableLiveData<List<User>>(emptyList())
    val speakers: LiveData<List<User>>
        get() = _speakers

    private val _speakersLoaded = SingleLiveEvent<Unit>()
    val speakersLoaded: LiveData<Unit>
        get() = _speakersLoaded

    private val _participants = MutableLiveData<List<User>>(emptyList())
    val participants: LiveData<List<User>>
        get() = _participants

    private val _participantsLoaded = SingleLiveEvent<Unit>()
    val participantsLoaded: LiveData<Unit>
        get() = _participantsLoaded

    val edited = MutableLiveData(empty)

    private val _eventCreated = SingleLiveEvent<Unit>()
    val eventCreated: LiveData<Unit>
        get() = _eventCreated

    private val _attachment = MutableLiveData(noAttachment)
    val attachment: LiveData<AttachmentModel?>
        get() = _attachment

    private val _coords = MutableLiveData<Coords?>()
    val coords: LiveData<Coords?>
        get() = _coords

    private val _speakersNewEvent = MutableLiveData<List<Long>>(emptyList())
    val speakersNewEvent: LiveData<List<Long>>
        get() = _speakersNewEvent

    private val _changed = MutableLiveData<Boolean>()
    val changed: LiveData<Boolean>
        get() = _changed

    private val _datetime = MutableLiveData(emptyDateTime)
    val datetime: LiveData<String>
        get() = _datetime

    private val _eventType = MutableLiveData(defaultType)
    val eventType: LiveData<EventType>
        get() = _eventType

    init {
        loadEvents()
    }

    fun loadEvents()  = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.getAll()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }
    private fun validate(): Boolean{
        if(_datetime.value.equals("")) {
            _dataState.value = FeedModelState(error = true, errorText = "It's necessary to specify the date")
            return false
        }
        if(speakers.value == null) {
            _dataState.value = FeedModelState(error = true, errorText = "It's necessary to specify speakers")
            return false
        }
        return true
    }

    fun save() {
        if(validate()) {
            edited.value?.let {
                val newEvent = it.copy(
                    datetime = _datetime.value!!,
                    published = AndroidUtils.calendarToUTCDate(Calendar.getInstance()),
                    coords = _coords.value,
                    speakerIds = _speakersNewEvent.value!!,
                    type = _eventType.value!!
                )
                _eventCreated.value = Unit
                viewModelScope.launch {
                    try {
                        when (_attachment.value) {
                            null -> {
                                repository.save(newEvent.copy(attachment = null))
                            } //событие без вложения
                            else -> {
                                //редактируется событие с уже загруженным вложением
                                if (_attachment.value?.url != null) {
                                    repository.save(newEvent)
                                } else {//новое событие или поменяли вложение
                                    _attachment.value!!.file?.let {
                                        repository.saveWithAttachment(
                                            newEvent,
                                            MediaUpload(_attachment.value!!.file!!),
                                            _attachment.value!!.attachmentType!!
                                        )
                                    }
                                }
                            }
                        }
                        _dataState.value = FeedModelState()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _dataState.value = FeedModelState(error = true)
                    }
                }
            }
            clearEdit()
        }
    }
    fun edit(event: Event?) {
        if(event != null){
            edited.value = event
        } else{
            clearEdit()
        }
    }

    private fun clearEdit(){
        edited.value = empty
        _attachment.value = null
        _coords.value = null
        _changed.value = false
        _datetime.value = emptyDateTime
        _eventType.value = defaultType
    }

    fun reset(){
        _changed.value = false
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value = edited.value?.copy(content = text)
        _changed.value = true

    }

    fun changeLink(link: String) {
        val text = link.trim()
        if (edited.value?.link == text) {
            return
        }
        if(text == "") {
            edited.value = edited.value?.copy(link = null)
        } else {
            edited.value = edited.value?.copy(link = text)
        }
        _changed.value = true
    }

    fun changeAttachment(url:String?, uri: Uri?, file: File?, attachmentType: AttachmentType?){
        if(uri == null){
            if(url != null){ //редактирование события с вложением
                _attachment.value = AttachmentModel(url, null, null, attachmentType)
            }else{
                _attachment.value = null //удалили вложение
            }
        } else{
            _attachment.value = AttachmentModel(null, uri, file, attachmentType)
        }
        _changed.value = true
    }

    fun changeCoords(coords: Coords?){
        _coords.value = coords
        _changed.value = true
    }

    fun changeDateTime(dateTime: String){
        _datetime.value = dateTime
        _changed.value = true
    }

    fun changeType(eventType: EventType){
        _eventType.value = eventType
        _changed.value = true
    }
    fun changeSpeakersNewEvent(list: List<Long>){
        _speakersNewEvent.value = list
    }

    fun chooseUser(user: User){
        _speakersNewEvent.value = speakersNewEvent.value?.plus(user.id)
        _changed.value = true
    }

    fun removeUser(user: User){
        _speakersNewEvent.value = speakersNewEvent.value?.filter { it != user.id }
        _changed.value = true
    }

    fun likeById(event: Event)  = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.likeById(event)
            _dataState.value = FeedModelState()
        } catch (e: Exception) {

            _dataState.value = FeedModelState(error = true)
        }
    }

    fun participateById(event: Event)  = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.participateById(event)
            _dataState.value = FeedModelState()
        } catch (e: Exception) {

            _dataState.value = FeedModelState(error = true)
        }
    }

    fun removeById(event: Event)  = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.removeById(event)
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun getLikers (event: Event) = viewModelScope.launch {
        try {
            _likers.value = repository.getLikers(event)
            _likersLoaded.value = Unit

        }catch (e: Exception) {
            println(e.stackTrace)
        }
    }

    fun getSpeakers (event: Event) = viewModelScope.launch {
        try {
            _speakers.value = repository.getSpeakers(event)
            _speakersLoaded.value = Unit

        }catch (e: Exception) {
            println(e.stackTrace)
        }
    }

    fun getParticipants (event: Event) = viewModelScope.launch {
        try {
            _participants.value = repository.getParticipants(event)
            _participantsLoaded.value = Unit

        }catch (e: Exception) {
            println(e.stackTrace)
        }
    }

    fun readNewEvents()  = viewModelScope.launch {
        repository.readNewEvents()
    }

}