package com.snowify.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.snowify.app.data.model.UserProfile
import com.snowify.app.data.remote.CloudState
import com.snowify.app.data.remote.FirestoreService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestoreService: FirestoreService,
) {
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user!!
            _currentUser.value = user
            loadUserProfile(user.uid)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createAccount(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user!!
            _currentUser.value = user
            val profile = UserProfile(uid = user.uid, displayName = email.substringBefore("@"))
            firestoreService.updateUserProfile(profile)
            try { firestoreService.ensureFriendCode(user.uid) } catch (_: Exception) {}
            _userProfile.value = profile
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try { firestoreService.setOffline() } catch (_: Exception) {}
        auth.signOut()
        _currentUser.value = null
        _userProfile.value = null
    }

    suspend fun loadUserProfile(uid: String) {
        _userProfile.value = firestoreService.getUserProfile(uid)
    }

    suspend fun loadCloudState(): CloudState? {
        return try {
            val state = firestoreService.cloudLoad()
            Log.d("UserRepo", "Cloud state loaded: liked=${state?.likedSongs?.size}, playlists=${state?.playlists?.size}")
            state
        } catch (e: Exception) {
            Log.e("UserRepo", "Failed to load cloud state", e)
            null
        }
    }

    fun isSignedIn() = auth.currentUser != null
}
