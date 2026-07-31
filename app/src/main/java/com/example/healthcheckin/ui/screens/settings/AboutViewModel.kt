package com.example.healthcheckin.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.example.healthcheckin.domain.analytics.AnalyticsEvents
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    init {
        analyticsTracker.track(AnalyticsEvents.ABOUT_OPENED)
    }

    fun trackLink(linkId: String) {
        analyticsTracker.track(
            AnalyticsEvents.EXTERNAL_LINK_CLICKED,
            mapOf("link_id" to linkId),
        )
    }
}
