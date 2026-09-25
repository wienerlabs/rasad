package xyz.wienerlabs.rasad.ui.sky

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.wienerlabs.rasad.sky.SkyLayers
import xyz.wienerlabs.rasad.ui.components.Hairline
import xyz.wienerlabs.rasad.ui.components.SectionLabel
import xyz.wienerlabs.rasad.ui.components.ToggleRow
import xyz.wienerlabs.rasad.ui.components.panel
import xyz.wienerlabs.rasad.ui.components.pressable
import xyz.wienerlabs.rasad.ui.theme.Palette
import xyz.wienerlabs.rasad.ui.theme.RasadType

@Composable
fun LayersPanel(
    layers: SkyLayers,
    nightVision: Boolean,
    onLayersChange: (SkyLayers) -> Unit,
    onNightVisionChange: (Boolean) -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.width(272.dp).panel(RoundedCornerShape(22.dp), strong = true).padding(vertical = 8.dp)) {
        SectionLabel("Gökyüzü katmanları", Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
        ToggleRow("Takımyıldız çizgileri", layers.constellationLines, { onLayersChange(layers.copy(constellationLines = it)) })
        ToggleRow("Takımyıldız adları", layers.constellationNames, { onLayersChange(layers.copy(constellationNames = it)) })
        ToggleRow("Yıldız adları", layers.starNames, { onLayersChange(layers.copy(starNames = it)) })
        ToggleRow("Samanyolu", layers.milkyWay, { onLayersChange(layers.copy(milkyWay = it)) })
        ToggleRow("Ufuk ızgarası", layers.grid, { onLayersChange(layers.copy(grid = it)) })
        ToggleRow("Kıble işareti", layers.qibla, { onLayersChange(layers.copy(qibla = it)) })
        ToggleRow("Zemini göster", layers.ground, { onLayersChange(layers.copy(ground = it)) })
        Hairline(Modifier.padding(vertical = 4.dp))
        ToggleRow("Gece görüşü", nightVision, onNightVisionChange)
        Hairline(Modifier.padding(vertical = 4.dp))
        Text(
            "Kaynaklar ve lisanslar",
            style = RasadType.body,
            color = Palette.TextMuted,
            modifier = Modifier.fillMaxWidth().pressable(onClick = onOpenAbout).padding(horizontal = 16.dp, vertical = 11.dp),
        )
    }
}
