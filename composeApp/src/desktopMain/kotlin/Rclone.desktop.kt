@file:OptIn(ExperimentalEncodingApi::class)

import com.alpha.showcase.common.networkfile.COMMAND_ABOUT
import com.alpha.showcase.common.networkfile.COMMAND_CONFIG
import com.alpha.showcase.common.networkfile.COMMAND_COPY
import com.alpha.showcase.common.networkfile.COMMAND_DELETE
import com.alpha.showcase.common.networkfile.COMMAND_LSJSON
import com.alpha.showcase.common.networkfile.COMMAND_OBSCURE
import com.alpha.showcase.common.networkfile.COMMAND_SERVE
import com.alpha.showcase.common.networkfile.OAUTH_PROCESS_REGEX
import com.alpha.showcase.common.networkfile.Rclone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import okio.use
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.InterruptedIOException
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.util.Properties
import java.util.regex.Pattern
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import com.alpha.showcase.api.rclone.RcloneFileItem
import com.alpha.showcase.api.rclone.Remote
import com.alpha.showcase.api.rclone.SpaceInfo
import kotlinx.coroutines.asCoroutineDispatcher
import okio.buffer
import okio.source
import java.lang.StringBuilder
import java.security.SecureRandom
import java.util.concurrent.Executors
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import com.alpha.showcase.api.rclone.About
import com.alpha.showcase.api.rclone.RemoteConfig
import com.alpha.showcase.common.networkfile.COMMAND_VERSION
import com.alpha.showcase.common.networkfile.model.NetworkFile
import com.alpha.showcase.common.networkfile.rclone.SERVE_PROTOCOL
import com.alpha.showcase.common.networkfile.storage.ext.toRemote
import com.alpha.showcase.common.networkfile.storage.remote.RcloneRemoteApi
import com.alpha.showcase.common.networkfile.storage.remote.RemoteStorage

const val TAG = "DesktopRclone"
const val WIN_NATIVE_LIB_NAME = "rclone.exe"
const val MAC_NATIVE_LIB_NAME = "rclone"
const val LINUX_NATIVE_LIB_NAME = "rclone"

class DesktopRclone: Rclone {

  override val downloadScope = Executors.newFixedThreadPool(3).asCoroutineDispatcher()

  override val rClone = AppConfig.getRclonePath()
  override val rCloneConfig = AppConfig.getResourcesDirectory() + "rclone.conf"
  override val logFilePath = AppConfig.getResourcesDirectory() + "rclone.log"
  override val serveLogFilePath = AppConfig.getResourcesDirectory() + "rclone_serve.log"

  override val cacheDir: String = AppConfig.getCacheDirectory()
  override val loggingEnable: Boolean = true
  override val proxyEnable: Boolean = false
  override val proxyPort: Int = 8899
  override fun logOutPut(log: String) {
    println("$TAG: $log")
  }


