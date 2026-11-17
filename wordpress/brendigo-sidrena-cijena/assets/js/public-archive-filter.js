/* BAZNA public archive: date-filtered navigation without hidden or altered CSV/XML URLs. */
(function () {
  "use strict";
  function attach(root) {
    if (root.dataset.baznaFilterReady === "1") return;
    root.dataset.baznaFilterReady = "1";
    var date = root.querySelector("[data-bazna-archive-date]");
    var reset = root.querySelector("[data-bazna-archive-reset]");
    var count = root.querySelector("[data-bazna-archive-count]");
    var rows = Array.prototype.slice.call(root.querySelectorAll(".sidrena-arhiva li[data-bazna-date]"));
    if (!date || !reset || !count || rows.length === 0) return;
    function apply() {
      var selected = date.value;
      var visible = 0;
      rows.forEach(function (row) {
        var match = !selected || row.getAttribute("data-bazna-date") === selected;
        row.hidden = !match;
        if (match) visible++;
      });
      count.textContent = selected
        ? (visible ? visible + " datoteka za odabrani dan" : "Nema datoteka za odabrani dan")
        : visible + " dostupnih datoteka";
      reset.disabled = !selected;
    }
    date.addEventListener("change", apply);
    reset.addEventListener("click", function () {
      date.value = "";
      apply();
      date.focus();
    });
    apply();
  }
  function init() {
    document.querySelectorAll("[data-bazna-archive]").forEach(attach);
  }
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
}());
