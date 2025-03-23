package com.alpha.showcase.common.networkfile

import com.alpha.showcase.api.rclone.RcloneFileItem
import com.alpha.showcase.api.rclone.Remote
import com.alpha.showcase.api.rclone.SpaceInfo
import com.alpha.showcase.common.networkfile.model.NetworkFile
import com.alpha.showcase.common.networkfile.rclone.SERVE_PROTOCOL
import com.alpha.showcase.common.networkfile.storage.ext.toRemote
import com.alpha.showcase.common.networkfile.storage.remote.RcloneRemoteApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "Rclone:"

const val PROXY_PORT = 8899

const val DEFAULT_SERVE_PORT = 12121

const val COMMAND_LSJSON = "lsjson"
const val COMMAND_ABOUT = "about"
const val COMMAND_CONFIG = "config"
const val COMMAND_DELETE = "delete"
const val COMMAND_OBSCURE = "obscure"
const val COMMAND_COPY = "copy"
const val COMMAND_SERVE = "serve"
const val COMMAND_VERSION = "version"

const val OAUTH_PROCESS_REGEX = "go to the following link: ([^\\s]+)"


interface Rclone {

  val rClone: String

  val rCloneConfig: String

  val cacheDir: String

  val logFilePath: String

  val serveLogFilePath: String

  val downloadScope: CoroutineDispatcher

  fun createCommand(vararg args: String): Array<String> {

    val staticArgSize = if (loggingEnable) 4 else 3
    val arraySize = args.size + staticArgSize
    val command = Array(arraySize) {""}

    command[0] = rClone
    command[1] = "--config"
    command[2] = rCloneConfig

    if (loggingEnable) {
      command[3] = "-vvv"
    }
    var i = staticArgSize
    for (arg in args) {
      command[i ++] = arg
    }
    return command
  }

  fun createCommandWithOption(vararg args: String): Array<String> {
    val size = if (loggingEnable) 10 else 9
    val command = Array(size + args.size) {""}
    command[0] = rClone
    command[1] = "--cache-chunk-path"
    command[2] = cacheDir
    command[3] = "--cache-db-path"
    command[4] = cacheDir
    command[5] = "--cache-dir"
    command[6] = cacheDir
    command[7] = "--config"
    command[8] = rCloneConfig
    if (loggingEnable) {
      command[9] = "-vvv"
    }
    var index = size
    args.forEach {
      command[index ++] = it
    }
    return command
  }

  val loggingEnable: Boolean

  val proxyEnable: Boolean

  val proxyPort: Int

  fun logOutPut(log: String)

  suspend fun setUpAndWait(rcloneRemoteApi: RcloneRemoteApi): Boolean


  suspend fun setUpAndWaitOAuth(
    options: List<String>,
    block: ((String?) -> Unit)? = null
  ): Boolean

  fun deleteRemote(remoteName: String): Boolean

  fun obscure(pass: String): String?

  fun configCreate(options: List<String>): Any?

  private fun getConfigEnv(vararg options: String): Array<String> {
    val environmentValues = mutableListOf<String>()

    if (proxyEnable) {
      val noProxy = "localhost"
      val protocol = "http"
      val host = "localhost"
      val url = "$protocol://$host:$proxyPort"
      environmentValues.add("http_proxy=$url")
      environmentValues.add("https_proxy=$url")
      environmentValues.add("no_proxy=$noProxy")
    }

    environmentValues.add("TMPDIR=$cacheDir")
    environmentValues.add("RCLONE_LOCAL_NO_SET_MODTIME=true")

    // Allow the caller to overwrite any option for special cases
    val envVarIterator = environmentValues.iterator()
    while (envVarIterator.hasNext()) {
      val envVar = envVarIterator.next()
      val optionName = envVar.substring(0, envVar.indexOf('='))
      for (overwrite in options) {
        if (overwrite.startsWith(optionName)) {
          envVarIterator.remove()
          environmentValues.add(overwrite)
        }
      }
    }
    return environmentValues.toTypedArray()
  }
  


  fun getDirContent(
    remote: Remote,
    path: String = "",
    recursive: Boolean = false,
    startAsRoot: Boolean = false
  ): Result<List<RcloneFileItem>>


  fun logErrorOut(process: Any): String

  fun getRemotes(): Result<List<Remote>>

  fun getRemote(key: String, block: ((Remote?) -> Unit)?) {
    getRemotes().also {
      if (it.isSuccess) {
        it.getOrNull()?.forEach { remote ->
          if (remote.key == key) {
            block?.invoke(remote)
            return
          }
        }
      }
    }
  }


