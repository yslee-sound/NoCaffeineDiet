package com.sweetapps.nocaffeinediet.feature.about

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sweetapps.nocaffeinediet.core.ui.AppElevation
import com.sweetapps.nocaffeinediet.core.ui.BaseActivity
import com.sweetapps.nocaffeinediet.R
import com.sweetapps.nocaffeinediet.core.ui.AppBorder

class AboutLicensesActivity : BaseActivity() {
    override fun getScreenTitle(): String = getString(R.string.about_open_license_notice)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BaseScreen(showBackButton = true) { AboutLicensesScreen() } }
    }
}

@Composable
private fun AboutLicensesScreen() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val ccByUrl = "https://creativecommons.org/licenses/by/4.0/"
    val sourceUrl = "https://www.figma.com/community/file/1227184301417272677/free-wayfinding-vector-icons-guidance-icon-set"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD),
            border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.about_open_license_notice),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Text(text = stringResource(R.string.about_section_app_icon), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                LabeledText(label = stringResource(R.string.about_label_original_work), value = stringResource(R.string.about_value_icon_name))
                LabeledText(label = stringResource(R.string.about_label_author), value = stringResource(R.string.about_value_icon_author))
                LabeledText(
                    label = stringResource(R.string.about_label_source),
                    value = stringResource(R.string.about_label_source_link),
                    isLink = true,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, sourceUrl.toUri())) },
                    trailingContent = {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_copy),
                            modifier = Modifier.clickable {
                                clipboard.setText(AnnotatedString(sourceUrl))
                                Toast.makeText(context, context.getString(R.string.toast_copied), Toast.LENGTH_SHORT).show()
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                LabeledText(
                    label = stringResource(R.string.about_label_license),
                    value = stringResource(R.string.about_value_license_cc_by),
                    isLink = true,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, ccByUrl.toUri())) }
                )
                LabeledText(label = stringResource(R.string.about_label_changes), value = stringResource(R.string.about_value_change_desc))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.about_notice_compliance),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5F6C7B)
                )
            }
        }
    }
}

@Composable
private fun LabeledText(
    label: String,
    value: String,
    isLink: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val labelStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF5F6C7B))
    val valueStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label:", style = labelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(8.dp))
        if (isLink && onClick != null) {
            Text(
                text = value,
                modifier = Modifier.clickable(onClick = onClick),
                style = valueStyle.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = TextDecoration.Underline
                )
            )
        } else {
            Text(text = value, style = valueStyle)
        }
        if (trailingContent != null) {
            Spacer(Modifier.weight(1f))
            trailingContent()
        }
    }
}
