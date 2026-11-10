# BAZNA — Bazne cijene — verzije i izdavanja

| Komponenta | Verzija |
|---|---|
| Android | **1.5.16 (javno izdanje)** |
| Windows Desktop | **0.10.6** |
| WordPress dodatak | **1.5.16 (javno izdanje)** |

Verzije su **odvojene**. Android 1.5.16 i WordPress 1.5.16 sadrže stvarne, međusobno usklađene promjene u PR-u za izdanje 1.5.16. Izdanje se objavljuje tek nakon uspješnog Android, WordPress, Windows i sigurnosnog CI-ja; debug APK i nepotpisani AAB nisu produkcijski potpisani. WordPress dodatak 1.5.16 potrebno je zasebno instalirati ili ažurirati da bi novi statusi objave CSV-a bili dostupni Androidu.

## Provjere i artefakti

Automatski GitHub Actions postupci izvode Android Kotlin/JUnit/lint provjere i grade APK i AAB, a Windows postupak pokreće .NET testove, WPF navigacijske provjere, izrađuje prijenosnu aplikaciju i Inno Setup instalaciju. Poseban audit pokriva WordPress PHP sintaksu i integritet službenog paketa, Windows i Android statičke provjere.

Novo objedinjeno izdanje sadrži:
- `BAZNA-Android-debug.apk` — Android instalacijski APK za provjeru.
- `BAZNA-Android-unsigned.aab` — AAB bez produkcijskog potpisa.
- `BAZNA-Desktop-win-x64-Portable.exe` — Windows prijenosna aplikacija.
- `BAZNA-Desktop-win-x64-Setup.exe` — Windows instalacijski program.
- `BAZNA-WordPress-1.5.16.zip` — WordPress ZIP isporučen u GitHub izdanju nakon provjere izvornog koda i kontrolnih zbrojeva.
- Kontrolne SHA-256 datoteke i Windows izvještaj o porijeklu binarnih datoteka.

Objava paketa moguća je tek kada Android i WordPress CI uspješno završe za isti commit i svi novi reproducibilni ZIP paketi imaju potvrđene SHA-256 kontrolne zbrojeve. WordPress ZIP 1.5.16 namjerno ima novi hash zbog promjena izvornog koda. U nedostatku produkcijskog potpisnog ključa Android AAB ne treba predstavljati kao spreman za Google Play.

[GitHub izdanja](https://github.com/bren-wp/BAZNA/releases)