  fun serve(
    serveProtocol: SERVE_PROTOCOL,
    port: Int,
    allowRemoteAccess: Boolean,
    user: String? = null,
    passwd: String? = null,
    remote: String,
    servePath: String? = null,
    baseUrl: String? = null,
    openRC: Boolean = false,
    openWebGui: Boolean = false,
    rcUser: String? = null,
    rcPasswd: String? = null
  ): Any?

  fun getFileInfo(remote: Remote, path: String): Result<RcloneFileItem>

  suspend fun suspendGetFileInfo(remote: Remote, path: String): Result<RcloneFileItem>

  fun aboutRemote(remote: Remote): SpaceInfo

  fun rcloneVersion(): String

  fun encodePath(localFilePath: String): String

  fun genServeAuthPath(): String


  fun allocatePort(port: Int, allocateFallback: Boolean): Int


  suspend fun getFileDirItems(
    storage: RcloneRemoteApi,
    path: String,
    recursive: Boolean = false
  ): Result<List<NetworkFile>> {
    val fileList = mutableListOf<NetworkFile>()
    return withContext(Dispatchers.Default) {
      val remote = storage.toRemote()
      val result = getDirContent(remote, path, recursive)
      if (result.isSuccess) {
        result.getOrNull()?.forEach {
          if (!it.isDir) return@forEach
          fileList.add(
            NetworkFile(
              remote,
              it.path,
              it.name,
              it.isDir,
              it.size,
              it.mimeType,
              it.modTime,
              it.isBucket
            )
          )
        }
        Result.success(fileList)
      } else {
        Result.failure(Exception("Error Connect."))
      }
    }
  }


  suspend fun getFileItems(
    storage: RcloneRemoteApi,
    recursive: Boolean = false,
    onlyDir: Boolean = false,
    filterMime: String? = null
  ): Result<List<NetworkFile>> {
    val fileList = mutableListOf<NetworkFile>()
    return withContext(Dispatchers.Default) {
      val remote = storage.toRemote()
      val result = getDirContent(remote, storage.path, recursive)
      if (result.isSuccess) {
        result.getOrNull()?.run {
          if (filterMime != null) filter {
            it.mimeType == filterMime
          } else this
        }?.forEach {

          if (onlyDir && !it.isDir) return@forEach
          fileList.add(
            NetworkFile(
              remote,
              it.path,
              it.name,
              it.isDir,
              it.size,
              it.mimeType,
              it.modTime,
              it.isBucket
            )
          )
        }
        Result.success(fileList)
      } else {
        Result.failure(Exception("Error Connect."))
      }
    }
  }

  suspend fun getFileItems(
    storage: RcloneRemoteApi,
    recursive: Boolean = false,
    filter: ((NetworkFile) -> Boolean)?
  ): Result<List<NetworkFile>> {
    val fileList = mutableListOf<NetworkFile>()
    return withContext(Dispatchers.Default) {
      val remote = storage.toRemote()
      val result = getDirContent(remote, storage.path, recursive)
      if (result.isSuccess) {
        result.getOrNull()?.forEach {
          fileList.add(
            NetworkFile(
              remote,
              it.path,
              it.name,
              it.isDir,
              it.size,
              it.mimeType,
              it.modTime,
              it.isBucket
            )
          )
        }
        val filtered = fileList.filter {
          filter?.invoke(it) ?: true
        }
        Result.success(filtered)
      } else {
        Result.failure(Exception("Error Connect."))
      }
    }
  }

  suspend fun getFileInfo(storage: RcloneRemoteApi): Result<NetworkFile>{
    return withContext(Dispatchers.Default){
      val remote = storage.toRemote()
      val result = suspendGetFileInfo(remote, storage.path)
      if (result.isSuccess) {
        result.getOrNull()?.let {
            Result.success(
                NetworkFile(
                remote,
                storage.path,
                it.name,
                it.isDir,
                it.size,
                it.mimeType,
                it.modTime,
                it.isBucket
                )
            )
        }?: Result.failure(Exception("Empty info."))
      } else {
        Result.failure(Exception("Error Connect."))
      }
    }
  }

  suspend fun getFileInfo(rcloneRemoteApi: RcloneRemoteApi, path: String): Result<NetworkFile> {
    return withContext(Dispatchers.Default) {
      val remote = rcloneRemoteApi.toRemote()
      val result = suspendGetFileInfo(remote, path)
      if (result.isSuccess) {
        result.getOrNull()?.let {
            Result.success(
                NetworkFile(
                remote,
                path,
                it.name,
                it.isDir,
                it.size,
                it.mimeType,
                it.modTime,
                it.isBucket
                )
            )
        }?: Result.failure(Exception("Empty info."))
      } else {
        Result.failure(Exception("Error Connect."))
      }
    }
  }

  suspend fun parseConfig(remote: String, key: String): String?

  suspend fun updateConfig(remote: String, key: String, value: String): Boolean

}
