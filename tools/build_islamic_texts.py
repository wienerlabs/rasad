import json
import pathlib
import re
import sys

REPO = pathlib.Path(__file__).resolve().parents[1]
KOTLIN = REPO / "app/src/main/java/xyz/wienerlabs/rasad/islam/IslamicTexts.kt"
FIXTURE = REPO / "app/src/test/resources/islamic-sources.tsv"

data = json.loads((REPO / "tools/islamic_sources.json").read_text())
ayat = {a["ref"]: a for a in data["ayat"]}
hadith = {h["id"]: h for h in data["hadith"]}

SURAH = {2: "Bakara", 4: "Nisâ", 6: "En'âm", 9: "Tevbe", 10: "Yûnus", 16: "Nahl", 53: "Necm", 67: "Mülk"}

AYAT = {
    "2:144": {},
    "2:185": {},
    "2:189": {},
    "4:103": {},
    "6:97": {},
    "9:36": {},
    "10:5": {},
    "16:16": {"excerptFrom": "işaretler", "translatorNote": " (15. âyetle birlikte verilen mealden)"},
    "53:49": {},
    "67:5": {},
}

HADITH = {
    "bukhari:1909": {
        "source": "bukhari:1909",
        "meaning": "Onu (hilali) görünce oruç tutun, onu görünce orucu bırakın. Size gizli kalırsa (göremezseniz) Şaban'ın sayısını otuza tamamlayın.",
        "citation": "Buhârî, 1909",
        "short": "Buhârî 1909",
        "grade": "Sahih",
    },
    "bukhari:846": {
        "source": "bukhari:846",
        "meaning": "(Allah buyurdu ki:) Kullarımdan kimi bana inanan, kimi de inkâr eden olarak sabahladı. ‘Allah'ın lütfu ve rahmetiyle yağmura kavuştuk’ diyen bana inanmış, yıldızı inkâr etmiştir. ‘Falan falan yıldızın nev'iyle (doğup batmasıyla)’ diyen ise beni inkâr etmiş, yıldıza inanmıştır.",
        "citation": "Buhârî, 846 · hadis-i kudsî",
        "short": "Buhârî 846",
        "grade": "Sahih",
    },
    "qatada": {
        "source": "bukhari-book59-chapter3-intro",
        "field": "arabic",
        "meaning": "Katâde, ‘Andolsun ki, yakın göğü kandillerle donattık’ âyeti hakkında dedi ki: Allah bu yıldızları üç şey için yarattı: Onları göğe süs, şeytanlara atılan taşlar ve kendileriyle yol bulunan işaretler kıldı. Kim onlara bundan başka bir anlam yüklerse yanılmış, nasibini yitirmiş ve hakkında bilgisi olmayan bir işe kalkışmış olur.",
        "citation": "Katâde'nin sözü (maktû') · Buhârî, Bed'ü'l-halk kitabı, Nücûm bâbının başında isnadsız (ta'lîkan)",
        "short": "Katâde (Buhârî)",
        "grade": None,
    },
    "tirmidhi:3451": {
        "source": "tirmidhi:3451",
        "meaning": "Allah'ım! Onu üzerimize bereket ve iman, esenlik ve İslam ile doğdur. (Ey hilal!) Benim Rabbim de senin Rabbin de Allah'tır.",
        "citation": "Tirmizî, 3451",
        "short": "Tirmizî 3451",
        "grade": "Tirmizî: hasen garîb · Elbânî: yollarının toplamıyla sahih · Dârüsselâm: zayıf",
        "transliteration": "Allâhümme ehlilhü aleynâ bi'l-yümni ve'l-îmân, ve's-selâmeti ve'l-İslâm. Rabbî ve Rabbüke'llâh.",
    },
    "bukhari:1044": {
        "source": "bukhari:1044",
        "meaning": "Güneş ve Ay, Allah'ın âyetlerinden iki âyettir; kimsenin ölümü ya da hayatı sebebiyle tutulmazlar. Bunu gördüğünüzde Allah'a dua edin, tekbir getirin, namaz kılın ve sadaka verin.",
        "citation": "Buhârî, 1044",
        "short": "Buhârî 1044",
        "grade": "Sahih",
    },
    "muslim:1094": {
        "source": "muslim:1094c",
        "fromKeyToEnd": True,
        "meaning": "Bilâl'in ezanı da, ufuktaki şöyle uzanan beyazlık da sizi sahurunuz konusunda yanıltmasın; ta ki (aydınlık) şöyle yayılıncaya kadar. Râvi Hammâd bunu elleriyle gösterdi; yani yatay olarak.",
        "citation": "Müslim, 1094c",
        "short": "Müslim 1094",
        "grade": "Sahih",
    },
    "muslim:1162": {
        "source": "muslim:1162a",
        "meaning": "Arefe günü orucunun, önceki yılın ve sonraki yılın günahlarına kefaret olmasını Allah'tan umarım. Âşûrâ günü orucunun da önceki yılın günahlarına kefaret olmasını Allah'tan umarım.",
        "citation": "Müslim, 1162a (hadisin bir bölümü)",
        "short": "Müslim 1162",
        "grade": "Sahih",
    },
    "muslim:1134": {
        "source": "muslim:1134b",
        "meaning": "Gelecek yıla ulaşırsam, dokuzuncu günü mutlaka oruç tutacağım.",
        "citation": "Müslim, 1134b",
        "short": "Müslim 1134",
        "grade": "Sahih",
    },
    "muslim:1164": {
        "source": "muslim:1164a",
        "meaning": "Kim Ramazan orucunu tutar, ardından ona Şevval'den altı gün eklerse, sürekli oruç tutmuş gibi olur.",
        "citation": "Müslim, 1164a",
        "short": "Müslim 1164",
        "grade": "Sahih",
    },
    "tirmidhi:761": {
        "source": "tirmidhi:761",
        "meaning": "Ey Ebû Zer! Aydan üç gün oruç tutacaksan on üçüncü, on dördüncü ve on beşinci günleri tut.",
        "citation": "Tirmizî, 761",
        "short": "Tirmizî 761",
        "grade": "Tirmizî: hasen · Elbânî: hasen sahih · Dârüsselâm: hasen",
    },
    "tirmidhi:757": {
        "source": "tirmidhi:757",
        "meaning": "Salih amelin Allah katında bu on günden daha sevimli olduğu hiçbir gün yoktur.",
        "citation": "Tirmizî, 757 (hadisin başı)",
        "short": "Tirmizî 757",
        "grade": "Tirmizî: hasen sahih garîb · Dârüsselâm: sahih",
    },
    "bukhari:2017": {
        "source": "bukhari:2017",
        "meaning": "Kadir Gecesi'ni Ramazan'ın son on gecesinin tek olanlarında arayın.",
        "citation": "Buhârî, 2017",
        "short": "Buhârî 2017",
        "grade": "Sahih",
    },
    "muslim:1141": {
        "source": "muslim:1141a",
        "meaning": "Teşrik günleri yeme ve içme günleridir.",
        "citation": "Müslim, 1141a",
        "short": "Müslim 1141",
        "grade": "Sahih",
    },
    "tirmidhi:149": {
        "source": "tirmidhi:149",
        "meaning": "Cebrâil (aleyhisselâm) Beytullah'ın yanında bana iki kez imamlık yaptı. Birincisinde öğleyi, gölge ayakkabı bağı kadarken kıldırdı; sonra ikindiyi, her şeyin gölgesi kendi boyu kadar olduğunda kıldırdı.",
        "citation": "Tirmizî, 149 (hadisin başı)",
        "short": "Tirmizî 149",
        "grade": "Tirmizî: hasen sahih · Elbânî: hasen sahih · Dârüsselâm: hasen",
    },
    "tirmidhi:149:fecr": {
        "source": "tirmidhi:149",
        "clauseAfterToken": 17,
        "meaning": "Sonra sabah namazını, fecir doğup oruçluya yemek haram olduğunda kıldırdı.",
        "citation": "Tirmizî, 149 (hadisin bir bölümü)",
        "short": "Tirmizî 149 · fecir",
        "grade": "Tirmizî: hasen sahih · Elbânî: hasen sahih · Dârüsselâm: hasen",
    },
    "abudawud:3905": {
        "source": "abudawud:3905",
        "meaning": "Kim yıldızlardan bir ilim edinirse sihirden bir şube edinmiş olur; onu arttırdıkça bu da artar.",
        "citation": "Ebû Dâvûd, 3905",
        "short": "Ebû Dâvûd 3905",
        "grade": "Elbânî: hasen",
    },
    "muslim:934": {
        "source": "muslim:934",
        "meaning": "Ümmetimde Câhiliye işlerinden dört şey vardır ki onları bırakmazlar: soylarla övünmek, soylara dil uzatmak, yağmuru yıldızlardan beklemek ve ölünün ardından feryat ederek ağlamak.",
        "citation": "Müslim, 934 (hadisin bir bölümü)",
        "short": "Müslim 934",
        "grade": "Sahih",
    },
    "hattabi": {
        "source": "khattabi-on-ilm-al-nujum",
        "field": "quoteArabic",
        "tokenSlice": [3, -1],
        "meaning": "Yasaklanan yıldız ilmi, müneccimlerin henüz olmamış olaylar ve hadiseler hakkında, yağmurların geleceği ya da fiyatların değişeceği gibi, haber verdikleri bilgidir. Namaz vakitlerinin ve kıble yönünün kendisiyle bilindiği ilim ise yasaklananın kapsamına girmez.",
        "citation": "Hattâbî, Meâlimü's-sünen · Avnü'l-ma'bûd 10/319'daki aktarımla",
        "short": "Hattâbî",
        "grade": None,
    },
    "uthaymin:ikiye": {
        "source": "uthaymin-tasir-tasyir",
        "field": "quoteArabic",
        "meaning": "Yıldız ilmi iki kısma ayrılır: 1. Te'sîr (etki) ilmi. 2. Tesyîr (seyir) ilmi.",
        "citation": "İbn Useymîn, el-Kavlü'l-müfîd 2/5 · Dürerü's-seniyye'deki aktarımla",
        "short": "İbn Useymîn",
        "grade": None,
    },
    "uthaymin:tesyir": {
        "source": "uthaymin-tasir-tasyir",
        "field": "quoteTasyirArabic",
        "meaning": "İkincisi tesyîr ilmidir. Bu da iki kısma ayrılır. Birincisi, yıldızların seyrinden dinî maslahatlar için delil çıkarmaktır; bu istenen bir şeydir. Vacip olan dinî maslahatlara yardım ediyorsa onu öğrenmek vacip olur; yıldızlarla kıble yönünü bulmak istemek gibi: falan yıldız gecenin üçte birinde kıble yönündedir, falan yıldız gecenin dörtte birinde kıble yönündedir. Bunda büyük fayda vardır.",
        "citation": "İbn Useymîn, el-Kavlü'l-müfîd 2/5 · Dürerü's-seniyye'deki aktarımla",
        "short": "İbn Useymîn",
        "grade": None,
    },
    "abudawud:1134": {
        "source": "abudawud:1134",
        "meaning": "Allah size (Câhiliye'de eğlendiğiniz) bu iki günün yerine onlardan daha hayırlısını verdi: Kurban Bayramı günü ve Ramazan Bayramı günü.",
        "citation": "Ebû Dâvûd, 1134",
        "short": "Ebû Dâvûd 1134",
        "grade": "Elbânî: sahih",
    },
    "bukhari:1990": {
        "source": "bukhari:1990",
        "meaning": "(Ömer dedi ki:) Bu ikisi, Resûlullah'ın (s.a.v.) oruç tutmayı yasakladığı iki gündür: Orucunuzu bitirdiğiniz gün ve kurbanlarınızdan yediğiniz diğer gün.",
        "citation": "Buhârî, 1990",
        "short": "Buhârî 1990",
        "grade": "Sahih",
    },
    "bukhari:3197": {
        "source": "bukhari:3197",
        "meaning": "Zaman, Allah'ın gökleri ve yeri yarattığı gündeki hâline döndü. Yıl on iki aydır; bunlardan dördü haram aylardır. Üçü art arda gelir: Zilkade, Zilhicce ve Muharrem. (Dördüncüsü) Cemâzî ile Şaban arasındaki Mudar'ın Recebi'dir.",
        "citation": "Buhârî, 3197",
        "short": "Buhârî 3197",
        "grade": "Sahih",
    },
}

