# BAZNA 1.5.12 — kontrolna lista za hrvatske cijene i javne cjenike

**Pregled izvora: 3. studenoga 2026.** Ovo nije pravno mišljenje ni potvrda zakonitosti podataka određenog trgovca. Budući provedbeni pravilnici nakon 3. studenoga zahtijevaju dodatnu reviziju.

## Pravni izvori i kalendar

- [Zakon o izmjenama i dopunama Zakona o zaštiti potrošača, NN 59/2026](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_06_59_728.html): razlikuje maloprodajnu, jediničnu i baznu cijenu; bazna cijena definira se kao maloprodajna cijena primjenjiva na određeni dan u prethodnom razdoblju. Nije automatski najniža cijena prije sniženja.
- [Pravilnik o isticanju maloprodajne i jedinične cijene, NN 105/2026](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_105_1270.html): jasna, vidljiva, čitljiva i nedvosmislena istaknutost cijena i jedinica mjere.
- [Odluka o isticanju dodatne cijene, NN 101/2026](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_101_1212.html), izmijenjena [NN 110/2026](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_110_1309.html): primjena **17. studenoga 2026.**; referentni dan općenito 10. rujna 2026., uz nastavak iznimke 2. svibnja 2025. za ranije obuhvaćene kategorije hrane, pića, kozmetike, kućanstva itd. Podatke mora potvrditi odgovorna osoba.
- [Odluka o objavi cjenika, NN 101/2026](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_101_1213.html), izmijenjena [NN 110/2026](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_110_1310.html): primjena **17. studenoga 2026.**; javni cjenici i strojno čitljiv pristup, uključujući objavu relevantnih informacija o proizvodima i uslugama, poslovnicama, posebnom obliku prodaje i referentnim cijenama.
- Nacrti budućih pravilnika o **baznoj cijeni** i **načinu objave cjenika** bili su u savjetovanju (do 13. studenoga 2026.). **Ne predstavljati nacrt kao konačno doneseni propis.** Kada bude objavljen konačni tekst, prije daljnje objave potrebna je ponovna provjera implementacije.

## Tehnički kriteriji koje treba provjeriti na stvarnom webu

1. Svaki objavljeni proizvod i usluga mora imati **stvarnu** maloprodajnu cijenu i zaseban povijesni/referentni zapis koji potječe iz evidencije (bez auto-popunjavanja iz trenutačne cijene).
2. Prikaz jedinične cijene i jedinice mjere, s točnim zaokruživanjem, tamo gdje je primjenjivo.
3. Poseban oblik prodaje i podatak o najnižoj cijeni u 30 dana prije sniženja tretirati odvojeno od bazne/sidrene cijene; ne koristiti jedno polje za sva tri pojma.
4. Javni HTML cjenik i CSV/XML ostaju dostupni bez lozinke, bez licencnog ključa i bez administratorske prijave. **Aktivacija, deaktivacija ili isteka licence ne smiju brisati arhivu ili sakriti objavljene cijene.**
5. CSV/XML imaju stabilnu shemu, identifikatore poslovnica, oznake vremena i arhivirane datoteke; objava mora jasno razlikovati **objavljeno** od **neobjavljenih promjena**.
6. Automatsko preuzimanje mora raditi s javnim WordPress REST v1 API-jem za `/wp-json/` i `?rest_route=`. Android ne smije izmišljati cijene pri nevaljanom CSV-u niti slati licence ili administratorske podatke.
7. Testirati stvarnu WordPress instalaciju, opcionalni WooCommerce, SEO/poveznice, cache/CDN/WAF, cron poslove, različite širine mobilnih uređaja i stvarne povijesne podatke prije produkcije.
8. Administratorske promjene provoditi uz WordPress prava pristupa i nonce; sigurnosno kopirati podatke prije nadogradnje. Ne objavljivati licencne ključeve ni GDPR/osobne podatke u javnom REST API-ju.
9. Obveza ažuriranja cjenika i točnost pravnih podataka ostaje kod trgovca. Prikaz `legal_compliance_certified=false` ne smije se automatski promijeniti u `true` bez posebnog postupka.

## Automatizirane provjere

- `php tools/test_legal_contract.php` i `php tools/test_legal_price_safety.php`: status propisa, referentni datumi, zabrana izmišljanja povijesnih cijena.
- `php tools/test_public_api_contract.php`: javno anoniman REST, CSV status objave, arhiva i poslovnice.
- `php licenca/tests/test.php` i `php tools/test_bazna_license_domain.php`: jedna domena po ključu, JSON evidencija i privatnost.
- Android JUnit/Lint/12 zaslona × 9 emulator profila i WordPress Chrome testovi na 13 širina.

**Posebno:** licencni sustav je komercijalna kontrola administracije, a ne dio zakonskog zahtjeva da javni cjenici budu otvoreni potrošačima.
