package com.example.kucharz2.data

import java.text.Normalizer

object StandardIngredientCatalog {
    private val rawIngredients = """
        jajka
        mleko
        cebula
        masło
        mąka
        makaron
        ser żółty
        cukier
        czosnek
        pomidor
        ziemniaki
        ryż
        papryka w proszku
        keczup
        proszek do pieczenia
        oregano
        bazylia
        majonez
        śmietana
        chleb
        liść laurowy
        majeranek
        cynamon
        olej rzepakowy
        musztarda
        ogórek
        marchewka
        spaghetti
        oliwa z oliwek
        pierś z kurczaka
        soda oczyszczona
        tymianek
        ziele angielskie
        zioła prowansalskie
        miód
        czosnek w proszku
        olej słonecznikowy
        jogurt
        płatki owsiane
        przyprawa do kurczaka
        papryka
        pieczarki
        por
        seler
        cukinia
        cebula czerwona
        szpinak
        kapusta
        papryka chilli
        brokuł
        pomidorki koktajlowe
        dynia
        rzodkiewka
        buraki
        cebula dymka
        kukurydza
        kapusta pekińska
        kalafior
        awokado
        rukola
        grzyby suszone
        sałata zielona
        sałata lodowa
        szparagi
        bakłażan
        szalotka
        korzeń pietruszki
        kurki
        mix sałat
        młoda kapusta
        kalarepa
        bób
        jarmuż
        brukselka
        botwina
        kapusta czerwona
        bataty
        boczniaki
        mieszanka warzywna
        jabłka
        cytryna
        truskawki
        pomarańcza
        banan
        maliny
        gruszka
        śliwki
        limonka
        jagody
        wiśnie
        ananas
        rabarbar
        brzoskwinie
        kiwi
        porzeczka
        żurawina
        mango
        borówka
        arbuz
        kokos
        syrop malinowy
        sos karmelowy
        syrop cukrowy
        sos truskawkowy
        syrop miętowy
        sos malinowy
        sos wiśniowy
        orzech włoski
        rodzynki
        migdały
        słonecznik
        orzeszki ziemne
        sezam
        mak
        płatki migdałowe
        orzech laskowy
        suszona żurawina
        suszone śliwki
        siemię lniane
        pestki dyni
        daktyle
        pistacje
        chia
        gouda
        twaróg
        parmezan
        mozzarella
        mascarpone
        feta
        serek śmietankowy
        serek homogenizowany
        serek topiony
        ser pleśniowy
        ricotta
        cheddar
        camembert
        serek wiejski
        ser kozi
        grana padano
        oscypek
        halloumi
        margaryna
        śmietanka
        śmietana kremówka
        kwaśna śmietana
        jogurt grecki
        masło klarowane
        maślanka
        kefir
        mleko w proszku
        mleko skondensowane
        mleko kokosowe
        mleko sojowe
        tofu
        mleko migdałowe
        płatki drożdżowe
        wędzone tofu
        śmietanka kokosowa
        jogurt kokosowy
        mleko ryżowe
        mleko owsiane
        szynka
        boczek wędzony
        kiełbasa
        parówki
        salami
        chorizo
        kabanosy
        kaszanka
        boczek
        mięso mielone
        schab
        cały kurczak
        udko z kurczaka
        karkówka wieprzowa
        wołowina
        pierś z indyka
        żeberka
        skrzydełka z kurczaka
        wieprzowina
        łopatka wieprzowa
        szynka wieprzowa
        polędwiczka wieprzowa
        kaczka
        cielęcina
        indyk
        królik
        golonka wieprzowa
        jagnięcina
        dziczyzna
        łosoś
        wędzony łosoś
        dorsz
        pstrąg
        filety rybne
        tuńczyk
        wędzona makrela
        karp
        anchois
        mintaj
        makrela
        halibut
        sardynki
        krewetki
        surimi
        małże
        nori
        kalmary
        przegrzebki
        mieszanka owoców morza
        krab
        ostrygi
        ośmiornica
        homar
        wakame
        kombu
        algi
        papryka słodka
        chili
        imbir
        wanilia
        gałka muszkatołowa
        pieprz biały
        kolendra
        kminek
        kurkuma
        chrzan
        rozmaryn
        goździki
        koper
        kmin rzymski
        kardamon
        pieprz cayenne
        gorczyca
        papryka wędzona
        mięta
        szałwia
        estragon
        szafran
        trawa cytrynowa
        płatki chili
        curry
        przyprawa do piernika
        przyprawa kebab-gyros
        przyprawa do mięsa
        przyprawa do ryb
        sól ziołowa
        zioła włoskie
        garam masala
        pięć smaków
        miso
        tikka masala
        tandoori masala
        pasta curry czerwona
        za'atar
        cukier puder
        cukier waniliowy
        cukier brązowy
        syrop klonowy
        ksylitol
        erytrytol
        melasa
        drożdże
        żelatyna
        wiórki kokosowe
        budyń
        galaretka
        aromat migdałowy
        aromat cytrynowy
        zakwas
        kisiel
        agar agar
        kakao
        karob
        semolina
        mąka ziemniaczana
        mąka tortowa
        mąka kukurydziana
        mąka krupczatka
        mąka żytnia
        mąka orkiszowa
        mąka chlebowa
        mąka ryżowa
        mąka gryczana
        mąka kokosowa
        skrobia kukurydziana
        mąka owsiana
        mąka migdałowa
        mąka bezglutenowa
        panierka panko
        kasza manna
        kasza jaglana
        kasza gryczana
        kuskus
        kasza jęczmienna
        kasza pęczak
        otręby owsiane
        ryż do risotto
        komosa ryżowa
        ryż brązowy
        bulgur
        musli
        ryż jaśminowy
        kasza kukurydziana
        dziki ryż
        fasolka szparagowa
        fasola
        fasola czerwona
        ciecierzyca
        soczewica
        soczewica zielona
        soczewica czerwona
        groszek cukrowy
        groch łuskany
        fasola czarna
        soja
        fasola mung
        makaron penne
        fusilli
        makaron ryżowy
        lasagne
        tortellini
        makaron chiński
        makaron sojowy
        soba
        makaron jajeczny
        ravioli
        gnocchi
        udon
        kluski
        oliwa
        smalec
        olej kokosowy
        olej roślinny
        olej sezamowy
        olej lniany
        olej arachidowy
        olej z pestek dyni
        koncentrat pomidorowy
        ogórki konserwowe
        kukurydza konserwowa
        pomidor suszony
        kapusta kiszona
        zielone oliwki
        puszka pomidorów
        groszek konserwowy
        czarne oliwki
        puszka tuńczyka
        kapary
        korniszony
        kimchi
        majonez
        sos sojowy
        ocet
        ocet winny
        balsamico
        ocet jabłkowy
        musztarda dijon
        ostry sos
        passata pomidorowa
        sos pomidorowy
        sos czosnkowy
        sos chili
        ocet ryżowy
        pesto
        sos rybny
        worcestershire
        tahini
        sos barbecue
        hummus
        gochujang
        sos pad thai
        dżem
        masło orzechowe
        powidła śliwkowe
        dżem truskawkowy
        dżem wiśniowy
        marmolada różana
        kakao
        czekolada
        gorzka czekolada
        biała czekolada
        nutella
        marcepan
        chałwa
        marshmallow
        wino białe
        czerwone wino
        rum
        piwo
        brandy
        whisky
        gin
        sherry
        mirin
        sok z cytryny
        woda gazowana
        sok z limonki
        sok pomarańczowy
        sok jabłkowy
        sok pomidorowy
        sok malinowy
        kawa
        herbata czarna
        herbata zielona
        matcha
        chleb
        bułka tarta
        tortilla
        bagietka
        wafle
        krakersy
        pita
        ciabatta
        wafle ryżowe
        chipsy tortilla
        taco
        bulion warzywny
        ciasto francuskie
        bulion drobiowy
        rosół z kurczaka
        bulion wołowy
        puree z dyni
        zakwas na żurek
        papier ryżowy
        ciasto filo
        spód do pizzy
    """.trimIndent()

    val all: List<String> = rawIngredients
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.normalizedIngredientKey() }

    fun contains(name: String): Boolean {
        val key = name.normalizedIngredientKey()
        return all.any { it.normalizedIngredientKey() == key }
    }

    fun suggestions(query: String, excluded: Set<String> = emptySet(), limit: Int = 12): List<String> {
        val normalizedQuery = query.normalizedIngredientKey()
        if (normalizedQuery.isBlank()) return emptyList()

        val excludedKeys = excluded.map { it.normalizedIngredientKey() }.toSet()
        return all
            .asSequence()
            .filter { it.normalizedIngredientKey() !in excludedKeys }
            .map { it to it.normalizedIngredientKey() }
            .filter { (_, key) -> key.contains(normalizedQuery) }
            .sortedWith(
                compareBy<Pair<String, String>> { (_, key) -> if (key.startsWith(normalizedQuery)) 0 else 1 }
                    .thenBy { it.first.length }
                    .thenBy { it.first }
            )
            .map { it.first }
            .take(limit)
            .toList()
    }
}

fun String.normalizedIngredientKey(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^a-z0-9ąćęłńóśźż ]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