  private fun createCommandWithOption(vararg args: String): Array<String> {
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

  override suspend fun setUpAndWait(rcloneRemoteApi: RcloneRemoteApi): Boolean {

    if (rcloneRemoteApi is RemoteStorage) {
      val createProcess = configCreate(rcloneRemoteApi.genRcloneOption())
      createProcess ?: apply {
        logOutPut("Error create remote !")
        return false
      }

      createProcess.apply {
        var exitCode: Int
        while (true) {
          try {
            exitCode = waitFor()
            break
          } catch (e: InterruptedException) {
            e.printStackTrace()
            try {
              exitCode = exitValue()
              break
            } catch (ignored: IllegalStateException) {
              ignored.printStackTrace()
            }
          }
        }
        return exitCode == 0
      }
      return false
    }
    return false
  }


  override suspend fun setUpAndWaitOAuth(
    options: List<String>,
    block: ((String?) -> Unit)?
  ): Boolean {

    return withContext(Dispatchers.IO) {

      suspendCancellableCoroutine{ continuation ->
        var createProcess: Process? = null
        continuation.invokeOnCancellation {
          createProcess?.destroy()
        }
        createProcess = configCreate(options)
        createProcess ?: apply {
          logOutPut("Error create remote !")
          continuation.resume(false)
        }
        try {
          createProcess?.apply {
            thread {
              try {
                val reader = BufferedReader(InputStreamReader(createProcess.errorStream))
                var line: String?
                while (reader.readLine().also {
                    line = it
                  } != null) {

                  line?.let {
                    logOutPut(it)
                    val pattern = Pattern.compile(OAUTH_PROCESS_REGEX, 0)
                    pattern.matcher(it).apply {
                      if (find()) {
                        val url = group(1)
                        if (url != null) {
                          logOutPut("oauth url: $url")
                          block?.invoke(url)
                        }
                      }
                    }
                  }
                }
              }catch (e: Exception){
                e.printStackTrace()
              }
            }

            var exitCode: Int
            while (true) {
              try {
                exitCode = waitFor()
                break
              } catch (e: InterruptedException) {
                e.printStackTrace()
                try {
                  exitCode = exitValue()
                  break
                } catch (ignored: IllegalStateException) {
                  ignored.printStackTrace()
                }
              }
            }
            continuation.resume(exitCode == 0)
          }
        }catch (e: Exception){
          e.printStackTrace()
          try {
            createProcess?.destroy()
          }catch (e: Exception){
            e.printStackTrace()
          }
          continuation.resume(false)
        }
      }
    }
  }

  override fun deleteRemote(remoteName: String): Boolean{
    val strings = createCommandWithOption(COMMAND_CONFIG, COMMAND_DELETE, remoteName)
    val process: Process
    return try {
      process = Runtime.getRuntime().exec(strings)
      process.waitFor()
      process.exitValue() == 0
    } catch (e: IOException) {
      logOutPut("$TAG deleteRemote: error delete remote $e")
      false
    } catch (e: InterruptedException) {
      logOutPut("$TAG deleteRemote: error delete remote $e")
      false
    }
  }

  override fun obscure(pass: String): String? {
    val command = createCommand(COMMAND_OBSCURE, pass)
    val process: Process
    return try {
      process = Runtime.getRuntime().exec(command)
      process.waitFor()
      if (process.exitValue() != 0) {
        return null
      }
      process.inputStream.source().use {
        it.buffer().readUtf8()
      }
    } catch (e: IOException) {
      logOutPut("$TAG obscure: error starting rclone $e")
      null
    } catch (e: InterruptedException) {
      logOutPut("$TAG obscure: error starting rclone $e")
      null
    }
  }

  override fun configCreate(options: List<String>): Process? {
    val command = createCommand(COMMAND_CONFIG, "create")
    val opt = options.toTypedArray()
    val commandWithOptions = Array(command.size + options.size) {""}
    System.arraycopy(command, 0, commandWithOptions, 0, command.size)
    System.arraycopy(opt, 0, commandWithOptions, command.size, opt.size)
    return try {
      Runtime.getRuntime().exec(commandWithOptions)
    } catch (e: IOException) {
      e.printStackTrace()
      logOutPut("$TAG configCreate: error starting rclone $e")
      null
    }
  }

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

  //todo
  fun encryptConfigFile(password: String): Boolean {
    val command = createCommand(COMMAND_CONFIG)
    return try {
      val process = Runtime.getRuntime().exec(command)

      val reader = BufferedReader(InputStreamReader(process.inputStream))
      val writer = BufferedWriter(OutputStreamWriter(process.outputStream))

      var setting = false
      var alreadySetPass = false
      var line: String
      while (reader.readLine().also { line = it } != null) {
        logOutPut("Received: $line")

        // 根据输出进行条件判断，然后发送新的命令
        if ("q) Quit config" == line) {
          if (setting) {
            writer.write("q\n")
            writer.flush()
          } else {
            writer.write("s\n")
            writer.flush()
            setting = true
          }
        } else if ("q) Quit to main menu" == line) {
          if (alreadySetPass) {
            writer.write("q\n")
            writer.flush()
          } else {
            writer.write("a\n")
            writer.flush()
          }
        } else if ("Enter NEW configuration password:" == line) {
          writer.write("$password\n")
          writer.flush()
        } else if ("Confirm NEW configuration password:" == line) {
          writer.write("$password\n")
          writer.flush()
          alreadySetPass = true
        }
      }

      process.waitFor()
      true
    } catch (e: IOException) {
      e.printStackTrace()
      logOutPut("$TAG configCreate: error starting rclone $e")
      false
    }

  }


  override fun getDirContent(
    remote: Remote,
    path: String,
    recursive: Boolean,
    startAsRoot: Boolean
  ): Result<List<RcloneFileItem>> {

    var remotePath = remote.key + ":"
    if (startAsRoot) remotePath += ""
    if (remotePath.compareTo("//" + remote.key) != 0) remotePath += path
    val process: Process

    try {
      val command = if (recursive) createCommandWithOption(
        COMMAND_LSJSON,
        remotePath,
        "-R"
      ) else createCommandWithOption(COMMAND_LSJSON, remotePath)
      process = Runtime.getRuntime().exec(command, getConfigEnv())

      val reader = BufferedReader(InputStreamReader(process.inputStream))
      var line: String?
      val output = StringBuilder()
      while (reader.readLine().also { line = it } != null) {
        output.append(line)
      }
      process.waitFor()

      if (process.exitValue() != 0 && (process.exitValue() != 6)) {
        val logErrorOut = logErrorOut(process)
        return Result.failure(Exception(logErrorOut))
      }

      val result = output.toString()
      val rCloneFileItemList = Json.decodeFromString<ArrayList<RcloneFileItem>>(result)
      return Result.success(rCloneFileItemList)
    } catch (ex: InterruptedException) {
      ex.printStackTrace()
      logOutPut(ex.toString())
    } catch (ex: IOException) {
      ex.printStackTrace()
      logOutPut(ex.toString())
    }

    return Result.failure(Exception("Error retrieving directory content."))
  }


  override fun logErrorOut(process: Any): String {
    val stringBuilder = java.lang.StringBuilder(100)
    try {
      BufferedReader(InputStreamReader((process as Process).errorStream)).use {reader ->
        var line: String?
        while (reader.readLine().also {line = it} != null) {
          stringBuilder.append(line).append("\n")
        }
      }
    } catch (iioe: InterruptedIOException) {
      iioe.printStackTrace()
      logOutPut("$TAG logErrorOutput: process died while reading. Log may be incomplete.")
    } catch (e: IOException) {
      e.printStackTrace()
      if ("Stream closed" == e.message) {
        logOutPut("$TAG logErrorOutput: could not read stderr, process stream is already closed")
      } else {
        logOutPut("$TAG logErrorOutput: $e")
      }
    }
    val log = stringBuilder.toString()
    logOutPut(log)

    return log
  }

  override fun getRemotes(): Result<List<Remote>> {

    val mutableList = mutableListOf<Remote>()
    val command = createCommand(COMMAND_CONFIG, "dump")
    val output = java.lang.StringBuilder()
    val process: Process

    try {
      process = Runtime.getRuntime().exec(command)
      val reader = BufferedReader(InputStreamReader(process.inputStream))
      var line: String?
      while (reader.readLine().also {line = it} != null) {
        output.append(line)
      }
      process.waitFor()
      if (process.exitValue() != 0) {
        val logErrorOut = logErrorOut(process)
        return Result.failure(Exception(logErrorOut))
      }

      val json = Json {ignoreUnknownKeys = true}
      val parseToJsonElement = json.parseToJsonElement(output.toString())

      parseToJsonElement.jsonObject.keys.forEach {
        val jsonElement = parseToJsonElement.jsonObject[it]
        val remoteConfig = json.decodeFromJsonElement<RemoteConfig>(jsonElement !!)
        mutableList.add(Remote(it, remoteConfig))
      }
      return Result.success(mutableList)

    } catch (e: IOException) {
      e.printStackTrace()
      logOutPut("$TAG  getRemotes: error retrieving remotes $e")
    } catch (e: InterruptedException) {
      e.printStackTrace()
      logOutPut("$TAG  getRemotes: error retrieving remotes $e")
    } catch (e: Exception) {
      e.printStackTrace()
      logOutPut("$TAG  getRemotes: error retrieving remotes $e")
    }

    return Result.failure(Exception("Error retrieving remotes."))

  }


  override fun serve(
    serveProtocol: SERVE_PROTOCOL,
    port: Int,
    allowRemoteAccess: Boolean,
    user: String?,
    passwd: String?,
    remote: String,
    servePath: String?,
    baseUrl: String?,
    openRC: Boolean,
    openWebGui: Boolean,
    rcUser: String?,
    rcPasswd: String?
  ): Process? {

//    val localRemotePath = if (remote.isType(LOCAL)) getLocalPathPrefix(remote) + "/" else ""
    val localRemotePath = ""

    val path =
      if (servePath == null || servePath.compareTo("//$remote") == 0)
        "$remote:$localRemotePath"
      else
        "$remote:$localRemotePath$servePath"
    val address = if (allowRemoteAccess) ":$port" else "127.0.0.1:$port"
    val protocol = serveProtocol.name
    val params =
      createCommandWithOption(
        COMMAND_SERVE,
        protocol,
        "--addr",
        address,
        path,
        "--vfs-cache-mode",
        "writes",
        "--no-modtime"
      ).toMutableList()
    user?.apply {
      params.add("--user")
      params.add(this)
    }
    passwd?.apply {
      params.add("--pass")
      params.add(this)
    }
    baseUrl?.apply {
      params.add("--baseurl")
      params.add(this)
    }

    if (loggingEnable && File(serveLogFilePath).exists()) {
      params.add("--log-file")
      params.add(serveLogFilePath)
    }

    if (openRC) {
      params.add("--rc")

      if (allowRemoteAccess) {
        params.add("--rc-serve")
      }
      if (rcUser != null && rcPasswd != null) {
        params.add("--rc-user")
        params.add(rcUser)
        params.add("--rc-pass")
        params.add(rcPasswd)
      } else {
        params.add("--rc-no-auth")
      }
      if (openWebGui){
        params.add("--rc-web-gui")
      }
    }
    val command = params.toTypedArray()
    return try {
      val process = Runtime.getRuntime().exec(command, getConfigEnv())
      val shutdownHook = Thread {
        process.destroy() // 尝试终止外部进程
      }
      // 添加shutdown hook到运行时环境
      Runtime.getRuntime().addShutdownHook(shutdownHook)
      process
    } catch (exception: IOException) {
      logOutPut("$TAG  serve error: $exception")
      null
    }
  }

  fun downloadFile(remote: Remote, path: String, file: RcloneFileItem, downloadToLocalPath: String): Result<File> {
    val command: Array<String>
    val remoteFilePath = "${remote.key}:$path/${file.path}"
    var localFilePath = if (file.isDir) {
      downloadToLocalPath + "/" + file.name
    } else {
      downloadToLocalPath
    }

    localFilePath = encodePath(localFilePath)

    command = createCommandWithOption(
      COMMAND_COPY,
      remoteFilePath,
      localFilePath,
      "--transfers",
      "1",
      "--stats=1s",
      "--stats-log-level",
      "NOTICE"
    )

    val env: Array<String> = getConfigEnv()
    return try {
      val downloadProcess = Runtime.getRuntime().exec(command, env)
      downloadProcess.apply {
        try {
          waitFor()
          return if (exitValue() == 0) {
            val readOnlyFile = File("$downloadToLocalPath/${file.name}")
            readOnlyFile.setReadOnly()
            Result.success(readOnlyFile)
          } else {
            val logErrorOut = logErrorOut(this)
            Result.failure(Exception(logErrorOut))
          }
        } catch (e: InterruptedException) {
          e.printStackTrace()
          logOutPut(e.toString())
        }
      }
      Result.failure(Exception("DownloadFile: error"))
    } catch (e: IOException) {
      logOutPut("$TAG downloadFile: error starting rclone $e")
      Result.failure(Exception("DownloadFile: error"))
    }
  }

  override fun getFileInfo(remote: Remote, path: String): Result<RcloneFileItem>{
    var remotePath = remote.key + ":"
    if (remotePath.compareTo("//" + remote.key) != 0) remotePath += path
    val process: Process

    try {
      val command = createCommandWithOption(COMMAND_LSJSON, remotePath, "--stat")
      process = Runtime.getRuntime().exec(command, getConfigEnv())

      val reader = BufferedReader(InputStreamReader(process.inputStream))
      var line: String?
      val output = StringBuilder()
      while (reader.readLine().also {line = it} != null) {
        output.append(line)
      }
      process.waitFor()

      if (process.exitValue() != 0 && (process.exitValue() != 6)) {
        val logErrorOut = logErrorOut(process)
        return Result.failure(Exception(logErrorOut))
      }

      val result = output.toString()
      val rCloneFileItemList = Json.decodeFromString<RcloneFileItem>(result)
      return Result.success(rCloneFileItemList)
    } catch (ex: InterruptedException) {
      ex.printStackTrace()
      logOutPut(ex.toString())
    } catch (ex: IOException) {
      ex.printStackTrace()
      logOutPut(ex.toString())
    }

    return Result.failure(Exception("Error retrieving directory content."))

  }

  override suspend fun suspendGetFileInfo(remote: Remote, path: String) =
    suspendCancellableCoroutine<Result<RcloneFileItem>> { continuation ->
      var remotePath = remote.key + ":"
      if (remotePath.compareTo("//" + remote.key) != 0) remotePath += path
      var process: Process? = null
      continuation.invokeOnCancellation {
        process?.destroy()
      }
      try {
        val command = createCommandWithOption(COMMAND_LSJSON, remotePath, "--stat")
        process = Runtime.getRuntime().exec(command, getConfigEnv())
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?
        val output = StringBuilder()
        while (reader.readLine().also { line = it } != null) {
          output.append(line)
        }
        process.waitFor()

        if (process.exitValue() != 0 && (process.exitValue() != 6)) {
          val logErrorOut = logErrorOut(process)
          continuation.resume(Result.failure(Exception(logErrorOut)))
        } else {
          val result = output.toString()
          val rCloneFileItemList = Json.decodeFromString<RcloneFileItem>(result)
          continuation.resume(Result.success(rCloneFileItemList))
        }
      } catch (ex: InterruptedException) {
        ex.printStackTrace()
        logOutPut(ex.toString())
        continuation.resume(Result.failure(Exception("Error retrieving directory content.")))
      } catch (ex: IOException) {
        ex.printStackTrace()
        logOutPut(ex.toString())
        continuation.resume(Result.failure(Exception("Error retrieving directory content.")))
      }
    }


  override fun aboutRemote(remote: Remote): SpaceInfo {
    val remoteName: String = remote.key + ':'
    val command = createCommand(COMMAND_ABOUT, "--json", remoteName)
    val output = java.lang.StringBuilder()
    val process: Process
    try {
      process = Runtime.getRuntime().exec(command, getConfigEnv())
      BufferedReader(InputStreamReader(process.inputStream)).use {reader ->
        var line: String?
        while (reader.readLine().also {line = it} != null) {
          output.append(line)
        }
      }
      process.waitFor()
      if (0 != process.exitValue()) {
        logOutPut("$TAG aboutRemote: rclone error, exit(%d) ${process.exitValue()}")
        logOutPut("$TAG aboutRemote: $output")
        logErrorOut(process)
        return SpaceInfo()
      }
    } catch (e: IOException) {
      logOutPut("$TAG aboutRemote: unexpected error $e")
    } catch (e: InterruptedException) {
      logOutPut("$TAG aboutRemote: unexpected error $e")
    }
    return try {
      val about = Json.decodeFromString<About>(output.toString())
      SpaceInfo(about.used, about.free, about.total, about.trashed)
    } catch (e: Exception) {
      logOutPut("$TAG aboutRemote: JSON format error $e")
      return SpaceInfo()
    }
  }

  override fun rcloneVersion(): String {
    val command = arrayOf(rClone, COMMAND_VERSION)
    val output = StringBuilder()
    val process: Process
    try {
      process = Runtime.getRuntime().exec(command)
      BufferedReader(InputStreamReader(process.inputStream)).use {reader ->
        var line: String?
        while (reader.readLine().also {line = it} != null) {
          output.append(line + "\n")
        }
      }
      process.waitFor()
      if (0 != process.exitValue()) {
        logOutPut("$TAG rcloneVersion: rclone error, exit(%d) ${process.exitValue()}")
        logOutPut("$TAG rcloneVersion: $output")
        logErrorOut(process)
        return output.toString()
      }
    } catch (e: IOException) {
      logOutPut("$TAG aboutRemote: unexpected error $e")
    } catch (e: InterruptedException) {
      logOutPut("$TAG aboutRemote: unexpected error $e")
    }
    return output.toString()
  }

  override fun encodePath(localFilePath: String): String {
    if (localFilePath.indexOf('\u0000') < 0) {
      return localFilePath
    }
    val localPathBuilder = java.lang.StringBuilder(localFilePath.length)
    for (c in localFilePath.toCharArray()) {
      if (c == '\u0000') {
        localPathBuilder.append('\u2400')
      } else {
        localPathBuilder.append(c)
      }
    }
    return localPathBuilder.toString()
  }


  @OptIn(ExperimentalEncodingApi::class)
  override fun genServeAuthPath(): String{
    val secureRandom = SecureRandom()
    val value = ByteArray(16)
    secureRandom.nextBytes(value)
    return Base64.encode(value, 0, value.size)
  }


  override fun allocatePort(port: Int, allocateFallback: Boolean): Int {
    try {
      ServerSocket(port).use {serverSocket ->
        serverSocket.reuseAddress = true
        return serverSocket.localPort
      }
    } catch (e: IOException) {
      if (allocateFallback) {
        return allocatePort(0, false)
      }
    }
    throw java.lang.IllegalStateException("No port available")
  }

  suspend fun getFiles(storage: RemoteStorage, filterMime: String? = null): Result<List<File>> {
    val fileList = mutableListOf<File>()
    return withContext(Dispatchers.IO){
      val remote = storage.toRemote()
      val result = getDirContent(remote, storage.path, false)
      if (result.isSuccess) {
        result.getOrNull()?.run {
          if (filterMime != null) filter {
            it.mimeType == filterMime
          } else this
        }?.forEach {
          getFile(storage, it).apply {
            if (isSuccess){
              getOrNull()?.apply {
                fileList.add(this)
              }
            }
          }
        }
        Result.success(fileList)
      } else {
        Result.failure(Exception("Error Connect."))
      }
    }
  }


  override suspend fun getFileDirItems(
    storage: RcloneRemoteApi,
    path: String,
    recursive: Boolean
  ): Result<List<NetworkFile>> {
    val fileList = mutableListOf<NetworkFile>()
    return withContext(Dispatchers.IO) {
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


  override suspend fun getFileItems(
    storage: RcloneRemoteApi,
    recursive: Boolean,
    onlyDir: Boolean,
    filterMime: String?
  ): Result<List<NetworkFile>> {
    val fileList = mutableListOf<NetworkFile>()
    return withContext(Dispatchers.IO) {
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

  override suspend fun getFileItems(
    storage: RcloneRemoteApi,
    recursive: Boolean,
    filter: ((NetworkFile) -> Boolean)?
  ): Result<List<NetworkFile>> {
    val fileList = mutableListOf<NetworkFile>()
    return withContext(Dispatchers.IO) {
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

  override suspend fun getFileInfo(storage: RcloneRemoteApi): Result<NetworkFile>{
    return withContext(Dispatchers.IO){
      val remote = storage.toRemote()
      val result = suspendGetFileInfo(remote, storage.path)
      if (result.isSuccess) {
        val info = result.getOrNull()
        if (info != null) {
          Result.success(
            NetworkFile(
              remote,
              storage.path,
              info.name,
              info.isDir,
              info.size,
              info.mimeType,
              info.modTime,
              info.isBucket
            )
          )
        } else {
          Result.failure(Exception("Empty info."))
        }
      } else {
        Result.failure(Exception("Error Connect."))
      }
    }
  }

  override suspend fun getFileInfo(rcloneRemoteApi: RcloneRemoteApi, path: String): Result<NetworkFile> {
    return withContext(Dispatchers.IO) {
      val remote = rcloneRemoteApi.toRemote()
      val result = suspendGetFileInfo(remote, path)
      if (result.isSuccess) {
        val info = result.getOrNull()
        if (info != null) {
          Result.success(
            NetworkFile(
              remote,
              path,
              info.name,
              info.isDir,
              info.size,
              info.mimeType,
              info.modTime,
              info.isBucket
            )
          )
        } else {
          Result.failure(Exception("Empty info."))
        }
      } else {
        Result.failure(Exception("Error Connect."))
      }
    }
  }

  suspend fun getFiles(
    storage: RemoteStorage,
    onSuccess: ((File) -> Unit)?,
    recursive: Boolean = false,
    filterMime: String? = null
  ) {
    withContext(Dispatchers.IO) {
      val result = getDirContent(storage.toRemote(), storage.path, recursive)
      if (result.isSuccess) {
        result.getOrNull()?.run {
          if (filterMime != null) {
            filter {
              it.mimeType == filterMime
            }
          } else {
            this
          }
        }?.forEach {
          getFile(storage, it).apply {
            if (this.isSuccess) {
              getOrNull()?.apply {
                onSuccess?.invoke(this)
              }
            }
          }
        }
      }
    }
  }

  suspend fun getFile(storage: RemoteStorage, file: RcloneFileItem): Result<File> {
    return withContext(downloadScope) {
      val savePath = "$cacheDir/${storage.name}/${storage.path}/"
      downloadFile(storage.toRemote(), storage.path, file, savePath)
    }
  }

  override suspend fun parseConfig(remote: String, key: String): String? {
    return withContext(Dispatchers.IO) {
      val configFile = File(rCloneConfig)
      if (configFile.exists()) {
        val reader = configFile.inputStream().reader()

        val properties = Properties()
        lateinit var section: String

        reader.readLines().forEach { line ->
          when {
            line.isEmpty() || line.startsWith(";") -> {
              // 忽略空行和注释行
            }
            line.startsWith("[") && line.endsWith("]") -> {
              // 解析 section
              section = line.substring(1, line.length - 1)
            }
            else -> {
              // 解析 key-value pair
              val (k, v) = line.split("=").map { it.trim() }
              properties.setProperty("$section.$k", v)
            }
          }
        }
        properties.getProperty("$remote.$key")?.let {
          it
        }
      }else {
        null
      }
    }
  }

  override suspend fun updateConfig(remote: String, key: String, value: String): Boolean{
    return withContext(Dispatchers.IO) {
      val configFile = File(rCloneConfig)
      if (configFile.exists()) {
        val lines = configFile.inputStream().reader().readLines()
        // Create a map to store sections and properties
        val map = LinkedHashMap<String, LinkedHashMap<String, String>>()
        var currentSection = ""

        // Loop through lines and parse sections and properties
        for (line in lines) {
          if (line.startsWith("[")) {
            // Found new section
            currentSection = line.substring(1, line.lastIndexOf("]"))
            map[currentSection] = LinkedHashMap()
          } else if (line.contains("=")) {
            // Found property
            val keyValuePair = line.split("=")
            val k = keyValuePair[0].trim()
            val v = keyValuePair[1].trim()
            map[currentSection]?.put(k, v)
          }
        }

        // Update property value
        map[remote]?.put(key, value)

        // Convert map back to INI string
        val sb = StringBuilder()
        for ((section, properties) in map) {
          sb.append("[$section]\n")
          for ((k, v) in properties) {
            sb.append("$k = $v\n")
          }
          sb.append("\n")
        }

        try {
          configFile.outputStream().writer().use {
            it.write(sb.toString())
          }
          true
        }catch (e: Exception){
          e.printStackTrace()
          false
        }
      }else {
        false
      }
    }
  }

}

object AppConfig {
  fun getResourcesDirectory(): String {
    val os = getPlatformName()
    return when {
      os.contains("win") -> File(System.getProperty("compose.application.resources.dir")).absolutePath + "\\"
      else -> File(System.getProperty("compose.application.resources.dir")).absolutePath + "/"
    }
  }

  fun getRclonePath(): String{
    val os = System.getProperty("os.name").lowercase()
    return when {
      os.contains("win") -> getResourcesDirectory() + WIN_NATIVE_LIB_NAME
      os.contains("mac") -> getResourcesDirectory() + MAC_NATIVE_LIB_NAME
      else -> getResourcesDirectory() + LINUX_NATIVE_LIB_NAME
    }
  }

  fun getCacheDirectory(): String {
    return getResourcesDirectory() + "cache"
  }

  fun isWindows(): Boolean {
    return getPlatformName().contains("win")
  }

  fun isMac(): Boolean {
    return getPlatformName().contains("mac")
  }

  fun getPlatformName(): String {
    return System.getProperty("os.name").lowercase()
  }
}
