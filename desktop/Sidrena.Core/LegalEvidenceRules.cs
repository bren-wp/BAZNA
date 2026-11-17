using System.Globalization;

namespace Sidrena.Core;

/// <summary>
/// Structured NN 132/2026 evidence checks shared with WP/PHP semantics.
/// A passing check only validates recorded fields, NOT the underlying
/// historical invoice or merchant's legal compliance.
/// </summary>
public static class LegalEvidenceRules
{
    public const string LegacyFmcg = "legacy_fmcg";
    public const string General = "general_goods_services";

    public static string? Issue(string kind,string? scope,string? method,string? date,string? source)
    {
        if(kind is not ("Proizvod" or "Usluga"))
            return "vrsta";
        if(string.IsNullOrWhiteSpace(source) || source.Trim().Length<4 || source.Length>500)
            return "historical_source";
        if(scope is not (LegacyFmcg or General) || (kind=="Usluga" && scope==LegacyFmcg))
            return "reference_scope";
        if(!DateOnly.TryParseExact(date,"yyyy-MM-dd",CultureInfo.InvariantCulture,
            DateTimeStyles.None,out var actual))
            return "sidrena_datum";
        var reference=scope==LegacyFmcg?new DateOnly(2025,5,2):new DateOnly(2026,9,10);
        var today=DateOnly.FromDateTime(TimeZoneInfo.ConvertTime(DateTimeOffset.UtcNow,
            TimeZoneInfo.FindSystemTimeZoneById("Europe/Zagreb")).DateTime);
        if(actual<reference || actual>today) return "sidrena_datum";

        return method switch
        {
            "reference_day" when actual==reference => null,
            "first_offer" when actual>reference => null,
            "discount_lowest_30d" when scope==LegacyFmcg => null,
            "discount_previous_retail" when scope==General => null,
            _ => "historical_method"
        };
    }
}
