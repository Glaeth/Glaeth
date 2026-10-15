package com.clipsmile.app.ui

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
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
import kotlin.math.sin

/**
 * A lit, rigged toy dog. Each companion is its own mesh. The body breathes,
 * the head looks around, the ears and tail move, and a tap makes it hop.
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            val width = width.coerceAtLeast(1)
            engine.lookTarget = (event.x / width) * 2f - 1f
            engine.lookAt = SystemClock.uptimeMillis()
            performClick()
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
        val idx: ShortBuffer,
        val indexCount: Int
    )

    private class Engine(private val context: Context) : Renderer {
        @Volatile var wanted: String = "dogs/pug.json"
        @Volatile var happyAt: Long = 0L
        @Volatile var shakeAt: Long = 0L
        @Volatile var lookTarget: Float = 0f
        @Volatile var lookAt: Long = 0L

        private var loadedName: String? = null
        private var parts: List<Part> = emptyList()
        private var program = 0
        private var aPos = 0
        private var aNor = 0
        private var uViewProj = 0
        private var uModel = 0
        private var uNormal = 0
        private var uColor = 0
        private var uSpec = 0
        private var uLight = 0
        private var uEye = 0
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

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(0.965f, 0.886f, 0.722f, 1f)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glDisable(GLES20.GL_CULL_FACE)
            program = link(VERT, FRAG)
            aPos = GLES20.glGetAttribLocation(program, "aPos")
            aNor = GLES20.glGetAttribLocation(program, "aNor")
            uViewProj = GLES20.glGetUniformLocation(program, "uViewProj")
            uModel = GLES20.glGetUniformLocation(program, "uModel")
            uNormal = GLES20.glGetUniformLocation(program, "uNormal")
            uColor = GLES20.glGetUniformLocation(program, "uColor")
            uSpec = GLES20.glGetUniformLocation(program, "uSpec")
            uLight = GLES20.glGetUniformLocation(program, "uLight")
            uEye = GLES20.glGetUniformLocation(program, "uEye")
            loadedName = null
            uploadGround()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            val aspect = width.toFloat() / height.coerceAtLeast(1)
            Matrix.perspectiveM(proj, 0, 34f, aspect, 0.1f, 40f)
            Matrix.setLookAtM(view, 0, 2.15f, 0.78f, 4.15f, 0f, 0.42f, 0.05f, 0f, 1f, 0f)
            Matrix.multiplyMM(viewProj, 0, proj, 0, view, 0)
        }

        override fun onDrawFrame(gl: GL10?) {
            ensureModel()
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            if (program == 0) return
            val now = SystemClock.uptimeMillis()
            val time = now / 1000f
            val happy = pulse(now, happyAt, 0.9f)
            val shake = pulse(now, shakeAt, 0.7f)
            val lookAge = (now - lookAt) / 1000f
            val look = if (lookAge in 0f..1.3f) lookTarget * (1f - lookAge / 1.3f) else 0f
            val blink = blinkScale(time)

            GLES20.glUseProgram(program)
            GLES20.glUniformMatrix4fv(uViewProj, 1, false, viewProj, 0)
            GLES20.glUniform3f(uLight, 0.35f, 0.85f, 0.55f)
            GLES20.glUniform3f(uEye, 2.15f, 0.78f, 4.15f)
            drawGround()

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
                    val breath = sin(time * 2.5f)
                    sy *= 1f + breath * 0.045f - happy * 0.12f
                    sx *= 1f - breath * 0.025f + happy * 0.08f
                    sz *= 1f + happy * 0.04f
                    ty += happy * 0.42f
                    rz += sin(time * 28f) * shake * 22f
                }
                name == "head" -> {
                    ry += sin(time * 0.85f) * 14f + look * 22f
                    rx += sin(time * 0.55f) * 7f - happy * 16f
                    rz += sin(time * 1.3f) * 4f
                }
                name.startsWith("ear") -> {
                    val phase = if (name.contains("L")) 0f else 1.4f
                    rz += sin(time * 3.1f + phase) * (8f + happy * 10f)
                }
                name == "tail" -> ry += sin(time * 7.5f) * (22f + happy * 28f)
                name.startsWith("leg") -> {
                    val front = name.contains("F")
                    val left = name.endsWith("L")
                    val phase = if (front == left) 0f else Math.PI.toFloat()
                    rx += sin(time * 2.6f + phase) * (7f + happy * 4f)
                }
                name.startsWith("eye") || name.startsWith("pupil") || name.startsWith("spark") -> sy *= blink
                name == "mouth" || name == "jaw" || name == "tongue" -> sy *= 1f + happy * 0.8f
            }
            Matrix.setIdentityM(local, 0)
            Matrix.translateM(local, 0, tx, ty, tz)
            Matrix.rotateM(local, 0, rz, 0f, 0f, 1f)
            Matrix.rotateM(local, 0, ry, 0f, 1f, 0f)
            Matrix.rotateM(local, 0, rx, 1f, 0f, 0f)
            Matrix.scaleM(local, 0, sx, sy, sz)
        }

        private fun ensureModel() {
            val name = wanted
            if (name == loadedName) return
            parts = load(name)
            loadedName = name
        }

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
            part.idx.position(0)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, part.indexCount, GLES20.GL_UNSIGNED_SHORT, part.idx)
        }

        private fun drawGround() {
            Matrix.setIdentityM(local, 0)
            Matrix.translateM(local, 0, 0f, 0.02f, 0f)
            normalOf(local)
            GLES20.glUniformMatrix4fv(uModel, 1, false, local, 0)
            GLES20.glUniformMatrix3fv(uNormal, 1, false, normal, 0)
            GLES20.glUniform3f(uColor, 0.78f, 0.62f, 0.38f)
            GLES20.glUniform1f(uSpec, 0.05f)
            GLES20.glEnableVertexAttribArray(aPos)
            GLES20.glEnableVertexAttribArray(aNor)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, groundPos)
            GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 0, 0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, groundNor)
            GLES20.glVertexAttribPointer(aNor, 3, GLES20.GL_FLOAT, false, 0, 0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, groundIdx)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, groundCount, GLES20.GL_UNSIGNED_SHORT, 0)
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
                pos[o] = kotlin.math.cos(angle) * 0.95f
                pos[o + 1] = 0f
                pos[o + 2] = kotlin.math.sin(angle) * 0.72f
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
            uniform mat4 uViewProj;
            uniform mat4 uModel;
            uniform mat3 uNormal;
            varying vec3 vN;
            varying vec3 vW;
            void main() {
                vec4 world = uModel * vec4(aPos, 1.0);
                vW = world.xyz;
                vN = normalize(uNormal * aNor);
                gl_Position = uViewProj * world;
            }
        """

        private const val FRAG = """
            precision mediump float;
            varying vec3 vN;
            varying vec3 vW;
            uniform vec3 uColor;
            uniform vec3 uLight;
            uniform vec3 uEye;
            uniform float uSpec;
            void main() {
                vec3 N = normalize(vN);
                vec3 L = normalize(uLight);
                vec3 V = normalize(uEye - vW);
                float diff = max(dot(N, L), 0.0);
                float wrap = diff * 0.62 + 0.38;
                float spec = pow(max(dot(N, normalize(L + V)), 0.0), 36.0) * uSpec;
                float rim = pow(1.0 - max(dot(N, V), 0.0), 2.2) * 0.16;
                vec3 color = uColor * wrap + vec3(spec + rim);
                gl_FragColor = vec4(color, 1.0);
            }
        """
    }
}
