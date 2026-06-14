# Kucharz

Aplikacja Android w Kotlin + Jetpack Compose do wyszukiwania przepisów na podstawie składników dostępnych w lodówce.

## Funkcje

- wpisywanie listy składników,
- automatyczne uwzględnianie stałych składników, np. cukier, sól, pieprz,
- ekran pasujących przepisów,
- ekran przepisów, w których brakuje 1–2 składników,
- dodawanie brakujących składników do listy zakupów,
- lokalna lista zakupów w Room,
- lokalna lista stałych składników w Room,
- historia otwieranych przepisów,
- Retrofit + Hilt + Room + KSP.

## API

Aplikacja korzysta z endpointu:

```http
POST /recipes/by-available-ingredients
```

Domyślny adres API jest ustawiony w `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "RECIPE_API_BASE_URL", "\"http://10.0.2.2:8000/\"")
```

Dla emulatora Androida `10.0.2.2` oznacza komputer hosta. Jeżeli API działa gdzie indziej, zmień tylko tę wartość, bez zmieniania ścieżek endpointów.

## Uruchomienie

```bash
./gradlew clean assembleDebug
```

Jeżeli Android Studio nie widzi najnowszych zmian z GitHuba:

```bash
git fetch origin
git checkout main
git pull origin main
```
