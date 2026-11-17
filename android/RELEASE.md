# BAZNA 1.5.25 — Android i zajedničko izdanje

| Komponenta | Inačica | Ograničenja i status |
|---|---|---|
| Android | **1.5.25**, versionCode **28** | `com.brendigo.bazna`, minSdk 26, targetSdk 36 |
| WordPress | **1.5.25** | Postojeći identitet `brendigo-sidrena-cijena`, GPLv2+ |
| Windows | **0.10.11** | Windows Setup i Portable |
| Web, Joomla, Drupal, Shopify | **1.1.6** | Postojeća kompatibilna javna v1 sučelja |

## Sinkronizacija

Android čita isključivo **posljednje javno objavljene** WordPress/CMS cijene, datoteke, poslovnice i arhivu preko HTTPS-a. Nakon povratka u prvi plan osvježava povezane podatke ako su stariji od dvije minute. Nema pozadinskog intervalnog dohvaćanja, oglasa ili administratorskih vjerodajnica u aplikaciji. Uvijek je moguće ručno osvježiti.

WordPress administracija i Windows imaju odvojenu autentificiranu razmjenu **samostalnog nacrta** sa zaštitom od sukoba verzija. Ta razmjena ne objavljuje službene CSV/XML cijene. Automatsko dvosmjerno uređivanje s Androida i kompletna transakcijska sinkronizacija WooCommerce/ostalih CMS-ova **nisu implementirani**.

## Google Play

Isporuka uključuje debug APK i nepotpisani release AAB. Za Play Console treba aplikacijski ključ, Google Play App Signing, točnu Data safety prijavu, objavljenu Android-specifičnu politiku privatnosti, pre-launch test i odobrenje. Prolazak GitHub CI testova **nije** Google Play odobrenje.

## WordPress.org

Izvorni slug, direktorij, text domain i postojeći podaci ostaju nepromijenjeni. Distribucija WordPress.org zaseban je SVN korak i nije automatski dokazana objavom na GitHub Releases. Potrebne su ovlaštene SVN vjerodajnice i uspješna sigurnosna revizija WordPress.org.

[Google Play priprema](../docs/ANDROID-GOOGLE-PLAY-CHECKLIST.md) · [WordPress.org ažuriranja](../docs/WORDPRESS-ORG-AZURIRANJE.md) · [Sva GitHub izdanja](https://github.com/bren-wp/BAZNA/releases)
