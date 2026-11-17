# BAZNA — revizija i prijelaz na NN 132/2026 (17. studenoga 2026.)

## Službeni izvori

- [Pravilnik o isticanju bazne cijene proizvoda, NN 132/2026, br. 1583](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1583.html)
- [Pravilnik o načinu objave cjenika proizvoda, NN 132/2026, br. 1584](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1584.html)
- [NN 132/2026, br. 1578–1580](https://narodne-novine.nn.hr/): ranije odluke o izravnim mjerama objave cjenika/dodatne cijene prestaju važiti 17. 11. 2026.
- Zakon o zaštiti potrošača NN 19/22, 59/23, 59/26; odvojeno primjenjivati obvezu najniže cijene prije sniženja i cijenu jedinice mjere.

**Ovaj softver ne utvrđuje jesu li podaci pojedinog trgovca pravno točni i nije zamjena za pravno tumačenje.**

## Što pravilnici izričito propisuju

1. **Referentni datumi bazne cijene:** 2. 5. 2025. za hranu, piće, kozmetiku, sredstva za čišćenje, toaletne potrepštine i proizvode za kućanstvo; 10. 9. 2026. za ostalu robu i usluge. Ako je roba/usluga prvi put stavljena u ponudu nakon relevantnog datuma, koristi se prvotni datum ponude.
2. **Popust na referentni dan:** za skupine s referentnim datumom 2. 5. 2025. temeljem čl. 3. st. 2–3 primjenjuje se najniža cijena u 30 dana prije početka posebnog oblika prodaje; za ostalu robu/usluge temeljem čl. 3. st. 4–5 prethodno primijenjena maloprodajna cijena, a ne snižena referentna cijena. To nije isto što i opći prikaz najniže cijene prije trenutačnog sniženja.
3. **Isticanje:** na prodajnom mjestu uz aktualnu cijenu i, kada je primjenjivo, cijenu po jedinici mjere. Za usluge uz aktualni cjenik ili u katalogu/brošuri kod velikog broja raznovrsnih usluga. Obvezno u tiskanom i televizijskom oglasu s aktualnom cijenom. U ostalim oblicima oglašavanja pravilo je drukčije.
4. **Iznimke:** cijene uređene posebnim propisima; odgovarajuće cijene za skupine potrošača ili načine plaćanja; individualno personalizirane cijene; određen dinamički model cijena kod kojeg jedinstveni usporedivi elementi nisu unaprijed utvrdivi; cijene na automatima; odgovarajući posrednički kanali/platforme. Opravdanost pojedine iznimke mora se dokumentirati.
5. **Objava cjenika:** na vlastitoj mrežnoj stranici, dostupna strojno čitljiva CSV/XML datoteka u UTF-8 kodiranju, CSV stupci odijeljeni točkom-zarezom. Nakon promjene cjenik treba objaviti **do 11:00 na dan promjene**, a zamijenjeni cjenik javno čuvati **15 dana od zamjene**.
6. **Redoslijed polja:** roba: naziv, šifra ako je primjenjivo, marka ako je primjenjivo, neto količina kada je primjenjivo, jedinica mjere ako je primjenjivo, maloprodajna cijena i podatak o posebnom obliku prodaje, cijena jedinice mjere ako je primjenjivo, barkod ako je primjenjivo, EAN barkod ako je primjenjivo, bazna cijena. Usluge: naziv, maloprodajna cijena i podatak o posebnom obliku prodaje, bazna cijena.
7. **Filename i poslovnice:** identifikator trgovca (OIB pravne osobe ili matični broj fizičke), prodajno mjesto / `ZA_SVA_PRODAJNA_MJESTA` kad je isti cjenik za sve, vrsta robe/usluga, broj pohrane, vremenska oznaka `GGGGMMDD_SSMM`. Izmijenjeni cjenik ide na isti URL. Automatizirani alati moraju moći pristupiti javnim cijenama.

## Konkretni zahvati u ovom PR-u

| Područje | Zahvat | Granice |
| --- | --- | --- |
| WordPress pravni API | `NN-132-2026-1583-1584`, status `enacted`, 11:00, 15 dana, potvrđeni izvori | `legal_compliance_certified=false`: stanje zakona ne ovjerava trgovca |
| WooCommerce | Novi eksplicitni EAN i neto količina, uključujući varijacije | Ne zaključivati neto količinu iz transportne mase |
| Samostalni WordPress katalog | Neto količina i EAN u podatkovnom modelu, obrascu i spremanju | Postojeća korisnička polja ostaju očuvana |
| Službeni cjenik | Dodani zasebni CSV i XML s novim zakonskim redoslijedom | Stari CSV/XML ostaje kao kompatibilni API za Android/Windows |
| Identifikator trgovca | Unos OIB-a (uz kontrolnu znamenku) ili MB-a, generiranje imenovanih datoteka | Potreban točan identitet i provjera kategorizacije prodajnih mjesta |
| Dostupnost i arhiva | Stalni URL-ovi službenih izvoznih datoteka; imenovane datoteke sačuvane >15 dana; postojeći povijesni arhiv zadržan | Nužan funkcionalan hosting/cron, ne može ga zajamčiti kod |
| Sigurnost objave | Ne izmišljaju se povijesne cijene; nepotpuni službeni izvoz prijavljuje grešku | Nepotpun katalog nije potvrda usklađenosti |
| Android | Novi pravni profil, provjera njegovih tipova i iznosa; stari profili kompatibilni | Javna aplikacija čita, ne uređuje WordPress |
| Windows | Novi pravni profil, status objave i 15-dnevno pravilo; stara kompatibilnost | Lokalni nacrti ne postaju automatski javno objavljeni |
| Testovi | PHP legal / službeni feed / REST stub i android-windows parser regresije; GitHub Actions | Potrebni i testovi na stvarnom WP/Woo, Android emulatoru, Windows računalu |

## Što ostaje provjeriti prije tvrdnje o punoj zakonskoj usklađenosti

- **Referentne cijene:** utvrditi stvarne povijesne iznose i imati dokaz za svaku stavku; naznačiti početak ponude, kategoriju i datum ako je roba/usluga naknadno ponuđena; zasebno označiti i dokumentirati popust na referentni datum, jer pravila nisu jednaka za FMCG i ostale kategorije. Ovaj PR ne provodi pravnu provjeru dokaza, niti je legitimno automatski kopirati trenutačne cijene.
- **Iznimke:** ručno analizirati i dokumentirati artikle i usluge sa zakonskom iznimkom; ne proglašavati svaku personaliziranu ili dinamičku cijenu automatski izuzetom. Trenutačni službeni projektor zahtijeva baznu cijenu za svaku uključenu stavku — katalog s iznimkama treba prethodno ispravno segmentirati, a budući obrazac iznimki i njegovu evidenciju zasebno testirati.
- **Valjanost izvoza:** na uzorku stvarnih trgovina provjeriti točnu semantiku polja `poseban_oblik_prodaje`, stvarnu obveznost neto količine i EAN-a, izvozne nazive datoteka i oblik klasifikacije prodajnog mjesta. OIB se provjerava kontrolnom znamenkom, ne pravnom aktivnošću identiteta.
- **Poslovnice:** testirati da iste cijene na svim lokacijama ne daju lažno različite datoteke, da odvojene cijene imaju odvojeni službeni cjenik i da ih vanjski alati mogu dohvatiti.
- **Hosting i cron:** provjeriti objavu na datum promjene prije 11:00 **Europe/Zagreb**, bez ovisnosti o posjetitelju, i testirati neuspjele objave, povrat na zadnju dobru verziju, istodobne objave, HTTP cache te dostupnost svake zamijenjene datoteke barem 15 dana. `WP-Cron` bez vanjskog pokretača ne jamči rok.
- **Komunikacija s kupcem:** provjeriti u WooCommerce frontend, mobilnoj trgovini, webu i tiskanom/TV oglašavanju da se bazna, aktualna i cijena po jedinici mjere ne miješaju.
- **UI i održavanje:** pregledati sve zaslone na malom/velikom ekranu, testirati pristupačnost, sigurnost AJAX/REST/CSV uvoza i kompatibilnost s WooCommerce/WordPress nadogradnjama. Stari `sidrena_` API ključevi ostaju radi migracije; nemojte ih neselektivno izbrisati.
- **Izdanje:** ne spajati PR i ne objavljivati instalacijske pakete dok svi CI tokovi i testovi na stvarnim platformama nisu zeleni.

## Operativni kontrolni popis

1. Napraviti sigurnosnu kopiju WordPress baze, datoteka i arhive prije nadogradnje.
2. U WordPress postavkama cjenika unijeti stvarni OIB/MB te pregledati svaku poslovnicu.
3. Provesti kategorizaciju robe/usluga i potvrditi baznu cijenu, referentni datum te eventualnu iznimku dokaznom dokumentacijom.
4. Unijeti EAN i neto količinu za artikle gdje su primjenjivi; provjeriti posebne oblike prodaje.
5. Ručno regenerirati i otvoriti službeni CSV/XML, analizirati sadržaj i redoslijed te provjeriti same HTTP URL-ove.
6. Konfigurirati poslužiteljski scheduler koji pouzdano aktivira regeneriranje pri promjeni cijena; redovito provjeravati status, arhivu i pristup automatiziranih programa.
7. Testirati Android i Windows pregled nad *stvarno objavljenim* WordPress podacima; stari API je za čitanje i ostaje stabilan.

Datum revizije: 17. 11. 2026. Ovo je inženjerska analiza usklađenja, a ne pravno mišljenje ni ovjera objavljenih trgovačkih cijena.
