package ru.kode.android.build.publish.plugin.task.play.track

import ru.kode.android.build.publish.plugin.task.play.PlayPublisher
import ru.kode.android.build.publish.plugin.task.play.publisher.GppAppDetails
import ru.kode.android.build.publish.plugin.task.play.publisher.GppImage
import ru.kode.android.build.publish.plugin.task.play.publisher.GppListing
import ru.kode.android.build.publish.plugin.task.play.publisher.ReleaseNote
import ru.kode.android.build.publish.plugin.task.play.publisher.ReleaseStatus
import ru.kode.android.build.publish.plugin.task.play.publisher.ResolutionStrategy
import java.io.File
import java.util.ServiceLoader

interface EditManager {
    fun getAppDetails(): GppAppDetails
    fun getListings(): List<GppListing>
    fun getImages(locale: String, type: String): List<GppImage>
    fun findMaxAppVersionCode(): Long
    fun findLeastStableTrackName(): String?
    fun getReleaseNotes(): List<ReleaseNote>
    fun publishAppDetails(
        defaultLocale: String?,
        contactEmail: String?,
        contactPhone: String?,
        contactWebsite: String?,
    )

    fun publishListing(
        locale: String,
        title: String?,
        shortDescription: String?,
        fullDescription: String?,
        video: String?,
    )

    fun publishImages(locale: String, type: String, images: List<File>)
    fun promoteRelease(
        promoteTrackName: String,
        fromTrackName: String,
        releaseStatus: ReleaseStatus?,
        releaseName: String?,
        releaseNotes: Map</* locale= */String, /* text= */String?>?,
        userFraction: Double?,
        updatePriority: Int?,
        retainableArtifacts: List<Long>?,
        versionCode: Long?
    )

    fun uploadBundle(
        bundleFile: File,
        strategy: ResolutionStrategy,
    ): Long?

    fun publishArtifacts(
        versionCodes: List<Long>,
        didPreviousBuildSkipCommit: Boolean,
        trackName: String,
        releaseStatus: ReleaseStatus?,
        releaseName: String?,
        releaseNotes: Map</* locale= */String, /* text= */String?>?,
        userFraction: Double?,
        updatePriority: Int?,
        retainableArtifacts: List<Long>?,
    )

    interface Factory {
        fun create(publisher: PlayPublisher, editId: String): EditManager
    }

    companion object {
        operator fun invoke(
            publisher: PlayPublisher,
            editId: String,
        ): EditManager = ServiceLoader.load(Factory::class.java).last()
            .create(publisher, editId)
    }
}
