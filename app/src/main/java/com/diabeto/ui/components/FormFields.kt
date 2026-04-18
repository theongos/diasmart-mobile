package com.diabeto.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import com.diabeto.ui.theme.Error
import com.diabeto.ui.theme.Primary
import com.diabeto.ui.theme.TextPrimary
import com.diabeto.ui.theme.TextSecondary

/**
 * Libelle de formulaire qui ajoute un asterisque rouge si le champ est obligatoire.
 *
 * Usage :
 * ```
 * OutlinedTextField(
 *   label = { RequiredFieldLabel("Nom", required = true) },
 *   ...
 * )
 * ```
 */
@Composable
fun RequiredFieldLabel(
    text: String,
    required: Boolean = false,
    color: Color = TextSecondary,
    asteriskColor: Color = Error
) {
    if (!required) {
        Text(text = text, color = color)
        return
    }
    val annotated: AnnotatedString = buildAnnotatedString {
        withStyle(SpanStyle(color = color)) { append(text) }
        append(" ")
        withStyle(SpanStyle(color = asteriskColor)) { append("*") }
    }
    Text(text = annotated)
}

/**
 * Couleurs par defaut pour les OutlinedTextField de DiaSmart.
 * Contraste renforce (texte fonce, labels visibles).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun diaSmartTextFieldColors(
    textColor: Color = TextPrimary,
    labelColor: Color = TextSecondary,
    focusedBorder: Color = Primary
): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = textColor,
    unfocusedTextColor = textColor,
    disabledTextColor = textColor.copy(alpha = 0.7f),
    focusedLabelColor = focusedBorder,
    unfocusedLabelColor = labelColor,
    focusedBorderColor = focusedBorder,
    unfocusedBorderColor = labelColor.copy(alpha = 0.4f),
    cursorColor = focusedBorder
)

/**
 * Version pre-cablee d'OutlinedTextField avec couleurs DiaSmart + support du flag `required`
 * qui affiche l'asterisque rouge dans le label.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaSmartTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    placeholder: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        label = { RequiredFieldLabel(label, required = required) },
        placeholder = placeholder?.let { { Text(it, color = TextSecondary.copy(alpha = 0.7f)) } },
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        isError = isError,
        supportingText = supportingText,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        colors = diaSmartTextFieldColors()
    )
}
