# SIDRENA — pravni okvir i interoperabilnost (24. 10. 2026.)

## Što je utvrđeno u službenim propisima

**Mjerodavni tekstovi**:
- NN 101/2026-1212: https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_101_1212.html
- NN 101/2026-1213: https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_101_1213.html
- NN 110/2026-1309: https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_110_1309.html
- NN 110/2026-1310: https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_110_1310.html
- Zakon o zaštiti potrošača — izmjene NN 59/2026: https://narodne-novine.nn.hr/clanci/sluzbeni/2026_06_59_728.html
- Pravilnik o načinu isticanja maloprodajne cijene i cijene za jedinicu mjere, NN 105/2026: https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_105_1270.html

Odluke NN 101/2026 izmijenjene su u NN 110/2026, tako da **17. studenoga 2026.** zamjenjuje prvotno planirani **1. listopada 2026.** za primjenu proširenih obveza. Referentni datumi nisu promijenjeni: **2. svibnja 2025.** za ranije obuhvaćene kategorije (hrana, piće, kozmetika, sredstva za čišćenje, toaletne potrepštine, proizvodi za kućanstvo) i **10. rujna 2026.** za ostalu robu i usluge.

Prema Odluci o objavi cjenika, trgovac s mrežnom stranicom objavljuje strojno čitljiv CSV/XML cjenik proizvoda jednom dnevno najkasnije do 8:00, a pružatelj usluga pri promjeni cijena, prema propisanom roku. Objavljeni cjenici moraju biti dostupni najmanje 30 dana. Datoteke moraju biti pogodne za automatizirano prikupljanje, s jedinstvenom strukturom po poslovnicama.

**Nacrti nisu propisi:** U listopadu 2026. objavljeni su prijedlozi dvaju pravilnika o baznoj cijeni i objavi cjenika, s javnim savjetovanjem do 13. studenoga 2026. Nisu automatski stupili na snagu. Nakon objave konačnih pravilnika potrebna je dodatna provjera izuzeća, trajnih pravila i mogućih prijelaznih odredbi. SIDRENA ne izdaje automatiziranu potvrdu pravne usklađenosti.

## Sinkronizirani javni profil

WordPress dodatak **1.5.2** dodaje informacijsko polje `legal` u postojeći javni `GET /wp-json/brendigo-sidrena/v1/cjenici` bez izmjena u starim `feeds`, `outlets` ili arhivskim rutama. Također vraća `plugin_version` i `api_version`. U `legal` se prenose pravna revizija, datumi, rokovi objave, trajanje arhive, formati, popisi traženih javnih polja i URL-ovi propisa.

Android **0.3.2** i Windows **0.10.5** čitaju taj isti profil, parsiraju ga s provjerama tipova i prikazuju izvorne referentne datume. Za stariji plugin 1.5.0 ostaje moguć **samo javni pregled**, uz upozorenje da novi profil nije dostupan. Ne mijenjaju se već pohranjene sidrene cijene niti se pokušava rekonstruirati cijena za prošle datume.

### Granice povezivanja

- WordPress je **izvor istine za objavljene online cjenike**. Android je javni klijent za čitanje.
- Windows ima **zasebnu lokalnu bazu**. Pregledava objavljene WordPress podatke, ali ne provodi automatsko dvosmjerno uređivanje.
- Potpuno administratorsko uređivanje na Androidu i Windowsu zahtijeva poseban autentificirani protokol, minimalne WordPress capability dozvole, autentifikaciju bez administratorove lozinke u aplikaciji, ograničenja brzine, verifikaciju konkurentnih izmjena, opoziv pristupa i audit log.
- Nikad ne koristite javni REST za izmjene kataloga ni ne pretpostavljajte da su lokalni Windows nacrti automatski objavljeni na webu.
- Sami API metapodaci ne znače da korisnikov katalog sadrži sve podatke ili da su cijene povijesno točne.

## Kontrolni popis prije 17. 11. 2026.

1. Za svaku stavku provjerite kojoj kategoriji pripada te referentni datum i **dokazivu cijenu** na taj datum; prazna povijest se ne popunjava iz trenutačne cijene.
2. Provjerite popunjenost obveznih javnih CSV/XML polja i ispravnost cijena, uključujući posebne oblike prodaje, barkod, marku, jedinice i dostupnost po poslovnici.
3. Provjerite stvarno objavljivanje prije propisanog roka, postavite pouzdani poslužiteljski cron i potvrdite najmanje 30 dana javne dostupnosti arhive.
4. Provjerite funkcionalnost dohvaćanja bez prijave, odvojene datoteke poslovnica, stabilne URL-ove te sigurnu dostupnost XML/CSV sadržaja automatiziranim klijentima.
5. Pratite objavu konačnih pravilnika i izuzeća prije promjene poslovnih pravila.