grades = {}


def clean(text):
    return text.replace("\u200f", "")


def normalize(text):
    text = clean(text).replace('"', "").replace("“", "").replace("”", "")
    text = re.sub(r"\s+", " ", text)
    return text.replace(" .", ".").replace(" ،", "،").strip()


def kotlin(text):
    return '"' + text.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def full_arabic(h):
    return h.get("arabicFull") or h.get("arabic") or h.get("quoteFullArabic") or h["quoteArabic"]


entries = []
fixture = []

for ref, spec in AYAT.items():
    a = ayat[ref]
    surah, verse = ref.split(":")
    name = SURAH[int(surah)]
    meaning = a["meal"]
    if "excerptFrom" in spec:
        meaning = "… " + meaning[meaning.index(spec["excerptFrom"]):]
    entries.append({
        "key": ref,
        "arabic": a["arabic"],
        "meaning": meaning,
        "citation": f"Kur'an, {name} {ref}",
        "short": f"{name} {ref}",
        "grade": None,
        "translator": "DIYANET" + (f' + "{spec["translatorNote"]}"' if "translatorNote" in spec else ""),
    })
    fixture.append((ref, a["url"], f"Quran {ref} ({a['arabicEdition']}, {a['mealEdition']})", normalize(a["arabic"]), re.sub(r"\s+", " ", a["meal"]).strip()))

