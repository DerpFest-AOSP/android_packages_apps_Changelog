/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.models

/**
 * Which changes the changelog shows.
 *
 * @param period The time range of the changes
 * @param sources Where the changes come from
 * @param showTranslations Whether to show changes that only update translations
 * @param showMerges Whether to show merge commits
 */
data class ChangesFilter(
    val period: Period,
    val sources: Set<ChangeSource>,
    val showTranslations: Boolean,
    val showMerges: Boolean,
) {
    /**
     * Whether some kinds of changes are hidden.
     */
    val hidesChanges: Boolean
        get() = sources != ChangeSource.entries.toSet() || !showTranslations || !showMerges

    /**
     * Whether the change is shown, regardless of the period.
     *
     * @param change The change
     */
    fun matches(change: Change) = change.source in sources &&
            (showTranslations || !change.isTranslation) &&
            (showMerges || !change.isMerge)
}
