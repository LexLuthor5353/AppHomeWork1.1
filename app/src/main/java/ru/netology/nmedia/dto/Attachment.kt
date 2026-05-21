package ru.netology.nmedia.dto

import ru.netology.nmedia.enumeration.AttachmentType

data class Attachment(
    val url: String = "",
    val description: String? = null,
    val type: AttachmentType = AttachmentType.IMAGE,
)
