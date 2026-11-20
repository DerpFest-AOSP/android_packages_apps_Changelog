/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.models

import java.time.LocalDate

/**
 * An entry of the changelog list.
 */
sealed interface ChangelogEntry {
    /**
     * The key identifying the entry in the list.
     */
    val key: String

    /**
     * The header of the changes of a day.
     *
     * @param date The day
     */
    data class Day(val date: LocalDate) : ChangelogEntry {
        override val key get() = "day_$date"
    }

    /**
     * A change, part of the group of changes of a day.
     *
     * @param change The change
     * @param index The position of the change in its group
     * @param count The number of changes in its group
     */
    data class Item(
        val change: Change,
        val index: Int,
        val count: Int,
    ) : ChangelogEntry {
        override val key get() = change.id
    }
}
