package com.dashdazz.light

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SetupScreen(
    isRootAvailable: Boolean,
    rootChecked: Boolean,
    hasShizukuPermission: Boolean,
    onRecheckRoot: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onContinue: () -> Unit
) {
    val canRun = isRootAvailable || hasShizukuPermission

    Scaffold(
        topBar = { TopAppBar(title = stringResource(R.string.app_name)) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.setup_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.primary)
                )
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.surfaceVariant)
                )
                Text(
                    text = stringResource(R.string.setup_step),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = stringResource(R.string.setup_desc),
                fontSize = 14.sp,
                color = Color.Gray
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = stringResource(R.string.title_root),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    when {
                        !rootChecked -> SuperArrow(
                            title = stringResource(R.string.root_checking),
                            summary = stringResource(R.string.root_checking_desc),
                            onClick = {}
                        )
                        isRootAvailable -> SuperArrow(
                            title = stringResource(R.string.root_granted),
                            summary = stringResource(R.string.setup_root_ok_desc),
                            onClick = {}
                        )
                        else -> SuperArrow(
                            title = stringResource(R.string.root_not_available),
                            summary = stringResource(R.string.setup_root_missing_desc),
                            onClick = onRecheckRoot
                        )
                    }
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
                            summary = stringResource(R.string.setup_shizuku_ok_desc),
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

            if (!canRun && rootChecked) {
                Text(
                    text = stringResource(R.string.setup_need_one),
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.primary
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MiuixTheme.colorScheme.primary)
                    .clickable { onContinue() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(if (canRun) R.string.setup_continue else R.string.setup_continue_anyway),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