Ovaj dokument opisuje stanje na datum iz naslova i nije zamjena za pravni savjet.

## Kontrolirani prijenos lokalnog Windows kataloga u WordPress

Windows **0.10.5** nudi eksplicitnu mogućnost izvoza radnog CSV-a s 15 zaglavlja usklađenih s WordPress SIDRENA uvoznikom (`naziv;sku;vrsta;marka;url;cijena_redovna;cijena_akcijska;naziv_posebnog_oblika;jedinica_mjere;cijena_jedinice_mjere;sidrena_cijena;sidrena_datum;barkod;dostupnost;poslovnica`).

Izvoze se samo stvarno spremljeni naziv, vrsta, SKU, barkod, jedinica, aktualna cijena te već potvrđeni referentna cijena i datum. Polja bez lokalnog izvora ostaju prazna. Slijedi **ručni pregled i potvrda** u WordPress administratorskom uvozu; datoteka nije samostalno zakonski ovjerena javna objava. Windows lokalni ID ne smije se automatski prenositi kao globalni WordPress ID.

## Dopuna 24. listopada 2026. — objavljeni propisi i prijelazni režim

NN 105/2026 zaseban je, već objavljen pravilnik za maloprodajnu cijenu i cijenu po jedinici mjere, uključujući način njihovog prikaza, eursku valutu i isticanje cjenika usluga. **Nije isto što i budući pravilnik o baznoj cijeni ili pravilnik o sadržaju i načinu objave cjenika.** Potonji su najavljeni, a najava nije zamjena za objavljeni normativni tekst. Proširenje obveza prema NN 101/2026 izmijenjenim NN 110/2026 predviđeno je za 17. studenoga 2026.

WordPress pravni profil u verziji 1.5.2 dodaje `consumer_law_revision` = `NN-59-2026` i `unit_price_rule_revision` = `NN-105-2026`, zadržavajući istu shemu API-ja i kompatibilnost klijenata. Windows i Android zadržavaju jednake oznake pravnih izvora i referentne datume, bez proizvoljnog određivanja kategorije robe ili generiranja povijesnih cijena. 

**Ograničenje:** Ovo je proširenje pravne sljedivosti, a ne certificiranje stvarnog trgovca. Nema automatske dvosmjerne administrativne sinkronizacije: javni WordPress API je samo za čitanje, a prijenos Windows nacrta u WordPress ostaje kontrolirani CSV uvoz.


## Dodatna interoperabilnost — Android 0.3.2 / Windows 0.10.5

Oba klijenta zahtijevaju `api_version: 1`, provjeravaju izvore javnih cjenika i odbijaju nepodržan odgovor. Android odsad provjerava sve poslovnice umjesto tihog skraćivanja popisa, odbija ponovljene oznake i neispravne HTTPS poveznice te koristi ista pravila kao Windows. Polja `consumer_law_revision` i `unit_price_rule_revision` prikazuju se samo ako su zaista objavljena u WordPress profilu.

**WordPress 1.5.2 ostaje nepromijenjen.** Izvor istine i dalje su javno objavljeni podaci na WordPressu; mobilni klijent dohvaća podatke, a Windows lokalni nacrti ne upisuju se automatski u WordPress. Sinkronizirani podaci ne znače dvosmjerni administratorski pristup.


## Dopuna 29. listopada 2026.

WordPress 1.5.3, Android 0.3.3 i Windows 0.10.6 razdvajaju najnižu cijenu tijekom 30 dana prije sniženja od fiksne sidrene cijene i uključuju datum stupanja NN 105/2026 na snagu 26. 9. 2026. Javna JSON pravna shema ostaje kompatibilna (schema 1). Proširena pravila 2026/11/17 ostaju odvojena od ranijih pravila koja su već na snazi. Prijedlozi pravilnika na savjetovanju do 13. 11. 2026. nisu konačni tekstovi. Detalji u [pravnoj reviziji](LEGAL-REVIEW-2026-10-29.md).

