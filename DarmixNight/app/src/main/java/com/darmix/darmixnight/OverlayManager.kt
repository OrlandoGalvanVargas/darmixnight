package com.darmix.darmixnight

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object OverlayManager {

    private const val TAG = "SleepGuardOverlay"

    /** Cuánto tiempo hay que mantener presionado el botón de emergencia. */
    private const val HOLD_DURATION_MS = 20_000L
    private const val HOLD_TICK_MS = 200L

    /** Frase exacta (sin distinguir mayúsculas ni espacios extra) para confirmar. */
    private const val REQUIRED_PHRASE = "necesito desbloquear"

    // Paleta propia de SleepGuard. El overlay se mantiene siempre en modo
    // noche, sin importar el tema elegido en la app: su trabajo es que la
    // persona vea lo mínimo de luz y vuelva a dormir, así que un fondo claro
    // aquí iría en contra del propósito de la app.
    private const val SCRIM = "#F3100E21"
    private const val CARD_BG = "#1E1D36"
    private const val CARD_BORDER = "#332F5A"
    private const val RAISED = "#2A2848"
    private const val TEXT = "#EDEBFF"
    private const val TEXT_DIM = "#9C99C4"
    private const val ACCENT = "#9B8CFF"
    private const val ACCENT_PRESSED = "#7C6AE8"
    private const val DANGER = "#F2787E"

    private var overlayView: View? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val holdHandler = Handler(Looper.getMainLooper())
    private var holdElapsedMs = 0L
    private var isHolding = false
    private var holdFillDrawable: ClipDrawable? = null

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    @SuppressLint("ClickableViewAccessibility")
    fun showOverlay(context: Context) {
        if (overlayView != null) return
        if (!canDrawOverlays(context)) {
            Log.w(TAG, "No hay permiso SYSTEM_ALERT_WINDOW, no se puede mostrar overlay")
            return
        }

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val appContext = context.applicationContext

        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor(SCRIM))
            isFocusableInTouchMode = true
            setOnKeyListener { _, keyCode, _ ->
                // Absorbe el botón atrás para que no filtre a lo que hay detrás.
                keyCode == KeyEvent.KEYCODE_BACK
            }
        }

        // Tarjeta central: todo el contenido vive dentro para que se lea
        // como un panel flotante sobre el fondo, no como texto suelto.
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = roundedDrawable(context, CARD_BG, 28, CARD_BORDER, 1)
            setPadding(context.dp(28), context.dp(32), context.dp(28), context.dp(28))
            elevation = context.dp(8).toFloat()
        }

        val moonIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_overlay_moon)
            setColorFilter(Color.parseColor(ACCENT))
            layoutParams = LinearLayout.LayoutParams(context.dp(48), context.dp(48))
        }
        card.addView(moonIcon)
        card.addView(spacer(context, 18))

        val title = TextView(context).apply {
            text = "Es hora de dormir"
            textSize = 24f
            setTextColor(Color.parseColor(TEXT))
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        card.addView(title)

        val nowMinutes = ScheduleUtils.currentMinutesOfDay()
        val emergencyAllowed = ScheduleUtils.isWithinEmergencyUnlockWindow(nowMinutes)

        card.addView(spacer(context, 8))
        val subtitle = TextView(context).apply {
            text = if (emergencyAllowed) {
                "Esta pantalla se irá sola cuando termine tu horario."
            } else {
                "El desbloqueo de emergencia no está disponible a esta hora."
            }
            textSize = 14f
            setTextColor(Color.parseColor(TEXT_DIM))
            gravity = Gravity.CENTER
        }
        card.addView(subtitle)

        if (emergencyAllowed) {
            card.addView(divider(context, topDp = 24, bottomDp = 20))

            val counterText = TextView(context).apply {
                text = "Cargando…"
                textSize = 12f
                setTextColor(Color.parseColor(TEXT_DIM))
                gravity = Gravity.CENTER
            }
            card.addView(counterText)
            scope.launch {
                val count = EmergencyUnlockManager.weeklyUsageCountOnce(appContext)
                val word = if (count == 1) "vez" else "veces"
                counterText.text = "Usado $count $word esta semana"
            }

            card.addView(spacer(context, 20))

            // --- Pantalla 1: mantener presionado ---
            val holdButton = Button(context).apply {
                text = "Mantén presionado para emergencia"
                textSize = 14f
                isAllCaps = false
                setTextColor(Color.parseColor(TEXT))
                stateListAnimator = null
                background = buildHoldBackground(context).also { holdFillDrawable = it.second }.first
                setPadding(context.dp(20), context.dp(18), context.dp(20), context.dp(18))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            card.addView(holdButton)

            // --- Pantalla 2: escribir frase (oculta al inicio) ---
            val phraseLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                visibility = View.GONE
            }
            val phraseLabel = TextView(context).apply {
                text = "Escribe exactamente: \u201C$REQUIRED_PHRASE\u201D"
                setTextColor(Color.parseColor(TEXT))
                textSize = 14f
                gravity = Gravity.CENTER
            }
            val phraseInput = EditText(context).apply {
                setTextColor(Color.parseColor(TEXT))
                setHintTextColor(Color.parseColor(TEXT_DIM))
                hint = "Escribe aquí"
                background = roundedDrawable(context, RAISED, 14, CARD_BORDER, 1)
                setPadding(context.dp(16), context.dp(14), context.dp(16), context.dp(14))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = context.dp(14) }
            }
            val phraseError = TextView(context).apply {
                text = "No coincide, inténtalo de nuevo"
                setTextColor(Color.parseColor(DANGER))
                textSize = 12f
                visibility = View.GONE
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = context.dp(8) }
            }
            val confirmButton = Button(context).apply {
                text = "Confirmar desbloqueo"
                isAllCaps = false
                setTextColor(Color.parseColor("#1A1730"))
                stateListAnimator = null
                background = ripplePill(context, ACCENT, ACCENT_PRESSED, radiusDp = 28)
                setPadding(context.dp(20), context.dp(16), context.dp(20), context.dp(16))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = context.dp(18) }
            }
            val cancelButton = Button(context).apply {
                text = "Cancelar"
                isAllCaps = false
                setTextColor(Color.parseColor(TEXT_DIM))
                stateListAnimator = null
                background = ripplePill(context, "#00000000", "#22FFFFFF", radiusDp = 28)
                setPadding(context.dp(20), context.dp(12), context.dp(20), context.dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = context.dp(6) }
            }

            phraseLayout.addView(phraseLabel)
            phraseLayout.addView(phraseInput)
            phraseLayout.addView(phraseError)
            phraseLayout.addView(confirmButton)
            phraseLayout.addView(cancelButton)
            card.addView(phraseLayout)

            holdElapsedMs = 0L
            isHolding = false

            val holdTick = object : Runnable {
                override fun run() {
                    if (!isHolding) return
                    holdElapsedMs += HOLD_TICK_MS
                    val remaining = ((HOLD_DURATION_MS - holdElapsedMs) / 1000L).coerceAtLeast(0)
                    holdButton.text = "Mantén presionado… ${remaining}s"
                    holdFillDrawable?.level = ((holdElapsedMs * 10_000L) / HOLD_DURATION_MS)
                        .toInt().coerceIn(0, 10_000)
                    if (holdElapsedMs >= HOLD_DURATION_MS) {
                        isHolding = false
                        holdButton.visibility = View.GONE
                        phraseLayout.visibility = View.VISIBLE
                    } else {
                        holdHandler.postDelayed(this, HOLD_TICK_MS)
                    }
                }
            }

            holdButton.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.isPressed = true
                        isHolding = true
                        holdElapsedMs = 0L
                        holdHandler.post(holdTick)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.isPressed = false
                        isHolding = false
                        holdHandler.removeCallbacks(holdTick)
                        holdFillDrawable?.level = 0
                        holdButton.text = "Mantén presionado para emergencia"
                        true
                    }
                    else -> false
                }
            }

            confirmButton.setOnClickListener {
                val typed = phraseInput.text.toString().trim().lowercase()
                if (typed == REQUIRED_PHRASE) {
                    scope.launch {
                        val duration = BlockPrefs.getEmergencyDurationMinutesBlocking(appContext)
                        EmergencyUnlockManager.activateBypass(duration)
                        EmergencyUnlockManager.recordUsage(appContext)
                        hideOverlay(appContext)
                    }
                } else {
                    phraseError.text = "No coincide, inténtalo de nuevo"
                    phraseError.visibility = View.VISIBLE
                }
            }

            cancelButton.setOnClickListener {
                phraseInput.setText("")
                phraseError.visibility = View.GONE
                phraseLayout.visibility = View.GONE
                holdButton.visibility = View.VISIBLE
                holdFillDrawable?.level = 0
                holdButton.text = "Mantén presionado para emergencia"
            }
        }

        val scrollContainer = ScrollView(context).apply {
            isFillViewport = false
            addView(
                card,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        val scrollParams = FrameLayout.LayoutParams(
            (context.resources.displayMetrics.widthPixels * 0.88f).toInt()
                .coerceAtMost(context.dp(360)),
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER }

        root.addView(scrollContainer, scrollParams)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // OJO: ya NO incluye FLAG_NOT_FOCUSABLE. Esto hace que el overlay
        // reciba toques de verdad (bloqueo real), en vez de dejarlos pasar
        // hacia lo que esté detrás.
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        try {
            windowManager.addView(root, params)
            overlayView = root
            Log.d(TAG, "Overlay mostrado (emergencia disponible: $emergencyAllowed)")
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando overlay: ${e.message}")
        }
    }

    fun hideOverlay(context: Context) {
        val view = overlayView ?: return
        holdHandler.removeCallbacksAndMessages(null)
        isHolding = false
        holdFillDrawable = null
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            windowManager.removeView(view)
        } catch (e: Exception) {
            Log.e(TAG, "Error ocultando overlay: ${e.message}")
        } finally {
            overlayView = null
        }
    }

    /* ------------------------- Ayudantes visuales ------------------------- */

    private fun Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun spacer(context: Context, dp: Int): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, context.dp(dp))
        }
    }

    private fun divider(context: Context, topDp: Int, bottomDp: Int): View {
        return View(context).apply {
            setBackgroundColor(Color.parseColor(CARD_BORDER))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, context.dp(1)
            ).apply { topMargin = context.dp(topDp); bottomMargin = context.dp(bottomDp) }
        }
    }

    private fun roundedDrawable(
        context: Context,
        colorHex: String,
        radiusDp: Int,
        strokeHex: String? = null,
        strokeWidthDp: Int = 0
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = context.dp(radiusDp).toFloat()
        setColor(Color.parseColor(colorHex))
        if (strokeHex != null) setStroke(context.dp(strokeWidthDp), Color.parseColor(strokeHex))
    }

    private fun ripplePill(
        context: Context,
        bgHex: String,
        rippleHex: String,
        radiusDp: Int
    ): Drawable {
        val mask = roundedDrawable(context, bgHex, radiusDp)
        return RippleDrawable(ColorStateList.valueOf(Color.parseColor(rippleHex)), mask, mask)
    }

    /**
     * Fondo del botón de "mantener presionado": una pista tenue de abajo y,
     * encima, un relleno que se recorta con [ClipDrawable] según el nivel
     * (0–10000). Se actualiza en cada tick del handler ya existente, así el
     * progreso se ve, no solo se lee en el contador de segundos.
     */
    private fun buildHoldBackground(context: Context): Pair<LayerDrawable, ClipDrawable> {
        val track = roundedDrawable(context, RAISED, 24, CARD_BORDER, 1)
        val fillShape = roundedDrawable(context, ACCENT, 24)
        val clip = ClipDrawable(fillShape, Gravity.START, ClipDrawable.HORIZONTAL).apply { level = 0 }
        return LayerDrawable(arrayOf(track, clip)) to clip
    }
}