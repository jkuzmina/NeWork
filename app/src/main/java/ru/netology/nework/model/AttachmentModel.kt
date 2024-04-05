package ru.netology.nework.model

import android.net.Uri
import ru.netology.nework.enumeration.AttachmentType
import java.io.File

data class AttachmentModel(val url: String? = null, val uri: Uri? = null, val file: File? = null, val attachmentType: AttachmentType? = null)
