package com.dashdazz.light

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import rikka.shizuku.Shizuku
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

class MainActivity : ComponentActivity() {

    private val permListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
        shizukuPermissionState.value = LightsBridge.hasShizukuPermission()
    }

    private val shizukuPermissionState = mutableStateOf(false)
    private val conflictState = mutableStateOf(false)
    private val darkModeState = mutableIntStateOf(0)
    private val rootState = mutableStateOf(false)
    private val rootCheckedState = mutableStateOf(false)
    private val setupDoneState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        darkModeState.intValue = Prefs.getDarkMode(this)
        setupDoneState.value = Prefs.isSetupDone(this)

        DashDazzService.restoreStateFromPrefs(this)

        applyNightMode(darkModeState.intValue)

        shizukuPermissionState.value = LightsBridge.hasShizukuPermission()
        Shizuku.addRequestPermissionResultListener(permListener)
        checkNotificationPermission()

        checkRootStatusAsync()

        setContent {
            val systemDark = isSystemInDarkTheme()
            val isDark = when (darkModeState.intValue) {
                1 -> false
                2 -> true
                else -> systemDark
            }
            val colors = if (isDark) darkColorScheme() else lightColorScheme()

            MiuixTheme(colors = colors) {
                if (!setupDoneState.value) {
                    SetupScreen(
                        isRootAvailable = rootState.value,
                        rootChecked = rootCheckedState.value,
                        hasShizukuPermission = shizukuPermissionState.value,
                        onRecheckRoot = { checkRootStatusAsync() },
                        onRequestShizukuPermission = {
                            if (LightsBridge.isShizukuAvailable()) {
                                Shizuku.requestPermission(1001)
                            } else {
                                Toast.makeText(this, R.string.shizuku_not_running, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onContinue = {
                            Prefs.setSetupDone(this@MainActivity, true)
                            setupDoneState.value = true
                        }
                    )
                } else {
                    DashDazzScreen(
                        hasShizukuPermission = shizukuPermissionState.value,
                        isRootAvailable = rootState.value,
                        rootChecked = rootCheckedState.value,
                        hasConflict = conflictState.value,
                        selectedDarkMode = darkModeState.intValue,
                        onDarkModeChange = { modeVal ->
                            darkModeState.intValue = modeVal
                            Prefs.setDarkMode(this@MainActivity, modeVal)
                            applyNightMode(modeVal)
                        },
                        onRequestShizukuPermission = {
                            if (LightsBridge.isShizukuAvailable()) {
                                Shizuku.requestPermission(1001)
                            } else {
                                Toast.makeText(this, R.string.shizuku_not_running, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRecheckRoot = { checkRootStatusAsync() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        shizukuPermissionState.value = LightsBridge.hasShizukuPermission()
        conflictState.value = ConflictDetector.isStockMusicLightActive(this)
        checkRootStatusAsync()
    }

    private fun checkRootStatusAsync() {
        Thread {
            val root = LightsBridge.checkRootFresh()
            runOnUiThread {
                rootState.value = root
                rootCheckedState.value = true
            }
        }.start()
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permListener)
        super.onDestroy()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1002
                )
            }
        }
    }

    companion object {
        fun applyNightMode(mode: Int) {
            val target = when (mode) {
                1 -> AppCompatDelegate.MODE_NIGHT_NO
                2 -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(target)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashDazzScreen(
    hasShizukuPermission: Boolean,
    isRootAvailable: Boolean,
    rootChecked: Boolean,
    hasConflict: Boolean,
    selectedDarkMode: Int,
    onDarkModeChange: (Int) -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onRecheckRoot: () -> Unit = {}
) {
    val context = LocalContext.current
    val serviceState by DashDazzService.stateFlow.collectAsState()

    LaunchedEffect(isRootAvailable, rootChecked) {
        if (!rootChecked || isRootAvailable) return@LaunchedEffect
        if (!serviceState.isRunning) return@LaunchedEffect
        if (serviceState.mode == DashDazzService.MODE_DUAL) {
            DashDazzService.updateSettings(context, mode = DashDazzService.MODE_RAINBOW)
        }
    }

    LaunchedEffect(serviceState.audioReactive) {
        if (!serviceState.audioReactive) return@LaunchedEffect
        if (serviceState.mode == DashDazzService.MODE_BREATHE) {
            DashDazzService.updateSettings(context, mode = DashDazzService.MODE_SOLID)
        }
        if (serviceState.mode == DashDazzService.MODE_DUAL &&
            serviceState.dualStyle !in DashDazzService.AUDIO_DUAL_STYLES
        ) {
            DashDazzService.updateSettings(context, dualStyle = DashDazzService.DUAL_BEAT_RAINBOW)
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (!granted) {
            Toast.makeText(
                context,
                context.getString(R.string.audio_reactive_permission_denied),
                Toast.LENGTH_LONG
            ).show()
        } else {

            DashDazzService.relaunchAnimation(context)
        }
    }

    fun selectMode(modeId: Int) {
        DashDazzService.updateSettings(context, mode = modeId)
    }

    var currentTab by remember { mutableIntStateOf(0) }
    var speedMs by remember { mutableLongStateOf(serviceState.speedMs) }
    var brightness by remember { mutableFloatStateOf(serviceState.brightness) }
    var duoHueOffset by remember { mutableIntStateOf(serviceState.duoHueOffset) }
    var notifDuration by remember { mutableIntStateOf(serviceState.notifActivateDuration) }
    var showAudioTuningDialog by remember { mutableStateOf(false) }

    LaunchedEffect(serviceState.duoHueOffset) {
        duoHueOffset = serviceState.duoHueOffset
    }

    LaunchedEffect(serviceState.speedMs) { speedMs = serviceState.speedMs }
    LaunchedEffect(serviceState.brightness) { brightness = serviceState.brightness }
    LaunchedEffect(serviceState.notifActivateDuration) {
        notifDuration = serviceState.notifActivateDuration
    }

    val initialHsv = remember {
        FloatArray(3).also { AndroidColor.colorToHSV(serviceState.color, it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var customPresets by remember { mutableStateOf(Prefs.getCustomPresets(context)) }
    var activeSplitSpot by remember { mutableIntStateOf(0) }
    var activeSpinSpot by remember { mutableIntStateOf(0) }

    fun getSelectedColor(): Int {
        val argb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))
        return if (argb == -1) 0xFFFEFEFE.toInt() else argb
    }

    Scaffold(
        topBar = {
            TopAppBar(title = stringResource(R.string.app_name))
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MiuixTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(0 to R.string.tab_lighting, 1 to R.string.tab_settings).forEach { (tabIndex, titleRes) ->
                    val isSelected = (currentTab == tabIndex)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MiuixTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { currentTab = tabIndex }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(titleRes),
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MiuixTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (currentTab == 0) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    if (hasConflict) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.title_conflict_warning),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9800)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.conflict_warning_desc),
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                SuperArrow(
                                    title = stringResource(R.string.btn_open_settings),
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                )
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            SuperSwitch(
                                title = stringResource(R.string.qs_tile_label),
                                summary = if (serviceState.isRunning) {
                                    if (serviceState.isScreenPaused) {
                                        stringResource(R.string.notification_paused)
                                    } else {
                                        stringResource(R.string.status_service_running)
                                    }
                                } else {
                                    stringResource(R.string.status_service_stopped)
                                },
                                checked = serviceState.isRunning,
                                onCheckedChange = { checked ->
                                    if (!hasShizukuPermission && !isRootAvailable) {
                                        onRequestShizukuPermission()
                                        return@SuperSwitch
                                    }
                                    if (checked) {

                                        if (serviceState.audioReactive) {
                                            val hasAudioPerm = ContextCompat.checkSelfPermission(
                                                context, Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (!hasAudioPerm) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.audio_reactive_permission_rationale),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                return@SuperSwitch
                                            }
                                        }

                                        DashDazzService.start(
                                            context = context,
                                            mode = serviceState.mode,
                                            color = getSelectedColor(),
                                            speedMs = speedMs,
                                            brightness = brightness,
                                            screenMode = serviceState.screenMode,
                                            blinkOnNotification = serviceState.blinkOnNotification,
                                            palette = serviceState.palette,
                                            dualStyle = serviceState.dualStyle,
                                            splitColorA = serviceState.splitColorA,
                                            splitColorB = serviceState.splitColorB,
                                            spinColorA = serviceState.spinColorA,
                                            spinColorB = serviceState.spinColorB,
                                            duoHueOffset = duoHueOffset
                                        )
                                    } else {
                                        DashDazzService.stop(context)
                                    }
                                }
                            )
                            if (!hasShizukuPermission && !isRootAvailable) {
                                SuperArrow(
                                    title = stringResource(R.string.btn_grant_permission),
                                    summary = stringResource(R.string.shizuku_permission_needed),
                                    onClick = onRequestShizukuPermission
                                )
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.title_modes),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val modeItems = buildList {
                                    add(DashDazzService.MODE_RAINBOW to R.string.mode_rainbow)

                                    if (!serviceState.audioReactive) {
                                        add(DashDazzService.MODE_BREATHE to R.string.mode_breathe)
                                    }
                                    add(DashDazzService.MODE_SOLID to R.string.mode_solid)
                                    if (isRootAvailable) {
                                        add(DashDazzService.MODE_DUAL to R.string.mode_dual)
                                    }
                                }
                                modeItems.forEach { (modeId, titleRes) ->
                                    val isSelected = (serviceState.mode == modeId)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) MiuixTheme.colorScheme.primary
                                                else MiuixTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable {

                                                selectMode(modeId)
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(titleRes),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MiuixTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (serviceState.mode == DashDazzService.MODE_RAINBOW) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.title_palettes),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Palettes.ALL.chunked(3).forEach { rowPalettes ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowPalettes.forEach { palette ->
                                                val isSelected = (serviceState.palette == palette.id)
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(
                                                            if (isSelected) MiuixTheme.colorScheme.primary
                                                            else MiuixTheme.colorScheme.surfaceVariant
                                                        )
                                                        .clickable {
                                                            DashDazzService.updateSettings(context, palette = palette.id)
                                                        }
                                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = stringResource(palette.nameRes),
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) Color.White else MiuixTheme.colorScheme.onSurface,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isRootAvailable && serviceState.mode == DashDazzService.MODE_DUAL) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.title_dual_effects),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                val audioOn = serviceState.audioReactive
                                val dualEffects = buildList {
                                    if (audioOn) {
                                        add(Triple(DashDazzService.DUAL_BEAT_RAINBOW, R.string.dual_beat_rainbow, listOf(Color(0xFFFF1744), Color(0xFFFF9100), Color(0xFF2979FF))))
                                        add(Triple(DashDazzService.DUAL_SPLIT_BEAT, R.string.dual_split_beat, listOf(Color(serviceState.splitColorA), Color(serviceState.splitColorB))))
                                    } else {
                                        add(Triple(DashDazzService.DUAL_HARDWARE_SPLIT, R.string.dual_hardware_split, listOf(Color(serviceState.splitColorA), Color(serviceState.splitColorB))))
                                        add(Triple(DashDazzService.DUAL_SPIN, R.string.dual_spin, listOf(Color(serviceState.spinColorA), Color(serviceState.spinColorB))))
                                        add(Triple(DashDazzService.DUAL_POLICE, R.string.dual_police, listOf(Color(0xFFFF1744), Color(0xFF2979FF))))
                                        add(Triple(DashDazzService.DUAL_ALTERNATE, R.string.dual_alternate, listOf(Color(serviceState.spinColorA), Color(serviceState.spinColorB))))
                                        add(Triple(DashDazzService.DUAL_RAINBOW_DUO, R.string.dual_rainbow_duo, listOf(Color(0xFFFF1744), Color(0xFF00E676), Color(0xFF2979FF))))
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    dualEffects.chunked(2).forEach { rowEffects ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowEffects.forEach { (effectId, titleRes, gradientColors) ->
                                                val isSelected = (serviceState.dualStyle == effectId)
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(
                                                            if (isSelected) MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                            else MiuixTheme.colorScheme.surfaceVariant
                                                        )
                                                        .border(
                                                            width = if (isSelected) 1.5.dp else 0.dp,
                                                            color = if (isSelected) MiuixTheme.colorScheme.primary else Color.Transparent,
                                                            shape = RoundedCornerShape(12.dp)
                                                        )
                                                        .clickable {
                                                            DashDazzService.updateSettings(context, dualStyle = effectId)
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 12.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    androidx.compose.ui.graphics.Brush.linearGradient(gradientColors)
                                                                )
                                                                .border(1.dp, Color.Gray.copy(alpha = 0.4f), CircleShape)
                                                        )
                                                        Text(
                                                            text = stringResource(titleRes),
                                                            fontSize = 12.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (serviceState.dualStyle == DashDazzService.DUAL_HARDWARE_SPLIT) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(R.string.title_split_colors),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            0 to (R.string.spot_left to serviceState.splitColorA),
                                            1 to (R.string.spot_right to serviceState.splitColorB)
                                        ).forEach { (spotIndex, data) ->
                                            val (labelRes, spotColor) = data
                                            val isSpotSelected = (activeSplitSpot == spotIndex)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (isSpotSelected) MiuixTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                        else MiuixTheme.colorScheme.surfaceVariant
                                                    )
                                                    .border(
                                                        width = if (isSpotSelected) 1.5.dp else 0.dp,
                                                        color = if (isSpotSelected) MiuixTheme.colorScheme.primary else Color.Transparent,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable {
                                                        activeSplitSpot = spotIndex
                                                        val hsv = FloatArray(3)
                                                        AndroidColor.colorToHSV(spotColor, hsv)
                                                        hue = hsv[0]
                                                        saturation = hsv[1]
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(spotColor))
                                                            .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                                    )
                                                    Text(
                                                        text = stringResource(labelRes),
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSpotSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSpotSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    val quickColors = listOf(
                                        Color(0xFFFF0000), Color(0xFFFF9800), Color(0xFFFFEB3B), Color(0xFF4CAF50),
                                        Color(0xFF00BCD4), Color(0xFF2196F3), Color(0xFF9C27B0), Color(0xFFFFFFFF)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        quickColors.forEach { qColor ->
                                            val currentColor = if (activeSplitSpot == 0) serviceState.splitColorA else serviceState.splitColorB
                                            val isSelected = (currentColor == qColor.toArgb())
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(qColor)
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) MiuixTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        val targetColor = qColor.toArgb()
                                                        val hsv = FloatArray(3)
                                                        AndroidColor.colorToHSV(targetColor, hsv)
                                                        hue = hsv[0]
                                                        saturation = hsv[1]
                                                        if (activeSplitSpot == 0) {
                                                            DashDazzService.updateSettings(context, splitColorA = targetColor)
                                                        } else {
                                                            DashDazzService.updateSettings(context, splitColorB = targetColor)
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(if (qColor == Color.White) Color.Black else Color.White)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (serviceState.dualStyle == DashDazzService.DUAL_SPIN ||
                                    serviceState.dualStyle == DashDazzService.DUAL_ALTERNATE) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(R.string.title_spin_colors),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            0 to (R.string.spin_step_1 to serviceState.spinColorA),
                                            1 to (R.string.spin_step_2 to serviceState.spinColorB)
                                        ).forEach { (spotIndex, data) ->
                                            val (labelRes, spotColor) = data
                                            val isSpotSelected = (activeSpinSpot == spotIndex)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (isSpotSelected) MiuixTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                        else MiuixTheme.colorScheme.surfaceVariant
                                                    )
                                                    .border(
                                                        width = if (isSpotSelected) 1.5.dp else 0.dp,
                                                        color = if (isSpotSelected) MiuixTheme.colorScheme.primary else Color.Transparent,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable {
                                                        activeSpinSpot = spotIndex
                                                        val hsv = FloatArray(3)
                                                        AndroidColor.colorToHSV(spotColor, hsv)
                                                        hue = hsv[0]
                                                        saturation = hsv[1]
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(spotColor))
                                                            .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                                    )
                                                    Text(
                                                        text = stringResource(labelRes),
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSpotSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSpotSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    val quickColors = listOf(
                                        Color(0xFFFF0000), Color(0xFFFF9800), Color(0xFFFFEB3B), Color(0xFF4CAF50),
                                        Color(0xFF00BCD4), Color(0xFF2196F3), Color(0xFF9C27B0), Color(0xFFFFFFFF)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        quickColors.forEach { qColor ->
                                            val currentColor = if (activeSpinSpot == 0) serviceState.spinColorA else serviceState.spinColorB
                                            val isSelected = (currentColor == qColor.toArgb())
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(qColor)
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) MiuixTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        val targetColor = qColor.toArgb()
                                                        val hsv = FloatArray(3)
                                                        AndroidColor.colorToHSV(targetColor, hsv)
                                                        hue = hsv[0]
                                                        saturation = hsv[1]
                                                        if (activeSpinSpot == 0) {
                                                            DashDazzService.updateSettings(context, spinColorA = targetColor)
                                                        } else {
                                                            DashDazzService.updateSettings(context, spinColorB = targetColor)
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(if (qColor == Color.White) Color.Black else Color.White)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (serviceState.dualStyle == DashDazzService.DUAL_RAINBOW_DUO) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Hue Offset: ${duoHueOffset}°",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Slider(
                                            progress = duoHueOffset.toFloat(),
                                            onProgressChange = { value ->
                                                duoHueOffset = value.toInt()
                                                DashDazzService.updateSettings(context, duoHueOffset = duoHueOffset)
                                            },
                                            minValue = 0f,
                                            maxValue = 360f,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(AndroidColor.HSVToColor(floatArrayOf(0f, 1f, 1f))))
                                                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Color(AndroidColor.HSVToColor(floatArrayOf((duoHueOffset.toFloat() % 360f), 1f, 1f)))
                                                    )
                                                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val anglePresets = listOf(
                                            60 to "60°",
                                            90 to "90°",
                                            120 to "120°",
                                            180 to "180°"
                                        )
                                        anglePresets.forEach { (angle, label) ->
                                            val isPresetSelected = (duoHueOffset == angle)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isPresetSelected) MiuixTheme.colorScheme.primary
                                                        else MiuixTheme.colorScheme.surfaceVariant
                                                    )
                                                    .clickable {
                                                        duoHueOffset = angle
                                                        DashDazzService.updateSettings(context, duoHueOffset = angle)
                                                    }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isPresetSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isPresetSelected) Color.White else MiuixTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }

                                if (serviceState.dualStyle == DashDazzService.DUAL_BEAT_RAINBOW) {
                                    Text(
                                        text = stringResource(R.string.title_beat_rainbow),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = stringResource(R.string.label_hue, duoHueOffset),
                                        fontSize = 13.sp
                                    )
                                    Slider(
                                        progress = duoHueOffset.toFloat(),
                                        onProgressChange = {
                                            val v = it.toInt().coerceIn(0, 360)
                                            duoHueOffset = v
                                            DashDazzService.updateSettings(context, duoHueOffset = v)
                                        },
                                        minValue = 0f,
                                        maxValue = 360f,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    var currentDegrees by remember {
                                        mutableFloatStateOf(AudioReactiveEngine.degreesPerBeat)
                                    }
                                    Text(
                                        text = stringResource(
                                            R.string.label_degrees_per_beat,
                                            currentDegrees.toInt()
                                        ),
                                        fontSize = 13.sp
                                    )
                                    Slider(
                                        progress = currentDegrees,
                                        onProgressChange = {
                                            currentDegrees = it
                                            AudioReactiveEngine.setDegreesPerBeat(context, it)
                                        },
                                        minValue = 5f,
                                        maxValue = 180f,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(R.string.beat_rainbow_hint),
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                if (serviceState.dualStyle == DashDazzService.DUAL_SPLIT_BEAT) {
                                    Text(
                                        text = stringResource(R.string.title_split_beat_colors),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(R.string.dual_split_beat_hint),
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    val beatColors = listOf(serviceState.splitColorA, serviceState.splitColorB)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        beatColors.forEachIndexed { idx, argb ->
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(argb))
                                                        .border(
                                                            1.dp,
                                                            Color.Gray.copy(alpha = 0.5f),
                                                            CircleShape
                                                        )
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = stringResource(
                                                        if (idx == 0) R.string.spot_left else R.string.spot_right
                                                    ),
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.title_adjustments),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                                if (serviceState.mode != DashDazzService.MODE_SOLID) {

                                    val isSpeedDisabled = serviceState.mode == DashDazzService.MODE_SOLID ||
                                            (serviceState.mode == DashDazzService.MODE_DUAL && (!isRootAvailable || serviceState.dualStyle == DashDazzService.DUAL_HARDWARE_SPLIT))
                                    Text(
                                        text = stringResource(R.string.label_speed, speedMs),
                                        fontSize = 14.sp,
                                        color = if (isSpeedDisabled) Color.Gray else Color.Unspecified
                                    )
                                    Slider(
                                        progress = speedMs.toFloat(),
                                        onProgressChange = { value ->
                                            speedMs = value.toLong()
                                            DashDazzService.updateSettings(context, speedMs = speedMs)
                                        },
                                        minValue = 1f,
                                        maxValue = 250f,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = stringResource(R.string.label_brightness, (brightness * 100).toInt()),
                                    fontSize = 14.sp
                                )
                                Slider(
                                    progress = brightness,
                                    onProgressChange = { value ->
                                        brightness = value
                                        val newColor = getSelectedColor()
                                        if (serviceState.mode == DashDazzService.MODE_DUAL) {
                                            if (serviceState.dualStyle == DashDazzService.DUAL_HARDWARE_SPLIT) {
                                                if (activeSplitSpot == 0) {
                                                    DashDazzService.updateSettings(context, brightness = brightness, splitColorA = newColor)
                                                } else {
                                                    DashDazzService.updateSettings(context, brightness = brightness, splitColorB = newColor)
                                                }
                                            } else if (serviceState.dualStyle == DashDazzService.DUAL_SPIN) {
                                                if (activeSpinSpot == 0) {
                                                    DashDazzService.updateSettings(context, brightness = brightness, spinColorA = newColor)
                                                } else {
                                                    DashDazzService.updateSettings(context, brightness = brightness, spinColorB = newColor)
                                                }
                                            } else {
                                                DashDazzService.updateSettings(context, brightness = brightness)
                                            }
                                        } else {
                                            DashDazzService.updateSettings(
                                                context,
                                                brightness = brightness,
                                                color = newColor
                                            )
                                        }
                                    },
                                    minValue = 0.05f,
                                    maxValue = 1f,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                if (serviceState.mode != DashDazzService.MODE_RAINBOW) {

                                    val isColorDisabled = serviceState.mode == DashDazzService.MODE_RAINBOW ||
                                            (serviceState.mode == DashDazzService.MODE_DUAL &&
                                            (!isRootAvailable ||
                                            (serviceState.dualStyle != DashDazzService.DUAL_HARDWARE_SPLIT &&
                                            serviceState.dualStyle != DashDazzService.DUAL_SPIN)))
                                    Text(
                                        text = stringResource(R.string.label_saturation, (saturation * 100).toInt()),
                                        fontSize = 14.sp,
                                        color = if (isColorDisabled) Color.Gray else Color.Unspecified
                                    )
                                    Slider(
                                        progress = saturation,
                                        onProgressChange = { value ->
                                            saturation = value
                                            val newColor = getSelectedColor()
                                            if (serviceState.mode == DashDazzService.MODE_DUAL) {
                                                if (serviceState.dualStyle == DashDazzService.DUAL_HARDWARE_SPLIT) {
                                                    if (activeSplitSpot == 0) {
                                                        DashDazzService.updateSettings(context, splitColorA = newColor)
                                                    } else {
                                                        DashDazzService.updateSettings(context, splitColorB = newColor)
                                                    }
                                                } else if (serviceState.dualStyle == DashDazzService.DUAL_SPIN) {
                                                    if (activeSpinSpot == 0) {
                                                        DashDazzService.updateSettings(context, spinColorA = newColor)
                                                    } else {
                                                        DashDazzService.updateSettings(context, spinColorB = newColor)
                                                    }
                                                }
                                            } else {
                                                DashDazzService.updateSettings(context, color = newColor)
                                            }
                                        },
                                        minValue = 0.05f,
                                        maxValue = 1f,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = stringResource(R.string.label_hue, hue.toInt()),
                                        fontSize = 14.sp,
                                        color = if (isColorDisabled) Color.Gray else Color.Unspecified
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Slider(
                                            progress = hue,
                                            onProgressChange = { value ->
                                                hue = value
                                                if (saturation < 0.1f) saturation = 1f
                                                val newColor = getSelectedColor()
                                                if (serviceState.mode == DashDazzService.MODE_DUAL) {
                                                    if (serviceState.dualStyle == DashDazzService.DUAL_HARDWARE_SPLIT) {
                                                        if (activeSplitSpot == 0) {
                                                            DashDazzService.updateSettings(context, splitColorA = newColor)
                                                        } else {
                                                            DashDazzService.updateSettings(context, splitColorB = newColor)
                                                        }
                                                    } else if (serviceState.dualStyle == DashDazzService.DUAL_SPIN) {
                                                        if (activeSpinSpot == 0) {
                                                            DashDazzService.updateSettings(context, spinColorA = newColor)
                                                        } else {
                                                            DashDazzService.updateSettings(context, spinColorB = newColor)
                                                        }
                                                    }
                                                } else {
                                                    DashDazzService.updateSettings(context, color = newColor)
                                                }
                                            },
                                            minValue = 0f,
                                            maxValue = 360f,
                                            modifier = Modifier.weight(1f)
                                        )
                                        val previewColor = if (serviceState.mode == DashDazzService.MODE_DUAL) {
                                            if (serviceState.dualStyle == DashDazzService.DUAL_HARDWARE_SPLIT) {
                                                if (activeSplitSpot == 0) serviceState.splitColorA else serviceState.splitColorB
                                            } else if (serviceState.dualStyle == DashDazzService.DUAL_SPIN) {
                                                if (activeSpinSpot == 0) serviceState.spinColorA else serviceState.spinColorB
                                            } else getSelectedColor()
                                        } else getSelectedColor()

                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color(previewColor))
                                                .border(1.5.dp, Color.Gray, CircleShape)
                                        )
                                    }
                                }
                            }
                        }

                    if (serviceState.mode != DashDazzService.MODE_DUAL &&
                        serviceState.mode != DashDazzService.MODE_RAINBOW) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.title_presets),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                val standardPresets = listOf(
                                    Triple(0f, 1f, Color(0xFFFF0000)),
                                    Triple(30f, 1f, Color(0xFFFF9800)),
                                    Triple(60f, 1f, Color(0xFFFFEB3B)),
                                    Triple(120f, 1f, Color(0xFF4CAF50)),
                                    Triple(180f, 1f, Color(0xFF00BCD4)),
                                    Triple(240f, 1f, Color(0xFF2196F3)),
                                    Triple(280f, 1f, Color(0xFF9C27B0)),
                                    Triple(316f, 0.50f, Color(0xFFFFFFFF))
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    standardPresets.forEach { (presetHue, presetSat, color) ->
                                        val isSelected = Math.abs(hue - presetHue) < 2f && Math.abs(saturation - presetSat) < 0.05f
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) MiuixTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    val targetColor = AndroidColor.HSVToColor(floatArrayOf(presetHue, presetSat, brightness)).let {
                                                        if (it == -1) 0xFFFEFEFE.toInt() else it
                                                    }
                                                    hue = presetHue
                                                    saturation = presetSat
                                                    DashDazzService.updateSettings(context, color = targetColor)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(if (presetSat < 0.6f && color == Color.White) Color.Black else Color.White)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.title_saved_presets),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.15f))
                                            .clickable {
                                                Prefs.saveCustomPreset(context, hue, saturation)
                                                customPresets = Prefs.getCustomPresets(context)
                                                Toast.makeText(context, context.getString(R.string.preset_saved), Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.btn_save_preset),
                                            color = MiuixTheme.colorScheme.primary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (customPresets.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.hint_no_saved_presets),
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        customPresets.forEachIndexed { index, (presetHue, presetSat) ->
                                            val isSelected = Math.abs(hue - presetHue) < 2f && Math.abs(saturation - presetSat) < 0.05f
                                            val presetColorInt = AndroidColor.HSVToColor(floatArrayOf(presetHue, presetSat, 1.0f))
                                            val presetColor = Color(presetColorInt)
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(presetColor)
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) MiuixTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                                    shape = CircleShape
                                                )
                                                .combinedClickable(
                                                    onClick = {
                                                        val targetColor = AndroidColor.HSVToColor(floatArrayOf(presetHue, presetSat, brightness)).let {
                                                            if (it == -1) 0xFFFEFEFE.toInt() else it
                                                        }
                                                        hue = presetHue
                                                        saturation = presetSat
                                                        DashDazzService.updateSettings(context, color = targetColor)
                                                    },
                                                    onLongClick = {
                                                        Prefs.removeCustomPreset(context, index)
                                                        customPresets = Prefs.getCustomPresets(context)
                                                        Toast.makeText(context, context.getString(R.string.preset_deleted), Toast.LENGTH_SHORT).show()
                                                    }
                                                ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(if (presetSat < 0.4f) Color.Black else Color.White)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = stringResource(R.string.hint_long_press_delete),
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.title_dark_mode),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val themeOptions = listOf(
                                    0 to R.string.theme_system,
                                    1 to R.string.theme_light,
                                    2 to R.string.theme_dark
                                )
                                themeOptions.forEach { (modeVal, titleRes) ->
                                    val isSelected = (selectedDarkMode == modeVal)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) MiuixTheme.colorScheme.primary
                                                else MiuixTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable {
                                                onDarkModeChange(modeVal)
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(titleRes),
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MiuixTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = stringResource(R.string.audio_card_title),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            Text(
                                text = stringResource(R.string.audio_card_desc),
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            SuperSwitch(
                                title = stringResource(R.string.audio_reactive_toggle),
                                summary = stringResource(
                                    if (serviceState.audioReactive) {
                                        R.string.audio_reactive_on_desc
                                    } else {
                                        R.string.audio_reactive_off_desc
                                    }
                                ),
                                checked = serviceState.audioReactive,
                                onCheckedChange = { enabled ->

                                    if (enabled) {
                                        val hasAudioPerm = ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!hasAudioPerm) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.audio_reactive_permission_rationale),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            return@SuperSwitch
                                        }
                                    }
                                    DashDazzService.updateSettings(context, audioReactive = enabled)
                                }
                            )

                            if (serviceState.audioReactive) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MiuixTheme.colorScheme.primary)
                                        .clickable { showAudioTuningDialog = true }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.audio_open_tuning),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = stringResource(R.string.title_features),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            Text(
                                text = stringResource(R.string.feature_screen_mode_desc),
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    DashDazzService.SCREEN_ON_ONLY to R.string.screen_mode_on,
                                    DashDazzService.SCREEN_OFF_ONLY to R.string.screen_mode_off,
                                    DashDazzService.SCREEN_ALWAYS to R.string.screen_mode_always,
                                    DashDazzService.SCREEN_NOTIF_ONLY to R.string.screen_mode_notif_only
                                ).forEach { (modeVal, titleRes) ->
                                    val isSelected = (serviceState.screenMode == modeVal)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) MiuixTheme.colorScheme.primary
                                                else MiuixTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable {
                                                DashDazzService.updateSettings(context, screenMode = modeVal)
                                            }
                                            .padding(vertical = 12.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(titleRes),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MiuixTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (serviceState.screenMode == DashDazzService.SCREEN_NOTIF_ONLY){

                                Text(
                                    text = stringResource(R.string.title_notification_mode),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        Prefs.NOTIF_MODE_OFF to R.string.notif_mode_off,
                                        Prefs.NOTIF_MODE_ACTIVATE to R.string.notif_mode_activate
                                    ).forEach { (modeVal, titleRes) ->
                                        val isSelected = (serviceState.notifMode == modeVal)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSelected) MiuixTheme.colorScheme.primary
                                                    else MiuixTheme.colorScheme.surfaceVariant
                                                )
                                                .clickable {
                                                    if ((modeVal == Prefs.NOTIF_MODE_ACTIVATE) &&
                                                        !DashDazzNotificationListener.isNotificationAccessGranted(context)
                                                    ) {
                                                        DashDazzNotificationListener.openNotificationAccessSettings(context)
                                                        Toast.makeText(context, R.string.btn_grant_notif_access, Toast.LENGTH_LONG).show()
                                                    }
                                                    DashDazzService.updateSettings(context, notifMode = modeVal)
                                                }
                                                .padding(vertical = 10.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = stringResource(titleRes),
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else MiuixTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(
                                        when (serviceState.notifMode) {
                                            Prefs.NOTIF_MODE_ACTIVATE -> R.string.notif_mode_activate_desc
                                            else -> R.string.notif_mode_off_desc
                                        }
                                    ),
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                if (serviceState.notifMode == Prefs.NOTIF_MODE_ACTIVATE) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = stringResource(R.string.title_notif_duration, notifDuration),
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Slider(
                                        progress = notifDuration.toFloat(),
                                        onProgressChange = { value ->
                                            notifDuration = value.toInt().coerceIn(1, 60)
                                            DashDazzService.updateSettings(
                                                context,
                                                notifActivateDuration = notifDuration
                                            )
                                        },
                                        minValue = 1f,
                                        maxValue = 60f,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = stringResource(R.string.title_root),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            SuperArrow(
                                title = stringResource(
                                    if (isRootAvailable) R.string.root_granted else R.string.root_not_available
                                ),
                                summary = stringResource(
                                    if (isRootAvailable) R.string.root_granted_desc else R.string.root_not_available_desc
                                ),
                                onClick = onRecheckRoot
                            )
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = stringResource(R.string.title_shizuku),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            if (hasShizukuPermission) {
                                SuperArrow(
                                    title = stringResource(R.string.shizuku_connected),
                                    summary = "Binder bridge active (uid 2000)",
                                    onClick = {}
                                )
                            } else {
                                SuperArrow(
                                    title = stringResource(R.string.btn_grant_permission),
                                    summary = stringResource(R.string.shizuku_permission_needed),
                                    onClick = onRequestShizukuPermission
                                )
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.title_about),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(
                                    R.string.about_version,
                                    BuildConfig.VERSION_NAME,
                                    BuildConfig.VERSION_CODE
                                ),
                                fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.about_desc),
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (showAudioTuningDialog) {
                AudioTuningDialog(onDismiss = { showAudioTuningDialog = false })
            }
        }
    }
}

@Composable
fun AudioTuningDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val diag by AudioReactiveEngine.diagnostics.collectAsState()

    var currentGain by remember { mutableFloatStateOf(AudioReactiveEngine.gainMultiplier) }
    var currentThreshold by remember { mutableFloatStateOf(AudioReactiveEngine.beatThreshold) }
    var currentMinHz by remember { mutableFloatStateOf(AudioReactiveEngine.minFreqHz) }
    var currentMaxHz by remember { mutableFloatStateOf(AudioReactiveEngine.maxFreqHz) }
    val currentSource by AudioReactiveEngine.sourceFlow.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.audio_tuning_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MiuixTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.audio_waveform_label, diag.fps),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(75.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val primaryColor = MiuixTheme.colorScheme.primary
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val midY = h / 2f
                        val points = diag.waveform
                        if (points.isNotEmpty()) {
                            val path = Path()
                            val step = w / (points.size - 1).coerceAtLeast(1)
                            for (i in points.indices) {
                                val x = i * step
                                val y = midY + points[i] * (h / 2.2f)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                color = if (diag.hasAudioSignal) primaryColor else Color.Gray,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }

                    if (!diag.hasAudioSignal) {
                        Text(
                            text = if (diag.engineRunning) {
                                stringResource(R.string.audio_no_signal)
                            } else {
                                stringResource(R.string.audio_capture_failed, diag.captureError ?: "")
                            },
                            color = Color(0xFFFF5252),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.audio_rms, (diag.level * 100).toInt()),
                        fontSize = 11.sp
                    )
                    Text(
                        text = stringResource(R.string.audio_band, (diag.targetEnergy * 100).toInt()),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.audio_bpm, diag.bpm),
                        fontSize = 11.sp
                    )
                    Text(
                        text = stringResource(R.string.audio_bps, diag.beatsPerSecond),
                        fontSize = 11.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .alpha(if (diag.isBeat) 1f else 0.35f)
                            .background(if (diag.isBeat) Color(0xFFFF1744) else Color.DarkGray)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (diag.isBeat) {
                                stringResource(R.string.audio_hit)
                            } else {
                                stringResource(R.string.audio_idle)
                            },
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.audio_spectrum_label),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                val lowLabel = stringResource(R.string.audio_meter_low)
                val midLabel = stringResource(R.string.audio_meter_mid)
                val highLabel = stringResource(R.string.audio_meter_high)
                val meters = listOf(
                    Triple(lowLabel, diag.bass, Color(0xFFFF5252)),
                    Triple(midLabel, diag.mid, Color(0xFF69F0AE)),
                    Triple(highLabel, diag.treble, Color(0xFF448AFF))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    meters.forEach { (label, value, barColor) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = label, fontSize = 9.sp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.DarkGray)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(value.coerceIn(0f, 1f))
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(barColor)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.audio_source),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        AudioReactiveEngine.SOURCE_GLOBAL_MIX to R.string.audio_source_global,
                        AudioReactiveEngine.SOURCE_MIC to R.string.audio_source_mic
                    ).forEach { (source, labelRes) ->
                        val selected = currentSource == source
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) MiuixTheme.colorScheme.primary
                                    else MiuixTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    AudioReactiveEngine.setSource(context, source)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(labelRes),
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color.White else MiuixTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.audio_source_band_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (diag.activeBand != AudioReactiveEngine.BAND_AUTO &&
                        diag.activeBand != AudioReactiveEngine.BAND_BROADBAND
                    ) {
                        val labelRes = when (diag.activeBand) {
                            AudioReactiveEngine.BAND_KICK -> R.string.audio_band_kick
                            AudioReactiveEngine.BAND_SNARE -> R.string.audio_band_snare
                            AudioReactiveEngine.BAND_VOCAL -> R.string.audio_band_vocal
                            AudioReactiveEngine.BAND_HAT -> R.string.audio_band_hat
                            else -> null
                        }
                        if (labelRes != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MiuixTheme.colorScheme.primary)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = stringResource(labelRes),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                val bandModeActive by AudioReactiveEngine.bandModeFlow.collectAsState()
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        AudioReactiveEngine.BAND_MODE_AUTO to R.string.audio_band_mode_auto,
                        AudioReactiveEngine.BAND_MODE_MANUAL to R.string.audio_band_mode_manual
                    ).forEach { (mode, labelRes) ->
                        val selected = bandModeActive == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) MiuixTheme.colorScheme.primary
                                    else MiuixTheme.colorScheme.surfaceVariant
                                )
                                .clickable { AudioReactiveEngine.setBandMode(context, mode) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(labelRes),
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color.White else MiuixTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                if (bandModeActive == AudioReactiveEngine.BAND_MODE_AUTO) {
                    Text(
                        text = stringResource(R.string.audio_band_mode_auto_desc),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
                if (bandModeActive == AudioReactiveEngine.BAND_MODE_MANUAL) {
                    Text(
                        text = stringResource(R.string.audio_band_selected, currentMinHz.toInt(), currentMaxHz.toInt()),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    val bandPresets = listOf(
                        R.string.audio_band_kick to (55f to 140f),
                        R.string.audio_band_snare to (200f to 600f),
                        R.string.audio_band_vocal to (800f to 2500f),
                        R.string.audio_band_hat to (5000f to 14000f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        bandPresets.forEach { (labelRes, range) ->
                            val isSel = (currentMinHz == range.first && currentMaxHz == range.second)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        currentMinHz = range.first
                                        currentMaxHz = range.second
                                        AudioReactiveEngine.setFrequencyRange(context, range.first, range.second)
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(labelRes),
                                    fontSize = 9.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.White else MiuixTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.audio_min_freq, currentMinHz.toInt()),
                        fontSize = 11.sp
                    )
                    Slider(
                        progress = currentMinHz,
                        onProgressChange = {
                            val clamped = it.coerceAtMost(currentMaxHz - 50f)
                            currentMinHz = clamped
                            AudioReactiveEngine.setFrequencyRange(context, clamped, currentMaxHz)
                        },
                        minValue = 20f,
                        maxValue = 10000f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = stringResource(R.string.audio_max_freq, currentMaxHz.toInt()),
                        fontSize = 11.sp
                    )
                    Slider(
                        progress = currentMaxHz,
                        onProgressChange = {
                            val clamped = it.coerceAtLeast(currentMinHz + 50f)
                            currentMaxHz = clamped
                            AudioReactiveEngine.setFrequencyRange(context, currentMinHz, clamped)
                        },
                        minValue = 500f,
                        maxValue = 16000f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val autoSensitivity by AudioReactiveEngine.autoSensitivityFlow.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.audio_sensitivity_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            true to R.string.audio_mode_auto,
                            false to R.string.audio_mode_manual
                        ).forEach { (auto, labelRes) ->
                            val selected = autoSensitivity == auto
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected) MiuixTheme.colorScheme.primary
                                        else MiuixTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        AudioReactiveEngine.setAutoSensitivity(context, auto)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(labelRes),
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color.White else MiuixTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (autoSensitivity) {
                    Text(
                        text = stringResource(R.string.audio_sensitivity_auto_desc),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                if (autoSensitivity) {
                    LaunchedEffect(diag.threshold) { currentThreshold = diag.threshold }
                    LaunchedEffect(diag.gainScale) { currentGain = diag.gainScale }
                }
                Text(
                    text = stringResource(R.string.audio_gain, "%.2f".format(currentGain)),
                    fontSize = 11.sp
                )
                Slider(
                    progress = currentGain,
                    onProgressChange = {
                        currentGain = it
                        AudioReactiveEngine.setGain(context, it)
                    },
                    enabled = !autoSensitivity,
                    minValue = 0.2f,
                    maxValue = 3.0f,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.audio_beat_threshold, "%.2f".format(currentThreshold)),
                    fontSize = 11.sp
                )
                Slider(
                    progress = currentThreshold,
                    onProgressChange = {
                        currentThreshold = it
                        AudioReactiveEngine.setBeatThreshold(context, it)
                    },
                    enabled = !autoSensitivity,
                    minValue = 1.05f,
                    maxValue = 1.6f,
                    modifier = Modifier.fillMaxWidth()
                )

                var currentDegrees by remember { mutableFloatStateOf(AudioReactiveEngine.degreesPerBeat) }
                Text(
                    text = stringResource(R.string.label_degrees_per_beat, currentDegrees.toInt()),
                    fontSize = 11.sp
                )
                Slider(
                    progress = currentDegrees,
                    onProgressChange = {
                        currentDegrees = it
                        AudioReactiveEngine.setDegreesPerBeat(context, it)
                    },
                    minValue = 5f,
                    maxValue = 180f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MiuixTheme.colorScheme.surfaceVariant)
                        .clickable {

                            currentGain = 0.7f
                            currentThreshold = 1.18f
                            currentMinHz = 5000f
                            currentMaxHz = 14000f
                            AudioReactiveEngine.setSource(context, AudioReactiveEngine.SOURCE_GLOBAL_MIX)
                            AudioReactiveEngine.setGain(context, 0.7f)
                            AudioReactiveEngine.setBeatThreshold(context, 1.18f)
                            AudioReactiveEngine.setFrequencyRange(context, 5000f, 14000f)
                            AudioReactiveEngine.setBandMode(context, AudioReactiveEngine.BAND_MODE_AUTO)
                            AudioReactiveEngine.setAutoSensitivity(context, true)
                            AudioReactiveEngine.setDegreesPerBeat(context, 30f)
                            DashDazzService.updateSettings(context, duoHueOffset = 90)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.audio_reset),
                        color = MiuixTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MiuixTheme.colorScheme.primary)
                        .clickable { onDismiss() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.btn_close),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
