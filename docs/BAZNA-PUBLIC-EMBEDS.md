# BAZNA — javni cjenici i arhiva na WordPress stranici

**Status:** dokumentacija za razvojni PR #241, 17. 11. 2026.; ne potvrđuje dovršeno produkcijsko zakonodavno ispitivanje trgovca.

## Ugradnja u WordPress

U administraciji stranice izaberite blok **Kratki kod / Shortcode**, zalijepite jedan od sljedećih kodova te objavite stranicu:

| Kratki kod | Što prikazuje |
| --- | --- |
| `[bazna_objava]` | Cjenik robe i usluga, poveznice na potvrđene službene CSV/XML i javnu arhivu |
| `[bazna_objava vrsta="proizvodi"]` | Samo roba, službeni formati za robu i arhiva |
| `[bazna_objava vrsta="usluge"]` | Samo usluge, službeni formati za usluge i arhiva |
| `[bazna_objava oznaka="RI-01"]` | Cjenik i službene datoteke odabrane poslovnice |
| `[bazna_objava arhiva="ne"]` | Bez ugrađenog popisa arhive; službeni CSV/XML ostaje javno dostupan |
| `[bazna_cjenik]` | Samo pretraživ HTML cjenik |
| `[bazna_cjenik vrsta="proizvodi"]` | Samo tablica robe |
| `[bazna_cjenik vrsta="usluge"]` | Samo tablica usluga |
| `[bazna_arhiva]` | Arhivske verzije: javne službene CSV/XML snimke i starije kompatibilne datoteke |
| `[bazna_datoteke]` | Samo potvrđeni službeni CSV/XML URL-ovi, bez HTML tablice |
| `[bazna_datoteke oznaka="RI-01" vrsta="usluge"]` | Samo službene datoteke usluga te poslovnice |

Stari `sidrena_*` kratki kodovi nastavljaju raditi zbog kompatibilnosti postojećih stranica i veza.

### Prilagodba izgleda

**BAZNA → Postavke → Izgled javnog cjenika i arhive**: svijetla/tamna tema, narančasta/plava/zelena/ljubičasta naglasna boja i prozračni/kompaktni razmaci. Postavke vrijede za samostalni javni HTML i ugrađene kratke kodove; **nikada ne mijenjaju službene CSV/XML stupce**.

Boje se odabiru iz validiranih, pristupačnih profila. Slobodan unos CSS-a i proizvoljnih URL-ova nije dopušten kako se ne bi otvorili XSS i CSS injection rizici. Stranica ostaje responzivna i podržava tipkovnicu i smanjene animacije.

### Ako stranica nije na WordPressu

Shortcode je WordPress funkcija i **neće se izvršiti** ako ga zalijepite u obični HTML, Joomlu ili Drupal. Na drugom CMS-u koristite BAZNA javni API i izravne CSV/XML poveznice, ili ugradite postojeću javnu HTML stranicu pomoću `iframe` kada to dopuštaju dopuštenja prikaza (`Content-Security-Policy: frame-ancestors` i `X-Frame-Options`). Iframe nije zamjena za službeni strojni format. Primjer HTML-a (zamijenite stvarnom provjerenom javnom adresom):

```html
<iframe
  src="https://primjer.hr/cjenik/"
  title="Javni BAZNA cjenik"
  loading="lazy"
  style="width:100%;min-height:700px;border:0"
></iframe>
```

Public HTML endpoint treba utvrditi na konkretnom hostingu i stranici prije korištenja. Ne kopirati javno nedostupne URL-ove iz administratorskog sučelja.

## Što je zakonski obvezno, a što vizualna nadogradnja

Na dan **17. 11. 2026.** mjerodavni su [NN 132/2026, br. 1583](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1583.html) i [NN 132/2026, br. 1584](https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1584.html).

- **Čl. 3. st. 1.–4. NN 132/26-1584:** javni cjenici robe/usluga s propisanim obveznim poljima i točnim redoslijedom; CSV za robu razlikuje naziv, šifru, marku, neto količinu, jedinicu mjere, maloprodajnu cijenu i status posebnog oblika prodaje, jediničnu cijenu, barkod, EAN i baznu cijenu; za usluge naziv, maloprodajnu cijenu/status posebnog oblika i baznu cijenu.
- **Čl. 6. st. 1., 6. i 7.:** javni **CSV ili XML** (oboje su korisna dodatna funkcionalnost), UTF-8 i CSV separator `;`; moguć dodatni ZIP/GZ.
- **Čl. 6. st. 2.–5.:** prilikom izmjene datoteke javni URL ostaje isti; službeni naziv datoteke uključuje trgovca, mjesto, vrstu cjenika, redni broj pohrane i vremensku oznaku; jedinstvena struktura po poslovnicama.
- **Čl. 3. st. 5.–7.:** promjene objaviti najkasnije do **11:00 na dan promjene**, stari javni cjenik čuvati najmanje **15 dana od zamjene**, odvojiti poslovnice ako im cijene nisu iste.
- **Čl. 5.:** tehnički dopustiti automatizirano preuzimanje bez login/CAPTCHA prepreka.
- **NN 132/26-1583, čl. 3.–6.:** prikazati točne i dokazive bazne cijene i njihove iznimke; ne zamijeniti ih najnižom cijenom prethodnih 30 dana ili današnjom redovnom cijenom.

**Važno:** HTML stranica, WP shortcode ili iframe ne zamjenjuju službene CSV/XML. Potvrda lokalnog generiranja datoteke ne dokazuje objavu preko HTTPS-a niti točnost povijesnih cijena.

## Automatizacija i kontrola

WordPress već ima automatizaciju nakon promjene cijene, dnevni raspored, evidenciju uspjeha/neuspjeha i ponovne pokušaje. Za vremenski pouzdanu objavu potreban je **stvarni poslužiteljski raspoređivač** (npr. hosting cron za WP-CLI ili drugi kompatibilni cron okidač). Osjetljive promjene ne smiju čekati slučajni posjet stranici. U administraciji BAZNA → Javni cjenik uključiti automatsko osvježavanje i nadzor objava.

Službeni snapshotovi različitih objava istog dana imaju različite redne brojeve i prikazuju se u arhivi ako su fizički pohranjeni; stariji kompatibilni dnevni arhiv prikazuje se zasebno. Provjeriti na stvarnom hostingu istodobne objave, originalne datoteke, 15-dnevni rok od zamjene te HTTP bot pristup.

**Kriterij prihvata:** test na konkretnoj WordPress instalaciji i domeni, dokumentirani poslovni podaci, izravan CSV/XML dohvat, automatsko zakazivanje i arhiva bez prebrisavanja zakonito čuvanih verzija. Bez toga nije opravdano objaviti softver kao potpuno pravno usklađen.
