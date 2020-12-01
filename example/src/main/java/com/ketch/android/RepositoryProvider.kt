package com.ketch.android

import com.ketch.android.repository.KetchRepository

interface RepositoryProvider {

    fun setRepository(repo: KetchRepository)
    fun getRepository(): KetchRepository
}
