import com.alpha.showcase.common.networkfile.Rclone
import com.alpha.showcase.api.rclone.RcloneFileItem
import com.alpha.showcase.api.rclone.Remote
import com.alpha.showcase.api.rclone.SpaceInfo
import com.alpha.showcase.common.networkfile.rclone.SERVE_PROTOCOL
import com.alpha.showcase.common.networkfile.storage.remote.RcloneRemoteApi
import kotlinx.coroutines.Dispatchers

class IosRclone: Rclone {

    override val downloadScope = Dispatchers.Default
    override val rClone = "rclone"
    override val rCloneConfig = "rclone.conf"
    override val logFilePath = "rclone.log"
    override val serveLogFilePath = "rclone_serve.log"

    override val cacheDir: String = "cache"
    override val loggingEnable: Boolean = true
    override val proxyEnable: Boolean = false
    override val proxyPort: Int = 8899

    override fun logOutPut(log: String) {
        TODO("Not yet implemented")
    }

    override suspend fun setUpAndWait(rcloneRemoteApi: RcloneRemoteApi): Boolean {
        return true
    }

    override suspend fun setUpAndWaitOAuth(
        options: List<String>,
        block: ((String?) -> Unit)?
    ): Boolean {
        return true
    }

    override fun deleteRemote(remoteName: String): Boolean {
        return true
    }

    override fun obscure(pass: String): String? {
        return pass
    }

    override fun configCreate(options: List<String>): Any? {
        return null
    }

    override fun getDirContent(
        remote: Remote,
        path: String,
        recursive: Boolean,
        startAsRoot: Boolean
    ): Result<List<RcloneFileItem>> {
        return Result.success(emptyList())
    }

    override fun logErrorOut(process: Any): String {
        return ""
    }

    override fun getRemotes(): Result<List<Remote>> {
        return Result.success(emptyList())
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
    ): Any? {
        TODO("Not yet implemented")
    }

    override fun getFileInfo(remote: Remote, path: String): Result<RcloneFileItem> {
        TODO("Not yet implemented")
    }

    override suspend fun suspendGetFileInfo(remote: Remote, path: String): Result<RcloneFileItem> {
        TODO("Not yet implemented")
    }

    override fun aboutRemote(remote: Remote): SpaceInfo {
        TODO("Not yet implemented")
    }

    override fun rcloneVersion(): String {
        TODO("Not yet implemented")
    }

    override fun encodePath(localFilePath: String): String {
        TODO("Not yet implemented")
    }

    override fun genServeAuthPath(): String {
        TODO("Not yet implemented")
    }

    override fun allocatePort(port: Int, allocateFallback: Boolean): Int {
        TODO("Not yet implemented")
    }

    override suspend fun parseConfig(remote: String, key: String): String? {
        TODO("Not yet implemented")
    }

    override suspend fun updateConfig(remote: String, key: String, value: String): Boolean {
        TODO("Not yet implemented")
    }
}