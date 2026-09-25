package xyz.wienerlabs.rasad.ui.sky

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.wienerlabs.rasad.ui.RasadIcons
import xyz.wienerlabs.rasad.ui.components.Hairline
import xyz.wienerlabs.rasad.ui.components.RoundIconButton
import xyz.wienerlabs.rasad.ui.components.SectionLabel
import xyz.wienerlabs.rasad.ui.theme.Palette
import xyz.wienerlabs.rasad.ui.theme.RasadType

private data class Credit(val title: String, val detail: String)

private val credits = listOf(
    Credit("Yıldız kataloğu", "HYG Database v4.1, David Nash (astronexus). CC BY-SA 4.0. Uygulamadaki türetilmiş yıldız verisi de aynı lisansla paylaşılır."),
    Credit("Takımyıldız çizgileri ve Samanyolu", "d3-celestial, Olaf Frohn. BSD 3-Clause lisansı."),
    Credit("Gök mekaniği", "Astronomy Engine, Don Cross. MIT lisansı. Güneş, Ay ve gezegen konumları, doğuş ve batış aramaları."),
    Credit("Ay dokusu", "NASA Goddard Scientific Visualization Studio, CGI Moon Kit (LRO LROC verisi). Kamu malı."),
    Credit("Dünya haritası", "Natural Earth, 1:110m kara poligonları. Kamu malı."),
    Credit("Yazı tipleri", "Funnel Display ve Funnel Sans (NORD ID), Amiri (Khaled Hosny). SIL Open Font License 1.1."),
    Credit("Hilal görünürlüğü", "B. D. Yallop, NAO Technical Note 69. En iyi gözlem anı gün batımı artı gecikmenin 4/9'u; q ölçütü A ile F arası."),
    Credit("Yıldız adlarının kökeni", "Arapça adlar ve anlamları, Paul Kunitzsch ve Tim Smart'ın yıldız adları çalışmaları ile klasik İslam astronomisi kaynaklarından derlendi."),
)

@Composable
fun AboutPanel(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(Palette.Ink)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Rasad", style = RasadType.title, color = Palette.Text)
                Text("Kaynaklar ve lisanslar", style = RasadType.caption, color = Palette.TextFaint)
            }
            RoundIconButton(RasadIcons.Close, "Kapat", onDismiss)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text(
                "Gökyüzü, hilal ve kıble hesapları tamamen cihazda yapılır ve internet gerektirmez. Yalnızca konumunun adını göstermek için Android'in adres servisi kullanılabilir.",
                style = RasadType.body,
                color = Palette.TextMuted,
            )
            Spacer(Modifier.height(18.dp))
            credits.forEachIndexed { index, credit ->
                if (index > 0) Hairline()
                Column(Modifier.padding(vertical = 14.dp)) {
                    SectionLabel(credit.title)
                    Spacer(Modifier.height(4.dp))
                    Text(credit.detail, style = RasadType.body, color = Palette.Text)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Wiener Labs", style = RasadType.label, color = Palette.TextFaint)
            Spacer(Modifier.height(32.dp))
        }
    }
}
