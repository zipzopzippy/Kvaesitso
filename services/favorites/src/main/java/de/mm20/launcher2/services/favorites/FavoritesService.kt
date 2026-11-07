package de.mm20.launcher2.services.favorites

import de.mm20.launcher2.preferences.search.FavoritesSettings
import de.mm20.launcher2.profiles.Profile
import de.mm20.launcher2.profiles.ProfileManager
import de.mm20.launcher2.search.SavableSearchable
import de.mm20.launcher2.searchable.PinnedLevel
import de.mm20.launcher2.searchable.SavableSearchableRepository
import de.mm20.launcher2.searchable.VisibilityLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class FavoritesService(
    private val searchableRepository: SavableSearchableRepository,
    private val favoritesSettings: FavoritesSettings,
    private val profileManager: ProfileManager,
) {
    fun getFavorites(
        includeTypes: List<String>? = null,
        excludeTypes: List<String>? = null,
        minPinnedLevel: PinnedLevel = PinnedLevel.FrequentlyUsed,
        maxPinnedLevel: PinnedLevel = PinnedLevel.ManuallySorted,
        limit: Int = 100,
    ): Flow<List<SavableSearchable>> {
        fun query(min: PinnedLevel, max: PinnedLevel) = searchableRepository.get(
            includeTypes = includeTypes,
            excludeTypes = excludeTypes,
            minPinnedLevel = min,
            maxPinnedLevel = max,
            limit = limit,
            minVisibility = VisibilityLevel.SearchOnly,
        )

        // Personal build: frequently used items are filtered, pinned ones never are (see MODIFICATIONS.md).
        // No caller asks for frequently used items by included types or for unused items.
        if (minPinnedLevel != PinnedLevel.FrequentlyUsed || includeTypes != null) {
            return query(minPinnedLevel, maxPinnedLevel)
        }

        val frequentlyUsed = combine(
            favoritesSettings.frequentlyUsedExcluded,
            lockedPrivateSpaceSerial(),
            ::Pair,
        ).flatMapLatest { (excluded, lockedSerial) ->
            searchableRepository.getFrequentlyUsed(
                excludeTypes = excludeTypes,
                excludeKeys = excluded,
                excludeUserSerial = lockedSerial,
                minVisibility = VisibilityLevel.SearchOnly,
                limit = limit,
            )
        }

        if (maxPinnedLevel == PinnedLevel.FrequentlyUsed) return frequentlyUsed

        return combine(
            query(PinnedLevel.AutomaticallySorted, maxPinnedLevel),
            frequentlyUsed,
        ) { pinned, frequent -> (pinned + frequent).take(limit) }
    }

    private fun lockedPrivateSpaceSerial(): Flow<Long?> {
        return profileManager.profileStates.map {
            if (it[Profile.Type.Private]?.locked == true) {
                profileManager.getProfile(Profile.Type.Private)?.serial
            } else null
        }.distinctUntilChanged()
    }

    fun isPinned(searchable: SavableSearchable): Flow<Boolean> {
        return searchableRepository.isPinned(searchable)
    }

    fun getVisibility(searchable: SavableSearchable): Flow<VisibilityLevel> {
        return searchableRepository.getVisibility(searchable)
    }

    fun pinItem(searchable: SavableSearchable) {
        searchableRepository.upsert(
            searchable,
            pinned = true,
        )
    }

    fun reset(searchable: SavableSearchable) {
        searchableRepository.update(
            searchable,
            pinned = false,
            visibility = VisibilityLevel.Default,
            weight = 0.0,
            launchCount = 0,
        )
    }

    fun unpinItem(searchable: SavableSearchable) {
        searchableRepository.upsert(
            searchable,
            pinned = false,
        )
    }

    fun setVisibility(searchable: SavableSearchable, visibility: VisibilityLevel) {
        searchableRepository.upsert(
            searchable,
            visibility = visibility,
        )
    }

    fun reportLaunch(searchable: SavableSearchable) {
        searchableRepository.touch(searchable)
    }

    fun updateFavorites(
        manuallySorted: List<SavableSearchable>,
        automaticallySorted: List<SavableSearchable>
    ) {
        searchableRepository.updateFavorites(
            manuallySorted = manuallySorted,
            automaticallySorted = automaticallySorted
        )
    }

    fun delete(searchable: SavableSearchable) {
        searchableRepository.delete(searchable)
    }

    fun upsert(searchable: SavableSearchable) {
        searchableRepository.upsert(searchable)
    }
}