package com.example.newsapp.ui.entity

import android.os.Parcel
import android.os.Parcelable

data class Article(
    val id: String,
    val title: String,
    val source: String,
    val time: String,
    val category: String,
    val authorName: String = "Unknown Author",
    val authorRole: String = "Reporter",
    val body: String = "",
    var isSaved: Boolean = false,
    val imageUrl: String? = null,
    val url: String? = null,
    val imageResId: Int? = null
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readString().orEmpty(),
        title = parcel.readString().orEmpty(),
        source = parcel.readString().orEmpty(),
        time = parcel.readString().orEmpty(),
        category = parcel.readString().orEmpty(),
        authorName = parcel.readString() ?: "Unknown Author",
        authorRole = parcel.readString() ?: "Reporter",
        body = parcel.readString().orEmpty(),
        isSaved = parcel.readByte() != 0.toByte(),
        imageUrl = parcel.readString(),
        url = parcel.readString(),
        imageResId = parcel.readValue(Int::class.java.classLoader) as? Int
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(source)
        parcel.writeString(time)
        parcel.writeString(category)
        parcel.writeString(authorName)
        parcel.writeString(authorRole)
        parcel.writeString(body)
        parcel.writeByte(if (isSaved) 1 else 0)
        parcel.writeString(imageUrl)
        parcel.writeString(url)
        parcel.writeValue(imageResId)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Article> {
        override fun createFromParcel(parcel: Parcel): Article {
            return Article(parcel)
        }

        override fun newArray(size: Int): Array<Article?> {
            return arrayOfNulls(size)
        }
    }
}

data class Category(
    val id: String,
    val name: String,
    var isSelected: Boolean = false
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readString().orEmpty(),
        name = parcel.readString().orEmpty(),
        isSelected = parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeByte(if (isSelected) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Category> {
        override fun createFromParcel(parcel: Parcel): Category {
            return Category(parcel)
        }

        override fun newArray(size: Int): Array<Category?> {
            return arrayOfNulls(size)
        }
    }
}
