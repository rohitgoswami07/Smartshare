package com.rohit.smartshare.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<MessageResponse>

    @PUT("profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<MessageResponse>

    @POST("change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<MessageResponse>

    @POST("forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<MessageResponse>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @GET("buckets")
    suspend fun getBuckets(@Header("Authorization") token: String): Response<List<BucketResponse>>

    @POST("bucket")
    suspend fun createBucket(
        @Header("Authorization") token: String,
        @Body request: BucketRequest
    ): Response<BucketResponse>

    @PUT("bucket/{id}")
    suspend fun renameBucket(
        @Header("Authorization") token: String,
        @Path("id") bucketId: Int,
        @Body request: BucketRequest
    ): Response<BucketResponse>

    @DELETE("bucket/{id}")
    suspend fun deleteBucket(
        @Header("Authorization") token: String,
        @Path("id") bucketId: Int
    ): Response<MessageResponse>

    @GET("files/{bucketId}")
    suspend fun getFiles(
        @Header("Authorization") token: String,
        @Path("bucketId") bucketId: Int
    ): Response<List<FileResponse>>

    @Multipart
    @POST("upload/{bucketId}")
    suspend fun uploadFile(
        @Header("Authorization") token: String,
        @Path("bucketId") bucketId: Int,
        @Part file: MultipartBody.Part
    ): Response<FileResponse>

    @PUT("file/{id}")
    suspend fun renameFile(
        @Header("Authorization") token: String,
        @Path("id") fileId: Int,
        @Body request: FileRenameRequest
    ): Response<FileResponse>

    @DELETE("file/{id}")
    suspend fun deleteFile(
        @Header("Authorization") token: String,
        @Path("id") fileId: Int
    ): Response<MessageResponse>

    @POST("share/{bucketId}")
    suspend fun createShare(
        @Header("Authorization") token: String,
        @Path("bucketId") bucketId: Int
    ): Response<ShareResponse>

    @GET("share/{code}")
    suspend fun lookupShare(
        @Header("Authorization") token: String,
        @Path("code") code: String
    ): Response<ShareLookupResponse>

    @GET("shared-buckets")
    suspend fun getSharedBuckets(
        @Header("Authorization") token: String
    ): Response<List<SharedAccessedBucket>>

    @POST("friends/{username}")
    suspend fun addFriend(
        @Header("Authorization") token: String,
        @Path("username") username: String
    ): Response<FriendResponse>

    @DELETE("friends/{friendId}")
    suspend fun removeFriend(
        @Header("Authorization") token: String,
        @Path("friendId") friendId: Int
    ): Response<MessageResponse>

    @GET("friends")
    suspend fun getFriends(
        @Header("Authorization") token: String
    ): Response<List<FriendResponse>>

    @POST("messages/{friendId}/{bucketId}")
    suspend fun sendCode(
        @Header("Authorization") token: String,
        @Path("friendId") friendId: Int,
        @Path("bucketId") bucketId: Int
    ): Response<CodeMessageResponse>

    @GET("messages/{friendId}")
    suspend fun getMessages(
        @Header("Authorization") token: String,
        @Path("friendId") friendId: Int
    ): Response<List<CodeMessageResponse>>
}
