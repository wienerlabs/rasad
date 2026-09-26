package xyz.wienerlabs.rasad.ui.islam

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.wienerlabs.rasad.islam.IslamicTexts
import xyz.wienerlabs.rasad.ui.RasadIcons
import xyz.wienerlabs.rasad.ui.components.RoundIconButton
import xyz.wienerlabs.rasad.ui.components.SectionLabel
import xyz.wienerlabs.rasad.ui.theme.Palette
import xyz.wienerlabs.rasad.ui.theme.RasadType

private val guidanceVerses = listOf("6:97", "16:16", "67:5")

@Composable
fun StarNotePanel(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(Palette.Ink)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Yıldızlar ve İslam", style = RasadType.title, color = Palette.Text)
                Text("Yön, vakit ve takvim için gök", style = RasadType.caption, color = Palette.TextFaint)
            }
            RoundIconButton(RasadIcons.Close, "Kapat", onDismiss)
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.widthIn(max = 720.dp)) {
                Text(
                    "Kur'an yıldızları, karada ve denizde yol bulmaya yarayan işaretler ve göğün süsü olarak anar. Âlimler yıldız ilmini bu yüzden ikiye ayırır.",
                    style = RasadType.body,
                    color = Palette.TextMuted,
                )
                IslamicTexts["uthaymin:ikiye"]?.let {
                    Spacer(Modifier.height(12.dp))
                    SourceCard(it)
                }
                Spacer(Modifier.height(16.dp))
                NoteBlock(
                    "İlm-i tesyîr: meşru olan",
                    "Güneş'in, Ay'ın ve yıldızların hareketini gözleyip yön, kıble, namaz vakti ve mevsim bulmak; hilalin nerede ve ne zaman aranacağını bilmek. Hicrî ay ise hesapla değil, hilalin görülmesiyle başlar. Rasad'daki bütün hesaplar bu sınırın içinde kalır.",
                )
                IslamicTexts["uthaymin:tesyir"]?.let {
                    Spacer(Modifier.height(10.dp))
                    SourceCard(it)
                }
                Spacer(Modifier.height(10.dp))
                NoteBlock(
                    "İlm-i te'sîr: reddedilen",
                    "Yıldızların insanların talihine, sağlığına, işlerine ya da yağmura etki ettiğini ileri sürmek. Burç yorumları, fal, uğurlu ve uğursuz saatler bu türdendir; aşağıdaki hadisler bunu açıkça reddeder.",
                )
                Spacer(Modifier.height(24.dp))
                SectionLabel("Yol bulmak ve göğün süsü")
                Spacer(Modifier.height(8.dp))
                guidanceVerses.mapNotNull { IslamicTexts[it] }.forEach {
                    SourceCard(it)
                    Spacer(Modifier.height(10.dp))
                }
                IslamicTexts["qatada"]?.let {
                    Spacer(Modifier.height(14.dp))
                    SectionLabel("Katâde'nin ölçüsü")
                    Spacer(Modifier.height(8.dp))
                    SourceCard(it)
                }
                IslamicTexts["bukhari:846"]?.let {
                    Spacer(Modifier.height(24.dp))
                    SectionLabel("Yağmur yıldızdan değildir")
                    Spacer(Modifier.height(8.dp))
                    SourceCard(it)
                }
                IslamicTexts["abudawud:3905"]?.let {
                    Spacer(Modifier.height(24.dp))
                    SectionLabel("Yıldızlardan hüküm çıkarmak")
                    Spacer(Modifier.height(8.dp))
                    SourceCard(it)
                    IslamicTexts["hattabi"]?.let { explanation ->
                        Spacer(Modifier.height(10.dp))
                        SourceCard(explanation)
                    }
                }
                IslamicTexts["muslim:934"]?.let {
                    Spacer(Modifier.height(24.dp))
                    SectionLabel("Câhiliye'den kalanlar")
                    Spacer(Modifier.height(8.dp))
                    SourceCard(it)
                }
                IslamicTexts["53:49"]?.let {
                    Spacer(Modifier.height(24.dp))
                    SectionLabel("Kur'an'da adıyla anılan yıldız")
                    Spacer(Modifier.height(8.dp))
                    SourceCard(it)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Mealdeki Şira (Arapça eş-Şi'râ), bugün Sirius diye bilinen yıldızdır.",
                        style = RasadType.caption,
                        color = Palette.TextFaint,
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "Bu yüzden Rasad'da burç, fal, uğur ya da uğursuzluk bildiren hiçbir içerik yer almaz. Yıldızların Arapça adları dilin ve astronomi tarihinin mirası olarak gösterilir; eski Arapların adlandırma anlatıları yalnızca adın nereden geldiğini açıklar, inanç olarak aktarılmaz.",
                    style = RasadType.body,
                    color = Palette.TextMuted,
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