## Dopuna 2. studenoga 2026. — pravna provjera i zaštita povijesnih podataka

**Datum provjere: 2. 11. 2026.** Odluke NN 101/2026 izmijenjene NN 110/2026 proširuju primjenu od **17. 11. 2026.** Povijesne cijene za ranije obuhvaćene kategorije odnose se na **2. 5. 2025.**, a za nove kategorije i usluge na **10. 9. 2026.** NN 105/2026 o maloprodajnoj cijeni, cijeni za jedinicu mjere i cjeniku usluga na snazi je od **26. 9. 2026.**. Dva predložena pravilnika o baznoj cijeni i digitalnim cjenicima u e-savjetovanju su do 13. 11. 2026.; prijedlog nije doneseni propis. Pojam bazne cijene iz Zakona o zaštiti potrošača, promijenjenog NN 59/2026, i zasebna najniža cijena 30 dana prije sniženja ne mogu se automatski zamijeniti sidrenom cijenom.

### WordPress 1.5.4 — provedene zaštite u PR-u #202

- Ukinuto automatsko popunjavanje sidrene cijene iz **današnje** redovne WooCommerce cijene. I ranije spremljena automatska postavka sada je blokirana.
- Isključene su stare masovne SQL/CLI/cron i AJAX operacije koje pokušavaju popuniti nepoznate povijesne vrijednosti iz aktualnih cijena; postojeće zabilježene vrijednosti nisu prebrisane.
- Prazan povijesni datum na proizvodu ili samostalnoj usluzi ostaje **prazan**, umjesto da se bez dokaza preslika opći datum. Ručno potvrđeni datumi i iznosi ostaju nepromijenjeni.
- U administraciji su zastarjele kontrole masovnog popunjavanja zamijenjene uputama za dokazivi unos.
- Javni REST v1 zadržava kompatibilne URL-ove, a legal profil dodatno sadrži datum pravne provjere, status savjetovanja, zabranu automatskog izvođenja bazne cijene, obvezu povijesne provjere i oznaku da pravna usklađenost NIJE certificirana.
- Oznaka publication.pending_changes razlikuje postojeće CSV datoteke od naknadnih promjena koje još nisu ponovno objavljene; publication.compliance_certified uvijek je false.

### Android 0.3.4 — provedene zaštite

- Android legalni parser prihvaća stare REST v1 profile i validira nova, opcionalna pravna polja. Ne prihvaća proturječan navod da je cijeli katalog zakonski certificiran.
- Detalj 06/12 čita **stvarna** objavljena polja: trenutnu cijenu, sidrenu cijenu i datum, jediničnu cijenu, naziv posebnog oblika prodaje, barkod, marku, dostupnost i neobaveznu stvarnu fotografiju. Nedostajući sidreni datum označen je neprovjerenim.
- Početna i učitavanje kataloga prikazuju obavijest za neobjavljene promjene čak i kada stariji objavljeni CSV i dalje postoji.
- Android je read-only i ne koristi administratorske lozinke niti mijenja WordPress cijene.

### Nužne provjere prije produkcijske potvrde

Automatski testovi pokrivaju tipove polja, nepoznat datum, blokadu automatskog popunjavanja, kompatibilne REST odgovore i publikacijski status. Prije zakonske potvrde trgovac mora neovisno dokazati stvarnu cijenu na referentni dan, ispravnu kategoriju i iznimke, punih 30 dana prije sniženja, jedinične cijene, opseg i ukupnu cijenu usluga, eventualne maksimalne dopuštene cijene, održavanje javne arhive i pouzdanu objavu u propisano vrijeme. Potrebno je provjeriti doneseni tekst pravilnika nakon savjetovanja i stvarno ponašanje WordPress hostinga; WP-Cron ne jamči 08:00. Pixel-perfect potvrda zahtijeva emulator i screenshot usporedbu.

**Normativni izvori:** NN 75/2025, NN 101/2026 (1212 i 1213), NN 110/2026 (1309 i 1310), NN 59/2026, NN 105/2026. Poveznice su navedene u uvodu ovog dokumenta. Status e-savjetovanja: https://mingo.gov.hr/vijesti/ministar-susnjar-kroz-dijalog-s-gospodarstvenicima-do-provedivih-pravila-i-vece-zastite-potrosaca/10461

Ova dopuna nije potvrda pravne usklađenosti bilo kojeg konkretnog trgovca.
