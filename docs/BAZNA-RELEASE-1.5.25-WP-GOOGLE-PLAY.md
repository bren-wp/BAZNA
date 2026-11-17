# BAZNA 1.5.25 — WordPress.org + Google Play provjera

## Funkcionalnosti implementirane u ovom izdanju
- Android `com.brendigo.bazna`: `targetSdk 36`, `compileSdk 37`, `minSdk 26`, `versionCode 28`; foreground-only osvježavanje već spojenog **javnog** kataloga nakon dvije minute, ili ručno na zahtjev.
- Android nema administratorski login ni dozvole za kameru, mikrofon, lokaciju ili kontakte; HTTPS i kontrola odredišta ostaju obvezni.
- WordPress dodatak `brendigo-sidrena-cijena` ostaje na izvornom WordPress.org slugu, GPLv2+, s podržanim PHP 7.4; sve postojeće administratorske funkcije i WooCommerce podaci ostaju netaknuti.
- Windows/WordPress privatni samostalni nacrt: odvojeni autorizirani REST pristup i zaštita od zastarjelih revizija; public REST v1 i Android nikad ne dobivaju administratorske vjerodajnice.
- WordPress readme sadrži napomene o javnoj objavi, privatnosti i aplikacijskim lozinkama; administrativne poruke ostaju kratke.

## Izvori pravila
- https://developer.wordpress.org/plugins/wordpress-org/detailed-plugin-guidelines/
- https://developer.wordpress.org/plugins/wordpress-org/automated-security-review/
- https://developer.wordpress.org/rest-api/using-the-rest-api/authentication/
- https://support.google.com/googleplay/android-developer/answer/11926878
- https://support.google.com/googleplay/android-developer/answer/10144311
- https://support.google.com/googleplay/android-developer/answer/10787469

## Granice tvrdnji i postupci za stvarnu distribuciju
- **GitHub Release nije WordPress.org SVN objava.** Nakon objavljenog i verificiranog GitHub izdanja treba uspješan WordPress.org deploy s `SVN_USERNAME` i `SVN_PASSWORD` tajnama te WordPress.org automatska revizija.
- **Nepotpisani AAB nije Google Play izdanje.** Potreban je potpis privatnim upload ključem i pregled kroz Play Console, deklaracija Data safety, objavljena BAZNA-specifična politika privatnosti te provjera svih uključenih SDK-ova.
- Samo javni WP CSV/XML podaci sinkroniziraju se s Androidom. Android ne šalje cijene na WordPress, a Windows dvosmjerno uređivanje ograničeno je na samostalni nacrt, bez WooCommerce transakcijske sinkronizacije.
- Nije proveden produkcijski test točnog zakonskog roka 11:00, 15-dnevne arhive nakon zamjene ili stvarnih povijesnih iznosa trgovca. Ne smiju se automatski potvrditi.
- Testovi 1:1 razmaka i piksela zahtijevaju referentne PNG slike svakog zaslona i usporedbu rezultata; postojeći emulator/desktop testovi sami nisu dokaz pixel-perfect podudarnosti.
