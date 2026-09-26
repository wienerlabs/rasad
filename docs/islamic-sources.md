# Islamic texts in Rasad

Every Arabic text shown in the app is cut from a verified source and checked by `IslamicTextsTest` against `app/src/test/resources/islamic-sources.tsv`. Turkish hadith and scholar translations are Rasad's own; the Quran meal is the Diyanet İşleri translation published on Tanzil.

Sources, fetched 2026-09-26:

- Quran: Tanzil simple script and the tr.diyanet meal via api.alquran.cloud, saved straight from the API responses. Letters and diacritics are used unchanged; on screen, pause marks are moved onto the preceding word.
- Hadith: sunnah.com pages read in a browser and compared byte for byte (SHA-256) with the live page.
- Gradings outside Bukhari and Muslim: the collection's own verdict and Albani's verdict as listed on dorar.net, plus the Darussalam grade shown on sunnah.com, wherever each exists.
- Scholar statements: quoted from the online sources named in the table, because the books themselves could not be opened; the app says "aktarımla" for these.

| Key | Source | Grading shown in the app | Grading sources |
|---|---|---|---|
| `2:144` | [Quran 2:144](https://api.alquran.cloud/v1/ayah/2:144/editions/quran-simple,tr.diyanet) | | |
| `2:185` | [Quran 2:185](https://api.alquran.cloud/v1/ayah/2:185/editions/quran-simple,tr.diyanet) | | |
| `2:189` | [Quran 2:189](https://api.alquran.cloud/v1/ayah/2:189/editions/quran-simple,tr.diyanet) | | |
| `4:103` | [Quran 4:103](https://api.alquran.cloud/v1/ayah/4:103/editions/quran-simple,tr.diyanet) | | |
| `6:97` | [Quran 6:97](https://api.alquran.cloud/v1/ayah/6:97/editions/quran-simple,tr.diyanet) | | |
| `9:36` | [Quran 9:36](https://api.alquran.cloud/v1/ayah/9:36/editions/quran-simple,tr.diyanet) | | |
| `10:5` | [Quran 10:5](https://api.alquran.cloud/v1/ayah/10:5/editions/quran-simple,tr.diyanet) | | |
| `16:16` | [Quran 16:16](https://api.alquran.cloud/v1/ayah/16:16/editions/quran-simple,tr.diyanet) | | |
| `53:49` | [Quran 53:49](https://api.alquran.cloud/v1/ayah/53:49/editions/quran-simple,tr.diyanet) | | |
| `67:5` | [Quran 67:5](https://api.alquran.cloud/v1/ayah/67:5/editions/quran-simple,tr.diyanet) | | |
| `bukhari:1909` | [Sahih al-Bukhari 1909](https://sunnah.com/bukhari:1909) | Sahih |  |
| `bukhari:846` | [Sahih al-Bukhari 846](https://sunnah.com/bukhari:846) | Sahih |  |
| `qatada` | [Sahih al-Bukhari, Book 59 (Beginning of Creation), Chapter 3: (About the) Stars (بَابٌ في النُّجُومِ), chapter introduction](https://sunnah.com/bukhari/59#C3.00) |  |  |
| `tirmidhi:3451` | [Jami` at-Tirmidhi 3451](https://sunnah.com/tirmidhi:3451) | Tirmizî: hasen garîb · Elbânî: yollarının toplamıyla sahih · Dârüsselâm: zayıf | [Albani](https://dorar.net/h/2GBWMtv6), [Tirmidhi](https://dorar.net/h/u2t2IZfy), sunnah.com: Da’if (Darussalam) |
| `bukhari:1044` | [Sahih al-Bukhari 1044](https://sunnah.com/bukhari:1044) | Sahih |  |
| `muslim:1094` | [Sahih Muslim 1094c](https://sunnah.com/muslim:1094c) | Sahih |  |
| `muslim:1162` | [Sahih Muslim 1162a](https://sunnah.com/muslim:1162a) | Sahih |  |
| `muslim:1134` | [Sahih Muslim 1134b](https://sunnah.com/muslim:1134b) | Sahih |  |
| `muslim:1164` | [Sahih Muslim 1164a](https://sunnah.com/muslim:1164a) | Sahih |  |
| `tirmidhi:761` | [Jami` at-Tirmidhi 761](https://sunnah.com/tirmidhi:761) | Tirmizî: hasen · Elbânî: hasen sahih · Dârüsselâm: hasen | [Albani](https://dorar.net/h/BoErL87Q), [Tirmidhi](https://dorar.net/h/ZyhfVOKd), sunnah.com: Hasan (Darussalam) |
| `tirmidhi:757` | [Jami` at-Tirmidhi 757](https://sunnah.com/tirmidhi:757) | Tirmizî: hasen sahih garîb · Dârüsselâm: sahih | sunnah.com: Sahih (Darussalam) |
| `bukhari:2017` | [Sahih al-Bukhari 2017](https://sunnah.com/bukhari:2017) | Sahih |  |
| `muslim:1141` | [Sahih Muslim 1141a](https://sunnah.com/muslim:1141a) | Sahih |  |
| `tirmidhi:149` | [Jami` at-Tirmidhi 149](https://sunnah.com/tirmidhi:149) | Tirmizî: hasen sahih · Elbânî: hasen sahih · Dârüsselâm: hasen | [Albani](https://dorar.net/h/ySO1XdB6), [Tirmidhi](https://dorar.net/h/vRRRAlHC), [Tirmidhi's verdict on the 150 page](https://sunnah.com/tirmidhi:150), sunnah.com: Hasan (Darussalam) |
| `tirmidhi:149:fecr` | [Jami` at-Tirmidhi 149](https://sunnah.com/tirmidhi:149) | Tirmizî: hasen sahih · Elbânî: hasen sahih · Dârüsselâm: hasen | [Albani](https://dorar.net/h/ySO1XdB6), [Tirmidhi](https://dorar.net/h/vRRRAlHC), [Tirmidhi's verdict on the 150 page](https://sunnah.com/tirmidhi:150), sunnah.com: Hasan (Darussalam) |
| `abudawud:3905` | [Sunan Abi Dawud 3905](https://sunnah.com/abudawud:3905) | Elbânî: hasen | [Albani](https://dorar.net/h/DFQhU4qL), sunnah.com: Hasan (Al-Albani) |
| `muslim:934` | [Sahih Muslim 934](https://sunnah.com/muslim:934) | Sahih |  |
| `hattabi` | [عون المعبود, الجزء 10, ص 319 (islamweb library page titled: عون المعبود - كتاب الطب - باب في النجوم- الجزء رقم10)](https://www.islamweb.net/ar/library/content/55/6781/%D8%A8%D8%A7%D8%A8-%D9%81%D9%8A-%D8%A7%D9%84%D9%86%D8%AC%D9%88%D9%85) |  |  |
| `uthaymin:ikiye` | [الدرر السنية, الموسوعة العقدية, الفرع الثالث: التنجيم](https://dorar.net/aqeeda/2958/%D8%A7%D9%84%D9%81%D8%B1%D8%B9-%D8%A7%D9%84%D8%AB%D8%A7%D9%84%D8%AB-%D8%A7%D9%84%D8%AA%D9%86%D8%AC%D9%8A%D9%85) |  |  |
| `uthaymin:tesyir` | [الدرر السنية, الموسوعة العقدية, الفرع الثالث: التنجيم](https://dorar.net/aqeeda/2958/%D8%A7%D9%84%D9%81%D8%B1%D8%B9-%D8%A7%D9%84%D8%AB%D8%A7%D9%84%D8%AB-%D8%A7%D9%84%D8%AA%D9%86%D8%AC%D9%8A%D9%85) |  |  |
| `abudawud:1134` | [Sunan Abi Dawud 1134](https://sunnah.com/abudawud:1134) | Elbânî: sahih | sunnah.com: Sahih (Al-Albani) |
| `bukhari:1990` | [Sahih al-Bukhari 1990](https://sunnah.com/bukhari:1990) | Sahih |  |
| `bukhari:3197` | [Sahih al-Bukhari 3197](https://sunnah.com/bukhari:3197) | Sahih |  |

Notes:

- `tirmidhi:3451` (hilal du'a): Tirmidhi grades it hasan gharib and Albani sahih by its combined routes (Sahih al-Tirmidhi 3451; al-Silsila al-Sahiha 1816). Ibn Hajar notes the chain itself is weak and Tirmidhi's hasan rests on supporting reports; Darussalam grades it da'if. The app shows all three.
- `tirmidhi:149`: at-Tirmidhi's own verdict on the hadith of Ibn Abbas, "حَسَنٌ صَحِيحٌ", is printed after hadith 150.
- `bukhari:969` is not used for the first ten days of Dhul Hijjah: its wording on sunnah.com does not state the ten days inside the Prophet's words. `tirmidhi:757` does.
- `16:16`: the Diyanet meal renders 16:15 and 16:16 as one text; the app shows the part that belongs to 16:16.
- `qatada` is a statement of Qatada that Bukhari places at the head of the chapter on stars (Book 59, Chapter 3); it is not a hadith and carries no grade.
- `hattabi` is al-Khattabi's statement from Ma'alim al-Sunan as quoted in Awn al-Ma'bud (islamweb, vol. 10, p. 319); the quoting marker "انتهى" and the lead-in "قال الخطابي" are left out.
- `uthaymin:ikiye` and `uthaymin:tesyir` are Ibn Uthaymin's division of the science of the stars from al-Qawl al-Mufid (2/5) as quoted in the dorar.net creed encyclopedia, section التنجيم.
- White days are listed outside Ramadan and Dhul Hijjah, so they never fall on the 13th of Dhul Hijjah, a day of Tashriq.
