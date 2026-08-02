package com.mobile.data

import android.content.Context
import com.mobile.data.db.AppDatabase
import com.mobile.data.db.toDomain
import com.mobile.data.db.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Persists generated Achievement Certificates (Room-backed) so they can be re-downloaded or re-shared later without regenerating them. */
object CertificateRepository {
    private lateinit var db: AppDatabase
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var initialized = false

    private val _certificates = MutableStateFlow<List<Certificate>>(emptyList())
    val certificates: StateFlow<List<Certificate>> = _certificates.asStateFlow()

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        db = AppDatabase.getInstance(context)
        scope.launch {
            db.certificateDao().observeAll().collect { entities ->
                _certificates.value = entities.map { it.toDomain() }
            }
        }
    }

    fun save(certificate: Certificate) {
        scope.launch { db.certificateDao().insert(certificate.toEntity()) }
    }

    fun delete(certificate: Certificate) {
        scope.launch { db.certificateDao().delete(certificate.id) }
    }
}