missing = []
for key, spec in HADITH.items():
    h = hadith.get(spec["source"])
    if h is None:
        missing.append(key)
        continue
    if "clauseAfterToken" in spec:
        verified = normalize(full_arabic(h))
        marker = normalize(h["keyStatementArabic"]).split(" ")[spec["clauseAfterToken"]]
        period = verified.index(".", verified.index(normalize(h["keyStatementArabic"])))
        start = verified.rfind(" " + marker + " ", 0, period) + 1
        arabic = verified[start:period]
    elif spec.get("fromKeyToEnd"):
        verified = normalize(full_arabic(h))
        arabic = verified[verified.index(normalize(h["keyStatementArabic"])):]
    elif spec.get("field", "").startswith("quote"):
        arabic = normalize(h[spec["field"]])
        if "tokenSlice" in spec:
            first, last = spec["tokenSlice"]
            arabic = " ".join(arabic.split(" ")[first:last])
    else:
        arabic = (h["arabic"] if spec.get("field") == "arabic" else h.get("keyStatementArabic")) or h["arabic"]
        arabic = clean(arabic).strip()
    if normalize(arabic) not in normalize(full_arabic(h)):
        sys.exit(f"arabic for {key} is not a substring of the verified text")
    grade = spec["grade"]
    if grade and grade.startswith("GRADE_"):
        grade = grades.get(grade)
        if grade is None:
            missing.append(f"{key} grade")
    entries.append({
        "key": key,
        "arabic": arabic,
        "meaning": spec["meaning"],
        "citation": spec["citation"],
        "short": spec["short"],
        "grade": grade,
        "translator": "RASAD",
        "transliteration": spec.get("transliteration"),
    })
    fixture.append((key, h["url"], h.get("reference") or h.get("quotedIn"), normalize(full_arabic(h)), ""))

