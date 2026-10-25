package com.clipsmile.app.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.sin

/**
 * A lit pet on the rug. Every animal uses the same calm breath.
 * The pug's skeletal clip is not played: that loop was an eye blink.
 * A root yaw and a fit scale face the mesh at the camera and keep the head in frame.
 * A tap still turns the pet toward the finger.
 */
class PetCanvas @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val engine = Engine(context.applicationContext)

    init {
        setEGLContextClientVersion(2)
        setRenderer(engine)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun setModel(assetName: String) {
        engine.wanted = "dogs/$assetName.json"
    }

    fun playHappy() {
        engine.happyAt = SystemClock.uptimeMillis()
    }

    fun playShake() {
        engine.shakeAt = SystemClock.uptimeMillis()
    }

    private var lastX = 0f
    private var lastY = 0f
    private var downX = 0f
    private var downY = 0f
    private var dragged = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                lastX = event.x
                lastY = event.y
                dragged = false
                engine.spinHeld = true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                lastX = event.x
                lastY = event.y
                if (abs(event.x - downX) > 18f || abs(event.y - downY) > 18f) dragged = true
                engine.userYaw = (engine.userYaw + dx * 0.55f).coerceIn(-170f, 170f)
                engine.userPitch = (engine.userPitch + dy * 0.25f).coerceIn(-40f, 40f)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                engine.spinHeld = false
                if (!dragged && event.actionMasked == MotionEvent.ACTION_UP) {
                    val width = width.coerceAtLeast(1)
                    engine.lookTarget = (event.x / width) * 2f - 1f
                    engine.lookAt = SystemClock.uptimeMillis()
                    performClick()
                }
            }
        }
        return true
    }

    private class Part(
        val name: String,
        val parent: Int,
        val restT: FloatArray,
        val restR: FloatArray,
        val restS: FloatArray,
        val color: FloatArray,
        val spec: Float,
        val pos: FloatBuffer,
        val nor: FloatBuffer,
        val col: FloatBuffer?,
        val uv: FloatBuffer?,
        val texture: String?,
        val idx: ShortBuffer,
        val indexCount: Int
    )

    private class Engine(private val context: Context) : Renderer {
        @Volatile var wanted: String = "dogs/pug.json"
        @Volatile var happyAt: Long = 0L
        @Volatile var shakeAt: Long = 0L
        @Volatile var lookTarget: Float = 0f
        @Volatile var lookAt: Long = 0L
        @Volatile var userYaw: Float = 0f
        @Volatile var userPitch: Float = 0f
        @Volatile var spinHeld: Boolean = false
        private var lastSpinMs = 0L

        private var loadedName: String? = null
        private var parts: List<Part> = emptyList()
        private var morph: PetMorph? = null
        private var clipStart = 0L
        private var eyeX = 0.22f
        private var eyeY = 0.64f
        private var eyeZ = 1.62f
        private var lookY = 0.38f
        private var viewW = 1
        private var viewH = 1
        private var program = 0
        private var aPos = 0
        private var aNor = 0
        private var aCol = 0
        private var aUV = 0
        private var uViewProj = 0
        private var uModel = 0
        private var uNormal = 0
        private var uColor = 0
        private var uSpec = 0
        private var uLight = 0
        private var uEye = 0
        private var uTex = 0
        private var uUseTex = 0
        private var whiteTex = 0
        private var coatTex = 0
        private var coatFor: String? = null
        private var fitYaw = 0f
        private var fitPitch = 0f
        private var fitScale = 1f
        private var fitOffX = 0f
        private var fitOffY = 0f
        private var fitOffZ = 0f
        private var fitLift = 0f
        private val viewProj = FloatArray(16)
        private val view = FloatArray(16)
        private val proj = FloatArray(16)
        private val world = Array(48) { FloatArray(16) }
        private val local = FloatArray(16)
        private val normal = FloatArray(9)
        private val inverse = FloatArray(16)
        private val transposed = FloatArray(16)
        private var groundPos = 0
        private var groundNor = 0
        private var groundIdx = 0
        private var groundCount = 0
        private var cubePos = 0
        private var cubeNor = 0
        private var cubeIdx = 0
        private var cubeCount = 0

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(0.58f, 0.74f, 0.86f, 1f)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glDisable(GLES20.GL_CULL_FACE)
            program = link(VERT, FRAG)
            aPos = GLES20.glGetAttribLocation(program, "aPos")
            aNor = GLES20.glGetAttribLocation(program, "aNor")
            aCol = GLES20.glGetAttribLocation(program, "aCol")
            aUV = GLES20.glGetAttribLocation(program, "aUV")
            uViewProj = GLES20.glGetUniformLocation(program, "uViewProj")
            uModel = GLES20.glGetUniformLocation(program, "uModel")
            uNormal = GLES20.glGetUniformLocation(program, "uNormal")
            uColor = GLES20.glGetUniformLocation(program, "uColor")
            uSpec = GLES20.glGetUniformLocation(program, "uSpec")
            uLight = GLES20.glGetUniformLocation(program, "uLight")
            uEye = GLES20.glGetUniformLocation(program, "uEye")
            uTex = GLES20.glGetUniformLocation(program, "uTex")
            uUseTex = GLES20.glGetUniformLocation(program, "uUseTex")
            loadedName = null
            coatFor = null
            whiteTex = solidTexture()
            uploadGround()
            uploadCube()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            viewW = width
            viewH = height
            applyCamera()
        }

        private fun applyCamera() {
            val aspect = viewW.toFloat() / viewH.coerceAtLeast(1)
            Matrix.perspectiveM(proj, 0, 32f, aspect, 0.05f, 30f)
            Matrix.setLookAtM(view, 0, eyeX, eyeY, eyeZ, 0f, lookY, 0.02f, 0f, 1f, 0f)
            Matrix.multiplyMM(viewProj, 0, proj, 0, view, 0)
        }

        override fun onDrawFrame(gl: GL10?) {
            ensureModel()
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            if (program == 0) return
            val now = SystemClock.uptimeMillis()
            val spinDt = if (lastSpinMs == 0L) 0.016f else ((now - lastSpinMs).coerceAtMost(48L)) / 1000f
            lastSpinMs = now
            if (!spinHeld) {
                val k = (spinDt * 7f).coerceIn(0f, 1f)
                userYaw += (0f - userYaw) * k
                userPitch += (0f - userPitch) * k
                if (abs(userYaw) < 0.2f) userYaw = 0f
                if (abs(userPitch) < 0.15f) userPitch = 0f
            }
            val time = now / 1000f
            val happy = pulse(now, happyAt, 0.9f)
            val shake = pulse(now, shakeAt, 0.7f)
            val lookAge = (now - lookAt) / 1000f
            val look = if (lookAge in 0f..1.3f) lookTarget * (1f - lookAge / 1.3f) else 0f
            val blink = blinkScale(time)
            val body = parts.firstOrNull()
            val seconds = (now - clipStart) / 1000f
            val morphing = morph
            if (body != null && morphing != null) {
                morphing.write(seconds, body.pos)
            }

            GLES20.glUseProgram(program)
            GLES20.glUniformMatrix4fv(uViewProj, 1, false, viewProj, 0)
            GLES20.glUniform3f(uLight, 0.22f, 0.88f, 0.55f)
            GLES20.glUniform3f(uEye, eyeX, eyeY, eyeZ)
            drawRoom()

            for (index in parts.indices) {
                val part = parts[index]
                pose(part, time, happy, shake, look, blink)
                val parent = if (part.parent >= 0) world[part.parent] else IDENTITY
                Matrix.multiplyMM(world[index], 0, parent, 0, local, 0)
                normalOf(world[index])
                GLES20.glUniformMatrix4fv(uModel, 1, false, world[index], 0)
                GLES20.glUniformMatrix3fv(uNormal, 1, false, normal, 0)
                GLES20.glUniform3fv(uColor, 1, part.color, 0)
                GLES20.glUniform1f(uSpec, part.spec)
                drawPart(part)
            }
        }

        private fun pose(part: Part, time: Float, happy: Float, shake: Float, look: Float, blink: Float) {
            var tx = part.restT[0]
            var ty = part.restT[1]
            var tz = part.restT[2]
            var rx = part.restR[0]
            var ry = part.restR[1]
            var rz = part.restR[2]
            var sx = part.restS[0]
            var sy = part.restS[1]
            var sz = part.restS[2]
            val name = part.name
            when {
                name == "body" -> {
                    val breath = sin(time * 2.1f)
                    val idle = sin(time * 1.5f)
                    sy *= 1f + breath * 0.055f - happy * 0.06f
                    sx *= 1f - breath * 0.025f + happy * 0.05f
                    sz *= 1f - breath * 0.02f
                    ty += idle * 0.022f + happy * 0.08f
                    rx += idle * 2.4f
                    ry += sin(time * 0.85f) * 7f + look * 14f
                    rz += sin(time * 26f) * shake * 10f
                }
                name == "head" -> {
                    ry += sin(time * 0.7f) * 12f + look * 20f
                    rx += sin(time * 0.5f) * 6f - happy * 14f
                    rz += sin(time * 1.1f) * 3f
                }
                name.startsWith("ear") -> {
                    val phase = if (name.contains("L")) 0f else 1.4f
                    rz += sin(time * 2.4f + phase) * (6f + happy * 14f)
                }
                name.startsWith("tail") -> {
                    // Tail runs back along -Z, so a yaw is the wag.
                    ry += sin(time * 8f) * (24f + happy * 28f)
                    rx += abs(sin(time * 8f)) * (8f + happy * 10f)
                }
                name.startsWith("leg") -> {
                    val left = name.endsWith("L")
                    val phase = if (left) 0f else Math.PI.toFloat()
                    rx += sin(time * 2.4f + phase) * (4f + happy * 6f)
                }
                name.startsWith("eye") || name.startsWith("pupil") || name.startsWith("spark") -> sy *= blink
                name == "mouth" || name == "jaw" || name == "tongue" -> {
                    sy *= 1f + happy * 2.2f
                    sz *= 1f + happy * 0.35f
                }
            }
            if (part.parent < 0) {
                ry += userYaw
                rx += userPitch
            }
            Matrix.setIdentityM(local, 0)
            Matrix.translateM(local, 0, tx, ty, tz)
            Matrix.rotateM(local, 0, rz, 0f, 0f, 1f)
            Matrix.rotateM(local, 0, ry, 0f, 1f, 0f)
            Matrix.rotateM(local, 0, rx, 1f, 0f, 0f)
            if (part.parent < 0) {
                // Android applies the last call first: center, then scale, then face the camera.
                Matrix.rotateM(local, 0, fitPitch, 1f, 0f, 0f)
                Matrix.rotateM(local, 0, fitYaw, 0f, 1f, 0f)
                Matrix.translateM(local, 0, 0f, fitLift, 0f)
                Matrix.scaleM(local, 0, sx * fitScale, sy * fitScale, sz * fitScale)
                Matrix.translateM(local, 0, fitOffX, fitOffY, fitOffZ)
            } else {
                Matrix.scaleM(local, 0, sx, sy, sz)
            }
        }

        private fun prepareFit(asset: String, part: Part?) {
            fitYaw = 0f
            fitPitch = 0f
            fitScale = 1f
            fitOffX = 0f
            fitOffY = 0f
            fitOffZ = 0f
            fitLift = 0f
            if (part == null) return
            val id = asset.substringAfterLast('/').removeSuffix(".json")
            if (id == "pug" || id == "cat") return
            val buf = part.pos
            buf.position(0)
            val count = buf.capacity() / 3
            if (count <= 0) return
            var minX = Float.POSITIVE_INFINITY
            var minY = Float.POSITIVE_INFINITY
            var minZ = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY
            var maxY = Float.NEGATIVE_INFINITY
            var maxZ = Float.NEGATIVE_INFINITY
            repeat(count) {
                val x = buf.get()
                val y = buf.get()
                val z = buf.get()
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                if (z < minZ) minZ = z
                if (z > maxZ) maxZ = z
            }
            buf.position(0)
            val height = (maxY - minY).coerceAtLeast(0.001f)
            val span = maxOf(maxX - minX, maxZ - minZ).coerceAtLeast(0.001f)
            val targetHeight = when (id) {
                "smurf" -> 0.60f
                "dissiz" -> 0.36f
                "pop" -> 0.52f
                "ucat" -> 0.58f
                else -> if (height > 0.74f) 0.66f else height
            }
            var scale = targetHeight / height
            val spanCap = if (id == "dissiz") 1.05f else 0.90f
            if (span * scale > spanCap) scale = spanCap / span
            fitScale = scale
            fitYaw = FACE_YAW[id] ?: 0f
            fitPitch = FACE_PITCH[id] ?: 0f
            fitOffX = -((minX + maxX) / 2f)
            fitOffY = -minY
            fitOffZ = -((minZ + maxZ) / 2f)
            fitLift = 0.02f
        }

        private fun ensureModel() {
            val name = wanted
            if (name == loadedName) return
            parts = load(name)
            loadedName = name
            prepareFit(name, parts.firstOrNull())
            eyeX = 0.18f
            eyeY = 0.50f
            eyeZ = 2.05f
            lookY = 0.30f
            applyCamera()
            morph = loadMorph(name)
            clipStart = nowMillis()
            val coat = parts.firstOrNull { it.texture != null }?.texture
            if (coat != coatFor) {
                if (coatTex != 0) {
                    GLES20.glDeleteTextures(1, intArrayOf(coatTex), 0)
                    coatTex = 0
                }
                coatFor = coat
                if (coat != null) coatTex = uploadTexture(coat)
            }
        }

        private fun loadMorph(model: String): PetMorph? {
            val frames = model.removeSuffix(".json") + ".frames"
            return try {
                context.assets.open(frames).use { PetMorph.open(it) }
            } catch (_: Exception) {
                null
            }
        }

        private fun nowMillis(): Long = SystemClock.uptimeMillis()

        private fun load(asset: String): List<Part> {
            val text = context.assets.open(asset).bufferedReader().use { it.readText() }
            val array = JSONObject(text).getJSONArray("parts")
            val list = ArrayList<Part>(array.length())
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                list.add(
                    Part(
                        name = item.getString("name"),
                        parent = item.getInt("parent"),
                        restT = floats(item.getJSONArray("t")),
                        restR = floats(item.getJSONArray("r")),
                        restS = floats(item.getJSONArray("s")),
                        color = floats(item.getJSONArray("color")),
                        spec = item.getDouble("spec").toFloat(),
                        pos = buffer(floats(item.getJSONArray("positions"))),
                        nor = buffer(floats(item.getJSONArray("normals"))),
                        col = if (item.has("colors")) buffer(floats(item.getJSONArray("colors"))) else null,
                        uv = if (item.has("uv")) buffer(floats(item.getJSONArray("uv"))) else null,
                        texture = if (item.has("texture")) item.getString("texture") else null,
                        idx = shorts(item.getJSONArray("indices")),
                        indexCount = item.getJSONArray("indices").length()
                    )
                )
            }
            return list
        }

        private fun drawPart(part: Part) {
            GLES20.glEnableVertexAttribArray(aPos)
            GLES20.glEnableVertexAttribArray(aNor)
            part.pos.position(0)
            part.nor.position(0)
            GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 0, part.pos)
            GLES20.glVertexAttribPointer(aNor, 3, GLES20.GL_FLOAT, false, 0, part.nor)
            if (part.col != null) {
                GLES20.glEnableVertexAttribArray(aCol)
                part.col.position(0)
                GLES20.glVertexAttribPointer(aCol, 3, GLES20.GL_FLOAT, false, 0, part.col)
                GLES20.glUniform3f(uColor, 1f, 1f, 1f)
            } else {
                GLES20.glDisableVertexAttribArray(aCol)
                GLES20.glVertexAttrib3f(aCol, 1f, 1f, 1f)
                GLES20.glUniform3fv(uColor, 1, part.color, 0)
            }
            if (part.uv != null && part.texture != null && coatTex != 0) {
                GLES20.glEnableVertexAttribArray(aUV)
                part.uv.position(0)
                GLES20.glVertexAttribPointer(aUV, 2, GLES20.GL_FLOAT, false, 0, part.uv)
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, coatTex)
                GLES20.glUniform1i(uTex, 0)
                GLES20.glUniform1f(uUseTex, 1f)
            } else {
                GLES20.glDisableVertexAttribArray(aUV)
                GLES20.glVertexAttrib2f(aUV, 0f, 0f)
                GLES20.glUniform1f(uUseTex, 0f)
            }
            part.idx.position(0)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, part.indexCount, GLES20.GL_UNSIGNED_SHORT, part.idx)
        }

        private fun drawRoom() {
            // Wall, window and floor so the dog sits in a room instead of empty space.
            box(0f, 1.25f, -1.25f, 5.2f, 3.4f, 0.08f, 0.58f, 0.74f, 0.86f, 0.04f)
            box(-1.15f, 1.45f, -1.18f, 0.85f, 1.05f, 0.04f, 0.78f, 0.90f, 0.98f, 0.15f)
            box(-1.15f, 1.45f, -1.16f, 0.04f, 1.05f, 0.02f, 0.92f, 0.95f, 0.98f, 0.1f)
            box(-1.15f, 1.45f, -1.16f, 0.85f, 0.04f, 0.02f, 0.92f, 0.95f, 0.98f, 0.1f)
            box(0.95f, 1.55f, -1.18f, 0.55f, 0.42f, 0.04f, 0.55f, 0.34f, 0.22f, 0.08f)
            box(0.95f, 1.55f, -1.15f, 0.42f, 0.30f, 0.02f, 0.98f, 0.86f, 0.55f, 0.12f)
            box(0f, -0.04f, 0.35f, 5.2f, 0.08f, 3.6f, 0.72f, 0.50f, 0.32f, 0.06f)
            box(0f, 0.02f, -1.05f, 5.2f, 0.10f, 0.08f, 0.93f, 0.90f, 0.84f, 0.05f)
            box(0.78f, 0.08f, 0.62f, 0.22f, 0.10f, 0.22f, 0.86f, 0.32f, 0.28f, 0.25f)
            Matrix.setIdentityM(local, 0)
            Matrix.translateM(local, 0, 0f, 0.015f, 0.15f)
            Matrix.scaleM(local, 0, 1.15f, 1f, 0.85f)
            paint(local, 0.93f, 0.55f, 0.38f, 0.05f, groundPos, groundNor, groundIdx, groundCount)
        }

        private fun box(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float, spec: Float) {
            Matrix.setIdentityM(local, 0)
            Matrix.translateM(local, 0, x, y, z)
            Matrix.scaleM(local, 0, sx, sy, sz)
            paint(local, r, g, b, spec, cubePos, cubeNor, cubeIdx, cubeCount)
        }

        private fun paint(model: FloatArray, r: Float, g: Float, b: Float, spec: Float, pos: Int, nor: Int, idx: Int, count: Int) {
            normalOf(model)
            GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0)
            GLES20.glUniformMatrix3fv(uNormal, 1, false, normal, 0)
            GLES20.glDisableVertexAttribArray(aCol)
            GLES20.glVertexAttrib3f(aCol, 1f, 1f, 1f)
            GLES20.glDisableVertexAttribArray(aUV)
            GLES20.glVertexAttrib2f(aUV, 0f, 0f)
            GLES20.glUniform1f(uUseTex, 0f)
            GLES20.glUniform3f(uColor, r, g, b)
            GLES20.glUniform1f(uSpec, spec)
            GLES20.glEnableVertexAttribArray(aPos)
            GLES20.glEnableVertexAttribArray(aNor)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, pos)
            GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 0, 0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, nor)
            GLES20.glVertexAttribPointer(aNor, 3, GLES20.GL_FLOAT, false, 0, 0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, idx)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, count, GLES20.GL_UNSIGNED_SHORT, 0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        }

        private fun uploadGround() {
            val slices = 24
            val pos = FloatArray((slices + 2) * 3)
            val nor = FloatArray((slices + 2) * 3)
            val idx = ShortArray(slices * 3)
            pos[0] = 0f; pos[1] = 0f; pos[2] = 0f
            nor[1] = 1f
            for (i in 0..slices) {
                val angle = i.toFloat() / slices * 6.28318f
                val o = (i + 1) * 3
                pos[o] = kotlin.math.cos(angle) * 0.72f
                pos[o + 1] = 0f
                pos[o + 2] = kotlin.math.sin(angle) * 0.55f
                nor[o + 1] = 1f
            }
            for (i in 0 until slices) {
                idx[i * 3] = 0
                idx[i * 3 + 1] = (i + 1).toShort()
                idx[i * 3 + 2] = (i + 2).toShort()
            }
            val ids = IntArray(3)
            GLES20.glGenBuffers(3, ids, 0)
            groundPos = ids[0]
            groundNor = ids[1]
            groundIdx = ids[2]
            putBuffer(groundPos, buffer(pos))
            putBuffer(groundNor, buffer(nor))
            val ib = ByteBuffer.allocateDirect(idx.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
            ib.put(idx).position(0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, groundIdx)
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, idx.size * 2, ib, GLES20.GL_STATIC_DRAW)
            groundCount = idx.size
        }

        private fun uploadCube() {
            val faces = arrayOf(
                floatArrayOf(0f, 0f, 1f),
                floatArrayOf(0f, 0f, -1f),
                floatArrayOf(0f, 1f, 0f),
                floatArrayOf(0f, -1f, 0f),
                floatArrayOf(1f, 0f, 0f),
                floatArrayOf(-1f, 0f, 0f)
            )
            val pos = FloatArray(6 * 4 * 3)
            val nor = FloatArray(6 * 4 * 3)
            val idx = ShortArray(6 * 6)
            var vertex = 0
            for (face in faces.indices) {
                val n = faces[face]
                val tx = if (n[0] != 0f) floatArrayOf(0f, 0f, 1f) else floatArrayOf(1f, 0f, 0f)
                val ty = floatArrayOf(
                    n[1] * tx[2] - n[2] * tx[1],
                    n[2] * tx[0] - n[0] * tx[2],
                    n[0] * tx[1] - n[1] * tx[0]
                )
                val corners = arrayOf(
                    floatArrayOf(-1f, -1f),
                    floatArrayOf(1f, -1f),
                    floatArrayOf(1f, 1f),
                    floatArrayOf(-1f, 1f)
                )
                val base = vertex / 3
                for (corner in corners) {
                    pos[vertex] = n[0] * 0.5f + tx[0] * corner[0] * 0.5f + ty[0] * corner[1] * 0.5f
                    nor[vertex] = n[0]
                    vertex++
                    pos[vertex] = n[1] * 0.5f + tx[1] * corner[0] * 0.5f + ty[1] * corner[1] * 0.5f
                    nor[vertex] = n[1]
                    vertex++
                    pos[vertex] = n[2] * 0.5f + tx[2] * corner[0] * 0.5f + ty[2] * corner[1] * 0.5f
                    nor[vertex] = n[2]
                    vertex++
                }
                val offset = face * 6
                idx[offset] = base.toShort()
                idx[offset + 1] = (base + 1).toShort()
                idx[offset + 2] = (base + 2).toShort()
                idx[offset + 3] = base.toShort()
                idx[offset + 4] = (base + 2).toShort()
                idx[offset + 5] = (base + 3).toShort()
            }
            val ids = IntArray(3)
            GLES20.glGenBuffers(3, ids, 0)
            cubePos = ids[0]
            cubeNor = ids[1]
            cubeIdx = ids[2]
            cubeCount = idx.size
            putBuffer(cubePos, buffer(pos))
            putBuffer(cubeNor, buffer(nor))
            val indices = ByteBuffer.allocateDirect(idx.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
            indices.put(idx).position(0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, cubeIdx)
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, idx.size * 2, indices, GLES20.GL_STATIC_DRAW)
        }

        private fun solidTexture(): Int {
            val ids = IntArray(1)
            GLES20.glGenTextures(1, ids, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
            val pixel = java.nio.ByteBuffer.allocateDirect(4).apply {
                put(byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte()))
                position(0)
            }
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 1, 1, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixel
            )
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
            return ids[0]
        }

        private fun uploadTexture(asset: String): Int {
            val bitmap = context.assets.open(asset).use { BitmapFactory.decodeStream(it) } ?: return 0
            val ids = IntArray(1)
            GLES20.glGenTextures(1, ids, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
            bitmap.recycle()
            return ids[0]
        }

        private fun putBuffer(id: Int, data: FloatBuffer) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, id)
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, data.capacity() * 4, data, GLES20.GL_STATIC_DRAW)
        }

        private fun normalOf(model: FloatArray) {
            Matrix.invertM(inverse, 0, model, 0)
            Matrix.transposeM(transposed, 0, inverse, 0)
            normal[0] = transposed[0]; normal[1] = transposed[1]; normal[2] = transposed[2]
            normal[3] = transposed[4]; normal[4] = transposed[5]; normal[5] = transposed[6]
            normal[6] = transposed[8]; normal[7] = transposed[9]; normal[8] = transposed[10]
        }

        private fun pulse(now: Long, start: Long, duration: Float): Float {
            if (start == 0L) return 0f
            val t = (now - start) / 1000f
            if (t < 0f || t > duration) return 0f
            return sin((t / duration) * Math.PI).toFloat()
        }

        private fun blinkScale(time: Float): Float {
            val cycle = time % 3.6f
            return if (cycle > 3.28f && cycle < 3.46f) 0.12f else 1f
        }
    }

    companion object {
        private val IDENTITY = FloatArray(16).also { Matrix.setIdentityM(it, 0) }

        /** Degrees around Y so the head points at the camera. Pets already facing us stay at 0. */
        private val FACE_YAW = mapOf(
            "capy" to -90f,
            "kipir" to -90f,
            "tekir" to -90f,
            "adim" to -90f,
            "yavru" to -90f,
            "sirtlan" to 45f,
            "dissiz" to -90f,
            "kurt" to -90f,
            "sosis" to 90f,
            "cizgi" to -90f,
            "shiba" to -80f,
            "smurf" to -22f,
            "oiia" to -40f,
            "meowth" to -40f,
            "baba" to -25f,
            "gece" to -30f,
            "pop" to -50f,
            "tuylu" to -90f
        )

        /** Degrees around X after the yaw. Lifts a face the sculpt points at the floor. */
        private val FACE_PITCH = mapOf(
            "kipir" to -22f,
            "tekir" to -22f
        )

        private fun floats(array: org.json.JSONArray): FloatArray {
            return FloatArray(array.length()) { array.getDouble(it).toFloat() }
        }

        private fun buffer(values: FloatArray): FloatBuffer {
            val buffer = ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            buffer.put(values).position(0)
            return buffer
        }

        private fun shorts(array: org.json.JSONArray): ShortBuffer {
            val buffer = ByteBuffer.allocateDirect(array.length() * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
            for (i in 0 until array.length()) buffer.put(array.getInt(i).toShort())
            buffer.position(0)
            return buffer
        }

        private fun link(vert: String, frag: String): Int {
            val vs = compile(GLES20.GL_VERTEX_SHADER, vert)
            val fs = compile(GLES20.GL_FRAGMENT_SHADER, frag)
            val program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vs)
            GLES20.glAttachShader(program, fs)
            GLES20.glLinkProgram(program)
            return program
        }

        private fun compile(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            return shader
        }

        private const val VERT = """
            attribute vec3 aPos;
            attribute vec3 aNor;
            attribute vec3 aCol;
            attribute vec2 aUV;
            uniform mat4 uViewProj;
            uniform mat4 uModel;
            uniform mat3 uNormal;
            varying vec3 vN;
            varying vec3 vW;
            varying vec3 vCol;
            varying vec2 vUV;
            void main() {
                vec4 world = uModel * vec4(aPos, 1.0);
                vW = world.xyz;
                vN = normalize(uNormal * aNor);
                vCol = aCol;
                vUV = aUV;
                gl_Position = uViewProj * world;
            }
        """

        private const val FRAG = """
            precision mediump float;
            varying vec3 vN;
            varying vec3 vW;
            varying vec3 vCol;
            varying vec2 vUV;
            uniform vec3 uColor;
            uniform vec3 uLight;
            uniform vec3 uEye;
            uniform float uSpec;
            uniform sampler2D uTex;
            uniform float uUseTex;
            void main() {
                vec3 N = normalize(vN);
                vec3 L = normalize(uLight);
                vec3 V = normalize(uEye - vW);
                float diff = max(dot(N, L), 0.0);
                vec3 albedo;
                float wrap;
                float rim;
                if (uUseTex > 0.5) {
                    // Bitmap row 0 is GL v = 0, and this coat was authored that way.
                    albedo = texture2D(uTex, vUV).rgb;
                    wrap = diff * 0.32 + 0.74;
                    rim = pow(1.0 - max(dot(N, V), 0.0), 2.2) * 0.04;
                } else {
                    albedo = uColor * vCol;
                    wrap = diff * 0.28 + 0.78;
                    rim = pow(1.0 - max(dot(N, V), 0.0), 2.0) * 0.42;
                }
                float hemi = N.y * 0.10 + 0.92;
                float spec = pow(max(dot(N, normalize(L + V)), 0.0), 22.0) * uSpec;
                vec3 color = albedo * wrap * hemi + vec3(1.0, 0.97, 0.92) * (spec + rim);
                gl_FragColor = vec4(color, 1.0);
            }
        """
    }
}
