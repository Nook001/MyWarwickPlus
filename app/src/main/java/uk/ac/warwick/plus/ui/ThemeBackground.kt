package uk.ac.warwick.plus.ui

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas as BitmapCanvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.core.graphics.createBitmap
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import java.util.Random

data class BackgroundKey(val theme: ColourTheme, val texture: Boolean, val width: Int, val height: Int)

fun backgroundKey(appearance: Appearance, aspect: Float): BackgroundKey {
    val ratio = aspect.coerceIn(.2f, 5f)
    fun quantize(value: Float) = ((value / 16).roundToInt() * 16).coerceIn(16, 1024)
    return if (ratio <= 1) BackgroundKey(appearance.theme, appearance.texture, quantize(1024 * ratio), 1024)
        else BackgroundKey(appearance.theme, appearance.texture, 1024, quantize(1024 / ratio))
}

class ThemeBackgroundCache {
    private val cache = LruCache<BackgroundKey, Bitmap>(2)
    private val mutex = Mutex()
    private val grain by lazy {
        val random = Random(73219)
        Bitmap.createBitmap(IntArray(128 * 128) {
            val grey = random.nextInt(256)
            android.graphics.Color.rgb(grey, grey, grey)
        }, 128, 128, Bitmap.Config.ARGB_8888)
    }
    suspend fun get(key: BackgroundKey): Bitmap = mutex.withLock {
        cache[key] ?: withContext(Dispatchers.Default) { render(key) }.also { cache.put(key, it) }
    }
    private fun render(key: BackgroundKey): Bitmap {
        val bitmap = createBitmap(key.width, key.height)
        val canvas = BitmapCanvas(bitmap)
        val palette = key.theme.palette()
        canvas.drawColor(palette.background.toArgb())
        val locations = floatArrayOf(0f, .2f, .5f, .8f, 1f)
        val alpha = floatArrayOf(.78f, .702f, .351f, .0624f, 0f)
        val centers = arrayOf(.08f to .02f, 1.05f to .48f, .08f to 1.04f)
        val radii = arrayOf(.85f to .62f, .90f to .65f, .95f to .65f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // CSS lists the first gradient on top; raster compositing draws that layer last.
        for (index in 2 downTo 0) {
            val shader = RadialGradient(0f, 0f, 1f, alpha.map { palette.spots[index].copy(alpha = it).toArgb() }.toIntArray(),
                locations, Shader.TileMode.CLAMP)
            shader.setLocalMatrix(Matrix().apply {
                setScale(key.width * radii[index].first, key.height * radii[index].second)
                postTranslate(key.width * centers[index].first, key.height * centers[index].second)
            })
            paint.shader = shader
            canvas.drawRect(0f, 0f, key.width.toFloat(), key.height.toFloat(), paint)
        }
        if (key.texture) {
            paint.shader = BitmapShader(grain, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            paint.alpha = 6
            canvas.drawRect(0f, 0f, key.width.toFloat(), key.height.toFloat(), paint)
        }
        return bitmap
    }
    // Do not recycle evicted bitmaps: Compose can still hold the last displayed image.
}

private val Backgrounds = ThemeBackgroundCache()

@Composable
fun ThemeBackground(modifier: Modifier = Modifier) {
    val appearance = LocalAppearance.current
    var measuredSize by remember { mutableStateOf(IntSize.Zero) }
    Box(modifier.fillMaxSize().background(appearance.theme.palette().background).onSizeChanged { measuredSize = it }) {
        if (measuredSize.width == 0 || measuredSize.height == 0) return@Box
        val key = backgroundKey(appearance, measuredSize.width.toFloat() / measuredSize.height)
        val cached by produceState<Pair<BackgroundKey, Bitmap>?>(null, key) { value = key to Backgrounds.get(key) }
        val image = cached?.takeIf { it.first == key }?.second
        if (image != null) {
            val source = remember(image) { image.asImageBitmap() }
            Canvas(Modifier.fillMaxSize().testTag("background-ready-${key.theme.id}-${key.texture}")) {
                drawImage(source, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = FilterQuality.Low)
            }
        }
    }
}
