package xyz.wienerlabs.rasad.sky

data class StarLore(
    val original: String,
    val transliteration: String,
    val meaning: String,
    val story: String,
    val language: String = "Arapça",
)

object StarLoreBook {
    private fun arabic(original: String, transliteration: String, meaning: String, story: String) =
        StarLore(original, transliteration, meaning, story)

    private val entries: Map<String, StarLore> = mapOf(
        "Sirius" to arabic(
            "الشعرى اليمانية", "eş-Şi'râ el-Yemâniye", "Yemen tarafındaki Şi'râ",
            "Kur'an'da özel adıyla anılan yıldızdır (Necm 53:49, aşağıda). Samanyolu'nu geçtiği için eş-Şi'râ el-Abûr, yani “geçen Şi'râ” diye de anılırdı. Batıdaki adı Yunanca “kavurucu” demektir.",
        ),
        "Procyon" to arabic(
            "الشعرى الغميصاء", "eş-Şi'râ el-Gumeysâ", "gözü yaşlı Şi'râ",
            "Câhiliye Araplarının anlatısına göre iki kız kardeşten biri Samanyolu'nu geçip gitti, bu ise geride kalıp ağlamaktan gözleri bulanıklaştı. Procyon Yunanca “köpekten önce” demektir; Sirius'tan az önce doğar.",
        ),
        "Canopus" to arabic(
            "سهيل", "Süheyl", "anlamı kesin bilinmeyen eski bir özel ad",
            "Arap şiirinde güneyin ve Yemen'in simgesidir; Süheyl'in doğuşu yaz sıcağının kırılacağına işaret sayılırdı. Türkiye'nin en güney kıyılarından kış gecelerinde ufkun hemen üstünde kısa süre seçilebilir.",
        ),
        "Suhail" to arabic(
            "سهيل", "Süheyl", "güneyin parlak yıldızı",
            "Süheyl adı eskiden birçok güney yıldızına verilirdi. Bugün resmî olarak Yelken takımyıldızındaki bu yıldıza aittir; asıl Süheyl ise Canopus'tur.",
        ),
        "Arcturus" to arabic(
            "السماك الرامح", "es-Simâk er-Râmih", "mızraklı Simâk",
            "Araplar Arcturus ile Spica'yı iki Simâk olarak görürdü. Önündeki sönük yıldızlar mızrağı sayıldığı için bu yıldıza “mızraklı” dendi. Batıdaki adı Yunanca “ayının bekçisi” anlamına gelir.",
        ),
        "Spica" to arabic(
            "السماك الأعزل", "es-Simâk el-A'zel", "silahsız Simâk",
            "Mızraklı Simâk'ın (Arcturus) karşısında silahsız olanıdır. Spica Latince “başak” demektir; Başak'ın elindeki buğday başağını temsil eder.",
        ),
        "Vega" to arabic(
            "النسر الواقع", "en-Nesr el-Vâki'", "konan kartal",
            "Kanatlarını kapatıp yere inen bir kartal olarak düşünülürdü; iki yanındaki sönük yıldızlar katlanmış kanatlarıdır. Vega adı, Latince çevirilerde “vâki'” kelimesinin bozulmasından doğdu.",
        ),
        "Altair" to arabic(
            "النسر الطائر", "en-Nesr et-Tâir", "uçan kartal",
            "Konan kartala (Vega) karşılık, iki yanındaki yıldızlarla kanat açmış uçan bir kartaldır. Altair adı doğrudan “et-tâir” kelimesinden gelir.",
        ),
        "Capella" to arabic(
            "العيوق", "el-Ayyûk", "engelleyen",
            "Câhiliye Araplarının anlatısına göre Deberân (Aldebaran) Süreyya'ya kavuşmak ister, el-Ayyûk ise yolunu keserdi. Capella Latincede “küçük keçi” anlamına gelir.",
        ),
        "Rigel" to arabic(
            "رجل الجوزاء", "Ricl el-Cevzâ", "Cevzâ'nın ayağı",
            "Araplar Avcı takımyıldızını Cevzâ adlı dev bir figür olarak görürdü; Rigel onun sol ayağıdır. Bugünkü ad bu tamlamanın ilk kelimesinden kalmıştır.",
        ),
        "Betelgeuse" to arabic(
            "يد الجوزاء", "Yed el-Cevzâ", "Cevzâ'nın eli",
            "Orta Çağ Latin çevirmenleri Arapça yazıdaki “yed” kelimesini “bed” diye okudu; Bedalgeuze zamanla Betelgeuse'e dönüştü. Kızıl bir üstdevdir ve bir gün süpernova olarak patlaması beklenir.",
        ),
        "Aldebaran" to arabic(
            "الدبران", "ed-Deberân", "arkadan gelen, izleyen",
            "Süreyya'yı (Ülker) her gece gökyüzünde arkadan izlediği için bu adı aldı. Boğa'nın kızıl gözü olarak bilinir.",
        ),
        "Antares" to arabic(
            "قلب العقرب", "Kalb el-Akreb", "akrebin kalbi",
            "Akrep takımyıldızının kızıl kalbidir. Batıdaki adı Yunanca “Ares'in rakibi” demektir; rengi Mars'la karıştırılacak kadar kırmızıdır.",
        ),
        "Regulus" to arabic(
            "قلب الأسد", "Kalb el-Esed", "aslanın kalbi",
            "Aslan takımyıldızının kalbinde parlar. Regulus Latince “küçük kral” anlamına gelir.",
        ),
        "Denebola" to arabic(
            "ذنب الأسد", "Zeneb el-Esed", "aslanın kuyruğu",
            "Aslan'ın kuyruk ucundadır. Batı dillerindeki adı bu Arapça tamlamanın kısalmış hâlidir.",
        ),
        "Algieba" to arabic(
            "الجبهة", "el-Cebhe", "alın",
            "Aslan'ın alnında yer alan yıldızlardandır. Küçük bir teleskopla altın renkli iki yıldıza ayrılır.",
        ),
        "Fomalhaut" to arabic(
            "فم الحوت", "Fem el-Hût", "balığın ağzı",
            "Güney Balığı'nın ağzında durur. Sonbahar akşamları güney ufkunda çevresinde parlak komşu olmadan tek başına parlar.",
        ),
        "Diphda" to arabic(
            "الضفدع الثاني", "ed-Dıfda' es-Sânî", "ikinci kurbağa",
            "Araplar Fomalhaut'u “birinci kurbağa”, bu yıldızı “ikinci kurbağa” diye anardı. Balina takımyıldızının en parlak yıldızıdır.",
        ),
        "Deneb" to arabic(
            "ذنب الدجاجة", "Zeneb ed-Decâce", "tavuğun kuyruğu",
            "Araplar Kuğu takımyıldızını uçan bir tavuk olarak görürdü; Deneb onun kuyruğu, Sadr göğsü, Albireo gagasıdır.",
        ),
        "Sadr" to arabic(
            "صدر الدجاجة", "Sadr ed-Decâce", "tavuğun göğsü",
            "Kuğu'nun, yani Arapların tavuğunun göğsündedir. Samanyolu'nun en yoğun bölgelerinden birinin tam ortasında durur.",
        ),
        "Albireo" to arabic(
            "منقار الدجاجة", "Minkâr ed-Decâce", "tavuğun gagası",
            "Arapça adı “tavuğun gagası”dır; Albireo adı ise Latince çeviriler arasında oluşmuş bir yanlış okumadır. Küçük bir dürbünde altın ve mavi iki yıldıza ayrılır.",
        ),
        "Pollux" to arabic(
            "رأس التوأم المؤخر", "Re'sü't-Tev'em el-Muahhar", "arkadaki ikizin başı",
            "Castor ile birlikte İkizler'in iki başını oluşturur. Arapça adı ikizlerden arkada kalanı anlatır.",
        ),
        "Castor" to arabic(
            "رأس التوأم المقدم", "Re'sü't-Tev'em el-Mukaddem", "öndeki ikizin başı",
            "İkizler'in öndeki başıdır. Teleskopla bakıldığında aslında altı yıldızdan oluşan bir sistem olduğu anlaşılır.",
        ),
        "Adhara" to arabic(
            "العذارى", "el-Azârâ", "bakireler",
            "Büyük Köpek'in arka ayağındaki yıldız grubuna verilen addan gelir.",
        ),
        "Aludra" to arabic(
            "العذرة", "el-Uzre", "bekaret",
            "Büyük Köpek'in kuyruğundaki bu yıldızın adı, yakınındaki el-Azârâ grubuyla aynı kökten gelir.",
        ),
        "Wezen" to arabic(
            "الوزن", "el-Vezn", "ağırlık",
            "Ufkun hemen üstünde ağır ağır yükseldiği için bu adı aldığı anlatılır.",
        ),
        "Mirzam" to arabic(
            "المرزم", "el-Mirzem", "haberci",
            "Sirius'tan hemen önce doğarak onun gelişini haber verir.",
        ),
        "Shaula" to arabic(
            "الشولة", "eş-Şevle", "kalkık kuyruk",
            "Akrebin sokmaya hazır, havaya kalkmış kuyruk ucudur.",
        ),
        "Lesath" to arabic(
            "اللسعة", "el-Les'a", "sokma",
            "Şevle'nin hemen yanında akrebin iğnesini tamamlar.",
        ),
        "Acrab" to arabic(
            "العقرب", "el-Akreb", "akrep",
            "Adını doğrudan takımyıldızın Arapçasından alır.",
        ),
        "Dschubba" to arabic(
            "الجبهة", "el-Cebhe", "alın",
            "Akrebin alnındaki yıldızlardandır; adı Almanca yazımla Batı'ya geçmiştir.",
        ),
        "Sabik" to arabic(
            "السابق", "es-Sâbık", "önde giden",
            "Yılancı takımyıldızında, bir yıldız çiftinin önde gideni olarak anılırdı.",
        ),
        "Elnath" to arabic(
            "النطح", "en-Nath", "boynuzla vuran",
            "Boğa'nın boynuz ucunda yer alır.",
        ),
        "Alnilam" to arabic(
            "النظام", "en-Nizâm", "inci dizisi",
            "Avcı'nın kuşağındaki üç yıldızın ortasıdır; Araplar bu üçlüyü ipe dizilmiş inciler olarak görürdü.",
        ),
        "Alnitak" to arabic(
            "النطاق", "en-Nitâk", "kuşak",
            "Avcı'nın kuşağının doğu ucundadır. Hemen yanında ünlü At Başı Bulutsusu bulunur.",
        ),
        "Mintaka" to arabic(
            "المنطقة", "el-Mintaka", "kuşak, bel bağı",
            "Kuşağın batı ucundaki yıldızdır ve gök ekvatorunun neredeyse tam üstündedir.",
        ),
        "Saiph" to arabic(
            "سيف الجبار", "Seyf el-Cebbâr", "devin kılıcı",
            "Ad aslında Avcı'nın kılıcını oluşturan bölgeye aitti; zamanla bu yıldıza geçti.",
        ),
        "Meissa" to arabic(
            "الميسان", "el-Meysân", "parıldayan",
            "Asıl olarak İkizler'deki bir yıldızın adıydı; bir karışıklıkla Avcı'nın başındaki bu yıldıza geçti.",
        ),
        "Cursa" to arabic(
            "كرسي الجوزاء", "Kürsî el-Cevzâ", "Cevzâ'nın kürsüsü",
            "Rigel'in yakınındaki yıldızlar Cevzâ'nın ayak taburesi olarak görülürdü.",
        ),
        "Zaurak" to arabic(
            "الزورق", "ez-Zevrak", "kayık",
            "Irmak takımyıldızında, nehirde yüzen bir kayık olarak düşünülürdü.",
        ),
        "Achernar" to arabic(
            "آخر النهر", "Âhir en-Nehr", "ırmağın sonu",
            "Irmak takımyıldızının güneydeki son parlak yıldızıdır. Türkiye'den görülemez, ufkun altında kalır.",
        ),
        "Dubhe" to arabic(
            "ظهر الدب الأكبر", "Zahr ed-Dubb el-Ekber", "büyük ayının sırtı",
            "Adı bu tamlamadaki “dubb” (ayı) kelimesinden kalmıştır. Merak ile birlikte Kutup Yıldızı'nı gösteren iki işaretçi yıldızdan biridir.",
        ),
        "Merak" to arabic(
            "المراق", "el-Merâkk", "karın altı, böğür",
            "Büyük Ayı'nın böğrüdür. Dubhe'den Merak'a çizilen doğru uzatılınca Kutup Yıldızı'na varılır.",
        ),
        "Phecda" to arabic(
            "فخذ الدب", "Fahiz ed-Dubb", "ayının uyluğu",
            "Büyük Ayı'nın arka bacağının üst kısmını temsil eder.",
        ),
        "Megrez" to arabic(
            "مغرز الذنب", "Magriz ez-Zeneb", "kuyruğun dibi",
            "Kuyruğun gövdeye bağlandığı yerdeki, yedilinin en sönük yıldızıdır.",
        ),
        "Alioth" to arabic(
            "الألية", "el-Elye", "yağlı kuyruk",
            "Adın kökeni tartışmalıdır; en yaygın açıklamaya göre koyunun yağlı kuyruğunu anlatan el-elye kelimesinden gelir.",
        ),
        "Mizar" to arabic(
            "المئزر", "el-Mi'zer", "peştamal, bel örtüsü",
            "Yanındaki sönük Alcor ile eski bir göz testidir. Araplar arasında “Ben ona Süha'yı gösteriyorum, o bana Ay'ı gösteriyor” sözü yaygındı; Süha, Alcor'un adıdır.",
        ),
        "Alcor" to arabic(
            "السها", "es-Süha", "gözden kaçan",
            "Mizar'ın hemen yanındaki bu sönük yıldızı seçebilmek keskin gözün işareti sayılırdı.",
        ),
        "Alkaid" to arabic(
            "قائد بنات نعش", "Kâidu Benâti Na'ş", "Na'ş kızlarının önderi",
            "Araplar Büyük Ayı'nın dörtgenini bir tabut (na'ş), kuyruktaki üç yıldızı da onu izleyen yaslı kızlar olarak görürdü. Alkaid bu kafilenin başındadır.",
        ),
        "Talitha" to arabic(
            "القفزة الثالثة", "el-Kafzetü's-Sâlise", "üçüncü sıçrayış",
            "Büyük Ayı'nın ayaklarındaki üç yıldız çifti, bir ceylanın bıraktığı üç sıçrayış izi olarak görülürdü. Talitha üçüncü sıçrayıştır.",
        ),
        "Tania Borealis" to arabic(
            "القفزة الثانية", "el-Kafzetü's-Sâniye", "ikinci sıçrayış",
            "Ceylanın ikinci sıçrayışını işaretleyen yıldız çiftinin kuzeydekidir.",
        ),
        "Tania Australis" to arabic(
            "القفزة الثانية", "el-Kafzetü's-Sâniye", "ikinci sıçrayış",
            "Ceylanın ikinci sıçrayışını işaretleyen yıldız çiftinin güneydekidir.",
        ),
        "Alula Borealis" to arabic(
            "القفزة الأولى", "el-Kafzetü'l-Ûlâ", "birinci sıçrayış",
            "Ceylanın ilk sıçrayışını işaretleyen yıldız çiftinin kuzeydekidir.",
        ),
        "Alula Australis" to arabic(
            "القفزة الأولى", "el-Kafzetü'l-Ûlâ", "birinci sıçrayış",
            "Ceylanın ilk sıçrayışını işaretleyen yıldız çiftinin güneydekidir. Yörüngesi ilk hesaplanan çift yıldızdır.",
        ),
        "Polaris" to arabic(
            "الجدي", "el-Cedy", "oğlak",
            "Türk boylarında Demirkazık ya da Altınkazık diye bilinir; gök bu kazığa bağlı dönen bir çadır gibi düşünülürdü. Yön bulmada ve halk astronomisinde kıbleyi kabaca belirlemede kullanılırdı.",
        ),
        "Kochab" to arabic(
            "الكوكب الشمالي", "el-Kevkeb eş-Şimâlî", "kuzeyin yıldızı",
            "Yaklaşık üç bin yıl önce kutba Polaris'ten daha yakındı ve kuzeyin yıldızı sayılırdı.",
        ),
        "Thuban" to arabic(
            "الثعبان", "es-Su'bân", "büyük yılan",
            "Yaklaşık 4.700 yıl önce kutup yıldızıydı. Ejderha takımyıldızının kıvrımları arasında yer alır.",
        ),
        "Eltanin" to arabic(
            "التنين", "et-Tinnîn", "ejderha",
            "Ejderha'nın başındaki en parlak yıldızdır. Işık sapmasının keşfi bu yıldızın gözlemleriyle yapıldı.",
        ),
        "Rastaban" to arabic(
            "رأس الثعبان", "Re'sü's-Su'bân", "yılanın başı",
            "Ejderha'nın başını oluşturan yıldız dörtgenindedir.",
        ),
        "Alpheratz" to arabic(
            "سرة الفرس", "Surretü'l-Feres", "atın göbeği",
            "Eskiden Kanatlı At'a ait sayılırdı; bugün Andromeda'nın başı kabul edilir.",
        ),
        "Mirach" to arabic(
            "المئزر", "el-Mi'zer", "peştamal",
            "Andromeda'nın beline sarılı örtüdeki yıldızdır. Andromeda Gökadası'nı bulmak için bu yıldızdan yukarı doğru gidilir.",
        ),
        "Almach" to arabic(
            "عناق الأرض", "Anâku'l-Arz", "karakulak",
            "Adı bir yaban kedisi olan karakulaktan gelir. Teleskopta turuncu ve mavi iki yıldıza ayrılır.",
        ),
        "Hamal" to arabic(
            "الحمل", "el-Hamel", "koç, kuzu",
            "Koç takımyıldızının en parlak yıldızıdır ve adını takımyıldızın Arapçasından alır.",
        ),
        "Sheratan" to arabic(
            "الشرطان", "eş-Şeretân", "iki işaret",
            "Yaklaşık iki bin yıl önce ilkbahar noktasını işaret eden yıldız çiftindendi.",
        ),
        "Algol" to arabic(
            "رأس الغول", "Re'sü'l-Gûl", "gulyabaninin başı",
            "Parlaklığı 2 gün 21 saatte bir birkaç saatliğine belirgin biçimde düşer. Birbirini örten iki yıldızın yarattığı bu “göz kırpma”, korkutucu adıyla birlikte anılır.",
        ),
        "Mirfak" to arabic(
            "مرفق الثريا", "Mirfaku's-Süreyyâ", "Süreyya'nın dirseği",
            "Perseus'un en parlak yıldızıdır; Süreyya'ya uzanan bir kolun dirseği olarak görülürdü.",
        ),
        "Menkib" to arabic(
            "منكب الثريا", "Menkibü's-Süreyyâ", "Süreyya'nın omzu",
            "Mirfak ile aynı imgenin parçasıdır: Süreyya'ya uzanan kolun omzu.",
        ),
        "Alcyone" to arabic(
            "الثريا", "es-Süreyyâ", "küçük bolluk",
            "Ülker (Süreyya) yıldız kümesinin en parlak üyesidir. Araplar kümeye es-Süreyyâ, Türkler Ülker der; iyi bir gecede gözle altı ya da yedi yıldız seçilir.",
        ),
        "Ain" to arabic(
            "عين الثور", "Aynü's-Sevr", "boğanın gözü",
            "Boğa'nın başındaki V biçimli Hyades kümesinin bir üyesidir.",
        ),
        "Menkar" to arabic(
            "المنخر", "el-Minhar", "burun deliği",
            "Balina takımyıldızındaki deniz canavarının burnudur.",
        ),
        "Enif" to arabic(
            "أنف الفرس", "Enfü'l-Feres", "atın burnu",
            "Kanatlı At'ın burnunda parlar.",
        ),
        "Markab" to arabic(
            "مركب", "Merkeb", "binek, eyer",
            "Kanatlı At'ın büyük dörtgeninin köşelerinden biridir.",
        ),
        "Algenib" to arabic(
            "الجنب", "el-Cenb", "yan, böğür",
            "Kanatlı At'ın böğrünü temsil eder; Büyük Dörtgen'in güneydoğu köşesidir.",
        ),
        "Alderamin" to arabic(
            "الذراع اليمين", "ez-Zirâü'l-Yemîn", "sağ kol",
            "Kefeus'un sağ kolundadır. Yaklaşık 5.500 yıl sonra kutup yıldızı olacaktır.",
        ),
        "Schedar" to arabic(
            "صدر ذات الكرسي", "Sadru Zâti'l-Kürsî", "kürsüdeki kadının göğsü",
            "Kassiopeia, kürsüde oturan bir kadın olarak görülürdü; Schedar onun göğsüdür.",
        ),
        "Caph" to arabic(
            "الكف الخضيب", "el-Keffü'l-Hadîb", "kınalı el",
            "Araplar Kassiopeia'nın yıldızlarını parmakları kınalı bir el olarak da görürdü.",
        ),
        "Ruchbah" to arabic(
            "ركبة ذات الكرسي", "Rukbetü Zâti'l-Kürsî", "kürsüdeki kadının dizi",
            "Kassiopeia'nın W biçimindeki yıldızlarından biridir.",
        ),
        "Rasalhague" to arabic(
            "رأس الحواء", "Re'sü'l-Havvâ", "yılancının başı",
            "Yılancı takımyıldızının başında parlar.",
        ),
        "Rasalgethi" to arabic(
            "رأس الجاثي", "Re'sü'l-Câsî", "diz çökenin başı",
            "Herkül, Araplarca diz çökmüş bir adam olarak görülürdü; bu yıldız onun başıdır.",
        ),
        "Alphecca" to arabic(
            "نير الفكة", "Neyyirü'l-Fekke", "kırık halkanın parlağı",
            "Kuzey Tacı'nın açık halkası kırık bir çember olarak görülürdü; bu yıldız onun en parlağıdır.",
        ),
        "Unukalhai" to arabic(
            "عنق الحية", "Unuku'l-Hayye", "yılanın boynu",
            "Yılan takımyıldızının boynunda yer alır.",
        ),
        "Zubenelgenubi" to arabic(
            "الزبانى الجنوبي", "ez-Zubânâ el-Cenûbî", "güney kıskacı",
            "Terazi'nin yıldızları eskiden Akrep'in kıskaçları sayılırdı; bu yıldız güneydeki kıskaçtır.",
        ),
        "Zubeneschamali" to arabic(
            "الزبانى الشمالي", "ez-Zubânâ eş-Şimâlî", "kuzey kıskacı",
            "Akrebin kuzeydeki kıskacıdır. Bazı gözlemciler rengini hafif yeşilimsi görür.",
        ),
        "Alphard" to arabic(
            "الفرد", "el-Ferd", "yalnız olan",
            "Çevresinde parlak yıldız bulunmadığı için Suyılanı'nın ortasında tek başına parlar.",
        ),
        "Kaus Australis" to StarLore(
            "القوس", "el-Kavs", "yay",
            "Arapça kavs (yay) ile Latince australis (güney) kelimelerinden oluşan melez bir addır. Yay takımyıldızının en parlak yıldızıdır.",
            "Arapça ve Latince",
        ),
        "Kaus Media" to StarLore(
            "القوس", "el-Kavs", "yay",
            "Yayın ortasındaki yıldızdır; adı Arapça ve Latince kelimelerden oluşur.",
            "Arapça ve Latince",
        ),
        "Kaus Borealis" to StarLore(
            "القوس", "el-Kavs", "yay",
            "Yayın kuzey ucundaki yıldızdır; adı Arapça ve Latince kelimelerden oluşur.",
            "Arapça ve Latince",
        ),
        "Albaldah" to arabic(
            "البلدة", "el-Belde", "şehir",
            "Ay'ın konaklarından birinin adıdır; yıldızsız boş bir alana verilen bu ad sonradan yakındaki yıldıza geçti.",
        ),
        "Rukbat" to arabic(
            "ركبة الرامي", "Rukbetü'r-Râmî", "okçunun dizi",
            "Yay takımyıldızındaki okçunun dizini temsil eder.",
        ),
        "Nashira" to arabic(
            "سعد ناشرة", "Sa'du Nâşira", "müjdeleyenin sa'dı",
            "Oğlak'ta yer alır. Adı, Câhiliye Araplarının bazı yıldız gruplarına “sa'd” (uğur) deyip onlara talih bağladığı eski adlandırmadan kalmıştır. İslam uğuru da uğursuzluğu da yıldızlara bağlamayı reddeder; bu ad bugün yalnızca bir isim olarak yaşar.",
        ),
        "Deneb Algedi" to arabic(
            "ذنب الجدي", "Zenebü'l-Cedy", "oğlağın kuyruğu",
            "Oğlak takımyıldızının kuyruğundadır.",
        ),
        "Algedi" to arabic(
            "الجدي", "el-Cedy", "oğlak",
            "Adını takımyıldızın Arapçasından alır; çıplak gözle çift görünen bir yıldızdır.",
        ),
        "Dabih" to arabic(
            "الذابح", "ez-Zâbih", "kesen",
            "Adı, eski Arapların Sa'dü'z-Zâbih dediği Ay konağından gelir. Ay konakları, Ay'ın her gece hangi yıldızların yanında olduğunu izleyerek ayı ve mevsimi takip etmeye yarardı; onlara bağlanan uğur ve yağmur inancını ise İslam reddeder.",
        ),
        "Sadalmelik" to arabic(
            "سعد الملك", "Sa'dü'l-Melik", "kralın sa'dı",
            "Kova'nın en parlak yıldızlarından biridir. Adı, eski Arapların “sa'd” (uğur) diye andığı yıldız gruplarından birinden gelir; yıldızlara uğur bağlamak İslam'da kabul görmez.",
        ),
        "Sadalsuud" to arabic(
            "سعد السعود", "Sa'dü's-Su'ûd", "sa'dların sa'dı",
            "Kova'da yer alır. Güneş bu yıldızlara kış sonunda yaklaşır. Câhiliye Arapları yağmuru Ay konaklarının doğup batmasına (nev') bağlardı; aşağıdaki hadis-i kudsî bunu reddeder.",
        ),
        "Sadachbia" to arabic(
            "سعد الأخبية", "Sa'dü'l-Ahbiye", "çadırların sa'dı",
            "Bu yıldızların doğuşu, çölde çadır kurma mevsiminin geldiğini haber veren bir takvim işaretiydi. Adındaki “sa'd” eski uğur adlandırmasından kalmadır; İslam yıldızlara talih bağlamayı reddeder.",
        ),
        "Skat" to arabic(
            "الساق", "es-Sâk", "incik",
            "Kova'yı taşıyan figürün bacağındadır.",
        ),
        "Fumalsamakah" to arabic(
            "فم السمكة", "Femü's-Semeke", "balığın ağzı",
            "Balıklar takımyıldızındaki batı balığının ağzındadır.",
        ),
        "Alrescha" to arabic(
            "الرشاء", "er-Rişâ", "ip",
            "İki balığı birbirine bağlayan ipin düğümündedir.",
        ),
        "Menkalinan" to arabic(
            "منكب ذي العنان", "Menkibü Zi'l-İnân", "dizgin tutanın omzu",
            "Arabacı takımyıldızındaki sürücünün omzudur.",
        ),
        "Alhena" to arabic(
            "الهنعة", "el-Hen'a", "damga",
            "Develerin boynuna vurulan damgayı anlatan bir Ay konağı adıdır.",
        ),
        "Wasat" to arabic(
            "وسط", "Vasat", "orta",
            "İkizler'in tam ortasına yakın durur.",
        ),
        "Mebsuta" to arabic(
            "المبسوطة", "el-Mebsûta", "uzatılmış",
            "Eski Arap gökyüzündeki büyük aslanın ileri uzattığı pençesinin parçası sayılırdı.",
        ),
        "Gienah" to arabic(
            "جناح الغراب", "Cenâhu'l-Gurâb", "karganın kanadı",
            "Karga takımyıldızının kanadını temsil eder.",
        ),
        "Algorab" to arabic(
            "الغراب", "el-Gurâb", "karga",
            "Adını doğrudan takımyıldızın Arapçasından alır.",
        ),
        "Minkar" to arabic(
            "المنخر", "el-Minhar", "burun",
            "Karganın gagasının dibindeki yıldızdır.",
        ),
        "Alkes" to arabic(
            "الكأس", "el-Ke's", "kâse, kadeh",
            "Kupa takımyıldızının tabanında yer alır.",
        ),
        "Zavijava" to arabic(
            "زاوية العواء", "Zâviyetü'l-Avvâ", "havlayanın köşesi",
            "Başak'ın yıldızlarının oluşturduğu açıya verilen bir Ay konağı adından gelir.",
        ),
        "Zaniah" to arabic(
            "الزاوية", "ez-Zâviye", "köşe",
            "Başak'taki yıldız açısının köşesinde durur.",
        ),
        "Izar" to arabic(
            "الإزار", "el-İzâr", "örtü, peştamal",
            "Çoban takımyıldızındaki figürün beline sarılı örtüdür. Teleskopta turuncu ve mavi iki yıldıza ayrılır.",
        ),
        "Muphrid" to arabic(
            "مفرد الرامح", "Müfredü'r-Râmih", "mızraklının yalnızı",
            "Mızraklı Simâk'ın (Arcturus) yakınında tek başına duran yıldızdır.",
        ),
        "Nekkar" to arabic(
            "البقار", "el-Bakkâr", "sığırtmaç",
            "Çoban takımyıldızının başındaki yıldızdır.",
        ),
        "Arneb" to arabic(
            "الأرنب", "el-Erneb", "tavşan",
            "Adını doğrudan takımyıldızın Arapçasından alır.",
        ),
        "Nihal" to arabic(
            "النهال", "en-Nihâl", "susuzluğunu gideren develer",
            "Tavşan'daki bu yıldızlar su içen develer olarak görülürdü.",
        ),
        "Phact" to arabic(
            "الفاختة", "el-Fâhite", "üveyik",
            "Güvercin takımyıldızının en parlak yıldızıdır; adı bir kuş türünü anlatır.",
        ),
        "Sheliak" to arabic(
            "الشلياق", "eş-Şelyâk", "lir, çalgı",
            "Çalgı takımyıldızının Arapça adından gelir. Parlaklığı yaklaşık 13 günde bir değişir.",
        ),
        "Sulafat" to arabic(
            "السلحفاة", "es-Sulahfât", "kaplumbağa",
            "Eski lirlerin gövdesi kaplumbağa kabuğundan yapıldığı için Çalgı'nın bir yıldızına bu ad verildi.",
        ),
        "Kitalpha" to arabic(
            "قطعة الفرس", "Kıt'atü'l-Feres", "atın parçası",
            "Tay takımyıldızının en parlak yıldızıdır; Kanatlı At'ın bir parçası sayılırdı.",
        ),
        "Chertan" to arabic(
            "الخرتان", "el-Hurtân", "iki küçük kaburga",
            "Aslan'ın sırtındaki bir çift yıldızın Ay konağı adından gelir.",
        ),
        "Alnair" to arabic(
            "النير", "en-Neyyir", "parlak olan",
            "Turna takımyıldızının en parlak yıldızıdır; adı “balığın kuyruğundaki parlak” tamlamasından kısalmıştır.",
        ),
        "Ankaa" to arabic(
            "العنقاء", "el-Ankâ", "Zümrüdüanka",
            "Anka takımyıldızının en parlak yıldızına modern dönemde verilen Arapça addır.",
        ),
        "Rigil Kentaurus" to arabic(
            "رجل قنطورس", "Riclü Kantûrus", "Kentaur'un ayağı",
            "Güneş'e en yakın yıldız sistemidir. Türkiye'den görülemez; her zaman ufkun altında kalır.",
        ),
        "Tarazed" to StarLore(
            "شاهين ترازو", "şâhîn-i terâzû", "terazinin şahini",
            "Adı Farsçadır ve Kartal takımyıldızındaki üç yıldızlık bir diziyi terazi kolu gibi tutan şahini anlatır.",
            "Farsça",
        ),
        "Alshain" to StarLore(
            "شاهين", "şâhîn", "şahin",
            "Adı Farsça “şahin” kelimesinden gelir; Altair'in iki yanındaki yıldızlardan biridir.",
            "Farsça",
        ),
        "Sualocin" to StarLore(
            "Nicolaus", "Sualocin", "Nicolaus'un tersten yazılışı",
            "Palermo Gözlemevi'nde çalışan Niccolò Cacciatore, Latince adı Nicolaus Venator'u tersten yazarak 1814'te yıldız kataloğuna gizlice yerleştirdi.",
            "Latince",
        ),
        "Rotanev" to StarLore(
            "Venator", "Rotanev", "Venator'un tersten yazılışı",
            "Sualocin'in eşidir: Niccolò Cacciatore'nin soyadının Latincesi olan Venator tersten okunur.",
            "Latince",
        ),
    )

    fun forStar(name: String?): StarLore? = name?.let { entries[it] }

    val names: Set<String> get() = entries.keys
}
