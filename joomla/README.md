# BAZNA za Joomlu (Joomla 5/6)

**Autor:** Brendigo · https://brendigo.com/

U \`joomla/plg_system_bazna\` je moderni Joomla **system plugin** s instalacijskim \`bazna.xml\`, namespaceom, \`services/provider.php\` DI registracijom i \`SubscriberInterface\` event handlerom. Koristi **isti** BAZNA PHP engine i domensku licencu kao samostalni web.

## Instalacija

1. U GitHub Actions preuzmite \`BAZNA-Joomla-Plugin-1.1.1.zip\`; ZIP sadrži \`vendor/Core.php\`, \`vendor/LicenseClient.php\`, **\`vendor/AdminCatalog.php\`**, zajednički responzivni CSS i manifest.
2. Joomla: System → Install → Extensions → Upload Package; zatim Plugins → **System – BAZNA** → Enable.
3. Konfigurirajte u PHP-FPM/Apache okolini (ne unositi u javni sadržaj):
   - \`BAZNA_JOOMLA_PRIVATE_DIR=/srv/bazna-private/joomla-shop\` (izvan Joomla document root);
   - \`BAZNA_JOOMLA_PUBLIC_URL=https://joomla-primjer.hr\`;
   - \`BAZNA_JOOMLA_APP_KEY=<64 nasumična hex znaka>\`.
4. Administratorska stranica: \`https://joomla-primjer.hr/administrator/index.php?bazna=dashboard\`. Joomla upravlja prijavom, ACL-om \`core.admin\` i CSRF tokenom.
5. Aktivirajte BAZNA licencu, a zatim koristite grafičke kartice Proizvodi, Usluge i Poslovnice. Napredni JSON unos dostupan je u kartici Uvoz/Izvoz. Javni v1 API je \`/bazna/v1/cjenici\`. Standardni Joomla SEF routing mora proslijediti putanje u \`index.php\`, ne na statičke 404 stranice.

## Usklađenost

Pruža 9 administratorskih kartica, čitanje HTML/CSV/XML, katalog usluga, poslovnica i arhive. Android koristi javne HTTPS putanje i zajednički API. BAZNA licenca registrira **kanonsku domenu** putem javnog kratkotrajnog proof endpointa.

**Granica izdanja 1.1.1:** Web i Joomla sada koriste isti grafički editor s pojedinačnim formama i provjerama spremanja, ali **još nije izrađena zasebna nativna Joomla MVC komponenta** ni nativni uvoz iz VirtueMart/HikaShop i nije potvrđeno testiranje instalacije na stvarnoj Joomla 5/6 instanci. Za produkciju su potrebni integracijski testovi, CSRF/ACL provjere u stvarnom CMS-u, migracijski scenariji i provjera proširenja.
