import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.alpha.showcase.common.networkfile.Rclone
import com.alpha.showcase.common.AndroidRclone


lateinit var AndroidApp: Application
class AndroidPlatform : Platform {
    override val name: String = "$PLATFORM_ANDROID ${Build.VERSION.SDK_INT}"
    override fun openUrl(url: String) {
        val uri = Uri.parse(url)
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = uri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        AndroidApp.startActivity(intent)
    }

    override fun getConfigDirectory(): String = AndroidApp.filesDir.absolutePath
}

actual fun getPlatform(): Platform = AndroidPlatform()
actual fun randomUUID(): String = java.util.UUID.randomUUID().toString()
actual fun rclone(): Rclone = AndroidRclone(AndroidApp)