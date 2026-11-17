<?php

declare(strict_types=1);

namespace Drupal\bazna\Form;

use Brendigo\Bazna\Core;
use Drupal\Core\Form\FormBase;
use Drupal\Core\Form\FormStateInterface;

/**
 * Native Drupal Form API editor: CSRF, ACL through routing and checked writes.
 */
final class CatalogForm extends FormBase {

  public function getFormId(): string {
    return 'bazna_catalog_form';
  }

  private function core(): Core {
    require_once dirname(__DIR__, 2) . '/vendor/Core.php';
    $core = new Core((string) (getenv('BAZNA_DRUPAL_PRIVATE_DIR') ?: ''),
      (string) (getenv('BAZNA_DRUPAL_PUBLIC_URL') ?: ''));
    $core->ensurePrivate();
    return $core;
  }

  public function buildForm(array $form, FormStateInterface $form_state): array {
    try {
      $core = $this->core();
      $draft = $core->read('draft.json');
      $published = $core->read('published.json');
    }
    catch (\Throwable $error) {
      $form['error'] = ['#markup' => '<p>BAZNA nije dostupna. Provjerite privatnu mapu i HTTPS konfiguraciju.</p>'];
      return $form;
    }
    $form['#tree'] = TRUE;
    $form['#attached']['library'][] = 'bazna/admin';
    $form['introduction'] = [
      '#markup' => '<p><strong>BAZNA</strong> · Uredite nacrt, provjerite izvornu evidenciju i zasebno objavite cjenik. '
        . 'Nijedna bazna cijena ne izvodi se automatski iz današnje cijene. '
        . 'Službeni CSV/XML zahtijeva konfiguriran valjan OIB ili matični broj.</p>'
        . '<p>Posljednja javna objava: ' . htmlspecialchars((string) ($published['published_at'] ?? 'nije objavljeno'), ENT_QUOTES, 'UTF-8') . '</p>',
    ];
    $groups = [
      'outlets' => ['label' => 'Poslovnice', 'fields' => ['code', 'name', 'address']],
      'products' => ['label' => 'Proizvodi', 'fields' => [
        'id', 'naziv', 'sku', 'marka', 'neto_kolicina', 'jedinica_mjere',
        'ean_barkod', 'barkod', 'maloprodajna_cijena', 'sidrena_cijena',
        'sidrena_datum', 'cijena_jedinice_mjere', 'naziv_posebnog_oblika',
        'dostupnost', 'slika_url', 'outlet',
      ]],
      'services' => ['label' => 'Usluge', 'fields' => [
        'id', 'naziv', 'maloprodajna_cijena', 'sidrena_cijena',
        'sidrena_datum', 'naziv_posebnog_oblika', 'outlet',
      ]],
      'on_request' => ['label' => 'Usluge s cijenom na upit', 'fields' => [
        'id', 'naziv', 'kriteriji', 'outlet',
      ]],
    ];
    foreach ($groups as $key => $group) {
      $form['catalog'][$key] = [
        '#type' => 'details', '#title' => $group['label'],
        '#open' => $key === 'products' || $key === 'outlets',
        '#tree' => TRUE,
      ];
      $rows = $draft[$key] ?? [];
      if (!is_array($rows)) $rows = [];
      // Always provide one blank row for new entries, without JavaScript.
      $rows[] = [];
      foreach ($rows as $number => $row) {
        if (!is_array($row)) $row = [];
        $form['catalog'][$key][$number] = [
          '#type' => 'details',
          '#title' => ($row['naziv'] ?? $row['name'] ?? '') ?: ($key . ' #' . ($number + 1)),
          '#open' => $number === count($rows) - 1,
        ];
        foreach ($group['fields'] as $field) {
          $form['catalog'][$key][$number][$field] = [
            '#type' => $field === 'sidrena_datum' ? 'date' : 'textfield',
            '#title' => str_replace('_', ' ', ucfirst($field)),
            '#default_value' => (string) ($row[$field] ?? ''),
            '#maxlength' => $field === 'naziv' ? 240 : 800,
          ];
        }
        if ($key === 'products' || $key === 'services') {
          $form['catalog'][$key][$number]['historical_verified'] = [
            '#type' => 'checkbox',
            '#title' => 'Potvrđujem da postoji izvorna povijesna evidencija bazne cijene.',
            '#default_value' => !empty($row['historical_verified']),
          ];
        }
        $form['catalog'][$key][$number]['delete'] = [
          '#type' => 'checkbox',
          '#title' => 'Ukloni ovu stavku iz nacrta',
          '#default_value' => FALSE,
        ];
      }
    }
    $form['actions'] = ['#type' => 'actions'];
    $form['actions']['save'] = [
      '#type' => 'submit', '#value' => 'Spremi nacrt',
      '#button_type' => 'primary',
    ];
    $form['actions']['publish'] = [
      '#type' => 'submit', '#value' => 'Spremi i objavi cjenik',
      '#submit' => ['::submitForm'], '#button_type' => 'danger',
      '#name' => 'publish',
    ];
    return $form;
  }

  public function validateForm(array &$form, FormStateInterface $form_state): void {
    try {
      $form_state->set('bazna_clean_draft',
        Core::validateCatalog($this->collect($form_state)));
    }
    catch (\Throwable $error) {
      $form_state->setErrorByName('catalog',
        'Provjerite naziv, poslovnicu, iznos, datum i potvrdu povijesne cijene: ' . $error->getMessage());
    }
  }

  private function collect(FormStateInterface $form_state): array {
    $input = $form_state->getValue('catalog') ?: [];
    $draft = ['outlets' => [], 'products' => [], 'services' => [], 'on_request' => []];
    foreach ($draft as $collection => $unused) {
      foreach (($input[$collection] ?? []) as $row) {
        if (!is_array($row) || !empty($row['delete'])) continue;
        unset($row['delete']);
        if ($collection === 'outlets') {
          if (trim((string) ($row['code'] ?? '')) === '' &&
            trim((string) ($row['name'] ?? '')) === '') continue;
        }
        elseif (trim((string) ($row['naziv'] ?? '')) === '') {
          continue;
        }
        if (in_array($collection, ['products', 'services'], TRUE)) {
          $row['historical_verified'] = !empty($row['historical_verified']);
        }
        $draft[$collection][] = $row;
      }
    }
    return $draft;
  }

  public function submitForm(array &$form, FormStateInterface $form_state): void {
    try {
      $core = $this->core();
      $draft = $form_state->get('bazna_clean_draft');
      if (!is_array($draft)) throw new \RuntimeException('Nacrt nije validiran.');
      $core->saveDraft($draft);
      $button = $form_state->getTriggeringElement();
      if (($button['#name'] ?? '') === 'publish') {
        $core->publish();
        $this->messenger()->addStatus('BAZNA: provjereni nacrt je javno objavljen.');
      }
      else {
        $this->messenger()->addStatus('BAZNA: nacrt je spremljen, nije javno objavljen.');
      }
    }
    catch (\Throwable $error) {
      \Drupal::logger('bazna')->error('Catalog operation failed: @type', ['@type' => get_class($error)]);
      $this->messenger()->addError('Objava nije uspjela. Provjerite valjanost podataka i poslužiteljski dnevnik.');
    }
  }
}