lines = [
    "package xyz.wienerlabs.rasad.islam",
    "",
    "data class SourceText(",
    "    val key: String,",
    "    val arabic: String?,",
    "    val meaning: String,",
    "    val citation: String,",
    "    val shortCitation: String,",
    "    val grade: String? = null,",
    "    val translator: String? = null,",
    "    val transliteration: String? = null,",
    ")",
    "",
    "object IslamicTexts {",
    '    private const val DIYANET = "meal: Diyanet İşleri"',
    '    private const val RASAD = "çeviri: Rasad"',
    "",
    "    private val all: Map<String, SourceText> = listOf(",
]
for e in entries:
    lines.append("        SourceText(")
    lines.append(f"            key = {kotlin(e['key'])},")
    lines.append(f"            arabic = {kotlin(e['arabic'])},")
    lines.append(f"            meaning = {kotlin(e['meaning'])},")
    lines.append(f"            citation = {kotlin(e['citation'])},")
    lines.append(f"            shortCitation = {kotlin(e['short'])},")
    if e.get("grade"):
        lines.append(f"            grade = {kotlin(e['grade'])},")
    lines.append(f"            translator = {e['translator']},")
    if e.get("transliteration"):
        lines.append(f"            transliteration = {kotlin(e['transliteration'])},")
    lines.append("        ),")
lines += [
    "    ).associateBy { it.key }",
    "",
    "    operator fun get(key: String): SourceText? = all[key]",
    "",
    "    val keys: Set<String> get() = all.keys",
    "}",
    "",
]
text = "\n".join(lines)
if "\u2014" in text:
    sys.exit("em dash in generated text")
KOTLIN.write_text(text)

FIXTURE.parent.mkdir(parents=True, exist_ok=True)
rows = ["key\turl\treference\tverifiedArabic\tverifiedMeal"]
for row in fixture:
    if any("\t" in part or "\n" in part for part in row):
        sys.exit(f"tab or newline in fixture row {row[0]}")
    rows.append("\t".join(row))
