# BAZNA — usporedba javnih cjenika i pravila prikaza

Datum pregleda: 17. 11. 2026. | Status: smjernice dizajna i funkcionalnosti; ne predstavlja potvrdu usklađenosti drugih trgovaca.

## Vanjski primjeri

**Tommy** — [Objava cjenika](https://www.tommy.hr/objava-cjenika) je zasebno dostupna stranica u donjem dijelu weba, s kratkim opisom i preuzimanjem CSV-a. Prednosti su vidljiv javni ulaz i neometano preuzimanje. U prikazanim rezultatima iz studenoga 2026. datoteke su odvojene po prodavaonicama, adresama, oznakama i vremenskim oznakama. **Ne kopirati prethodni pravni tekst**: stranica još citira NN 75/2025, dok je noviji Pravilnik NN 132/2026-1584 stupio na snagu 17. 11. 2026.

**BAZNA dizajnerska pravila iz usporedbe**: sažeta naslovnica javnog cjenika; odvojeni filteri poslovnice, vrste robe/usluga i datuma; jasne akcije **Otvori CSV**, **Otvori XML**, **Arhiva**; bez dugih tekstualnih uputa u svakom panelu; legenda i detalji u zasebnom odjeljku **Pomoć**. Izgled i sadržaj tuđih stranica nisu predlošci za doslovno kopiranje.

## Važeća pravila su jača od primjera

[NN 132/2026-1584](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1584.html):
- Članak 3. stavak 5.: izmijenjeni cjenik na dan izmjene **najkasnije do 11:00**.
- Članak 3. stavak 6.: stari javni cjenik mora ostati dostupan **15 dana od zamjene**.
- Članak 3. stavak 7.: zasebne poslovnice osim istoga cjenika za sva mjesta.
- Članak 5.: automatiziran strojni dohvat mora biti omogućen.
- Članak 6.: CSV ili XML, propisana imena, stabilan URL, UTF-8 i stupci CSV-a odvojeni točka-zarezom.

[NN 132/2026-1583](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1583.html): bazna cijena mora potjecati iz stvarne povijesne evidencije prema klasifikaciji i iznimkama; trenutna i 30-dnevna najniža cijena nisu zamjena za dokaz o baznoj cijeni.

## Pravila za UI/UX

1. Na **svakom operativnom zaslonu** samo naslov, polja, akcije i povratna informacija; pravna objašnjenja i upute na jednom centralnom mjestu.
2. Pokaži upozorenje **samo kada je vezano uz trenutačnu radnju** (nedostaje dokaz, neuspjela objava, neusklađena promjena ili brisanje).
3. Nikada ne prikazivati korisniku izraze `debug`, `fixture`, `CI`, `internal`, `mock`, `developer mode` ili implementacijske detalje; iznimke su samo interne evidencije i developerski dokumenti.
4. Javni prikaz omogućiti bez obvezne prijave, bez CAPTCHA-e i bez ovisnosti o JavaScriptu za stvarni CSV/XML dohvat.
5. Jasno razlikovati **spremljeno kao nacrt**, **čeka objavu** i **službeno objavljeno**. Sama sinkronizacija nikada se ne prikazuje kao pravna objava.
6. Za slike 1:1 usporediti referentne zaslone, tipografiju, razmake, boje i stanja gumba kroz DPI i dostupne veličine — samo responsive snimka nije pixel-perfect certifikat.

## Provjera implementacije

- WordPress: `[bazna_objava]`, `[bazna_arhiva]`, `[bazna_datoteke]`, javna CSV/XML dostupnost.
- Windows: kratki tekstovi u kontekstualnim zaslonima, detaljni koraci u **Pomoć**, potvrda samo za sigurnosno osjetljive akcije.
- Android: pravni detalji sklopljeni prema zadanim postavkama; zastarjeli tekst o savjetovanju uklonjen nakon donošenja pravilnika.
- Standalone PHP: sažet dashboard, posebna kartica pomoć i javni API.
- Prije produkcije: E2E test na pravoj domeni i hostingu, rok 11:00, dostupnost arhive te provjera stvarnih povijesnih iznosa.