FIXTURE.write_text("\n".join(rows) + "\n")

print(f"entries: {len(entries)}, fixture rows: {len(fixture)}")
if missing:
    sys.exit(f"missing: {missing}")

doc = [
    "# Islamic texts in Rasad",
    "",
    "Every Arabic text shown in the app is cut from a verified source and checked by `IslamicTextsTest` against `app/src/test/resources/islamic-sources.tsv`. Turkish hadith and scholar translations are Rasad's own; the Quran meal is the Diyanet İşleri translation published on Tanzil.",
    "",
    "Sources, fetched " + data["meta"]["fetchedOn"] + ":",
    "",
    "- Quran: Tanzil simple script and the tr.diyanet meal via api.alquran.cloud, saved straight from the API responses. Letters and diacritics are used unchanged; on screen, pause marks are moved onto the preceding word.",
    "- Hadith: sunnah.com pages read in a browser and compared byte for byte (SHA-256) with the live page.",
    "- Gradings outside Bukhari and Muslim: the collection's own verdict and Albani's verdict as listed on dorar.net, plus the Darussalam grade shown on sunnah.com, wherever each exists.",
    "- Scholar statements: quoted from the online sources named in the table, because the books themselves could not be opened; the app says \"aktarımla\" for these.",
    "",
    "| Key | Source | Grading shown in the app | Grading sources |",
    "|---|---|---|---|",
]
for ref in AYAT:
    a = next(x for x in data["ayat"] if x["ref"] == ref)
    doc.append(f"| `{ref}` | [Quran {ref}]({a['url']}) | | |")
for key, spec in HADITH.items():
    src = hadith[spec["source"]]
    label = src.get("reference") or src.get("quotedIn") or src["id"]
    grading = spec["grade"] or ""
    extra = []
    look = hadith.get("dorar:" + spec["source"])
    if look:
        primary = look.get("albaniVerdictForTirmidhi") or look.get("albaniVerdictPrimary")
        if primary:
            extra.append(f"[Albani]({primary['url']})")
        for v in look.get("otherVerdicts", []):
            if v["muhaddith"] == "الترمذي":
                extra.append(f"[Tirmidhi]({v['url']})")
    if spec["source"] == "tirmidhi:149":
        extra.append(f"[Tirmidhi's verdict on the 150 page]({hadith['tirmidhi:150']['url']})")
    if src.get("grade"):
        extra.append(f"sunnah.com: {src['grade']}")
    doc.append(f"| `{key}` | [{label}]({src['url']}) | {grading} | {', '.join(extra)} |")
doc += [
    "",
    "Notes:",
    "",
    "- `tirmidhi:3451` (hilal du'a): Tirmidhi grades it hasan gharib and Albani sahih by its combined routes (Sahih al-Tirmidhi 3451; al-Silsila al-Sahiha 1816). Ibn Hajar notes the chain itself is weak and Tirmidhi's hasan rests on supporting reports; Darussalam grades it da'if. The app shows all three.",
    "- `tirmidhi:149`: at-Tirmidhi's own verdict on the hadith of Ibn Abbas, \"حَسَنٌ صَحِيحٌ\", is printed after hadith 150.",
    "- `bukhari:969` is not used for the first ten days of Dhul Hijjah: its wording on sunnah.com does not state the ten days inside the Prophet's words. `tirmidhi:757` does.",
    "- `16:16`: the Diyanet meal renders 16:15 and 16:16 as one text; the app shows the part that belongs to 16:16.",
    "- `qatada` is a statement of Qatada that Bukhari places at the head of the chapter on stars (Book 59, Chapter 3); it is not a hadith and carries no grade.",
    "- `hattabi` is al-Khattabi's statement from Ma'alim al-Sunan as quoted in Awn al-Ma'bud (islamweb, vol. 10, p. 319); the quoting marker \"انتهى\" and the lead-in \"قال الخطابي\" are left out.",
    "- `uthaymin:ikiye` and `uthaymin:tesyir` are Ibn Uthaymin's division of the science of the stars from al-Qawl al-Mufid (2/5) as quoted in the dorar.net creed encyclopedia, section التنجيم.",
    "- White days are listed outside Ramadan and Dhul Hijjah, so they never fall on the 13th of Dhul Hijjah, a day of Tashriq.",
    "",
]
text = "\n".join(doc)
assert "\u2014" not in text
(REPO / "docs/islamic-sources.md").write_text(text)
