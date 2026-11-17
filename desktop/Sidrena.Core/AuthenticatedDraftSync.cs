using System.Globalization;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;

namespace Sidrena.Core;

public sealed record DraftSyncRow(string Id,string Name,string Sku,string Kind,string Outlet,string CurrentPrice);
public sealed record DraftSyncSnapshot(string Revision,IReadOnlyList<DraftSyncRow> Rows);

public sealed class DraftSyncConflictException : InvalidOperationException
{
    public DraftSyncConflictException() : base("WordPress nacrt je izmijenjen. Ponovno ga preuzmite i odlučite koje promjene zadržati.") {}
}

/// <summary>
/// Authenticated WordPress draft exchange. Never publishes a legal CSV/XML feed.
/// WordPress Application Password is caller-supplied per operation, not persisted.
/// </summary>
public static class AuthenticatedDraftSync
{
    private const int MaximumBytes=1048576;
    private const string DiscoverySuffix="/wp-json/brendigo-sidrena/v1/cjenici";

    public static Uri Endpoint(string site)
    {
        var discovery=OfficialPluginDiscovery.Endpoint(site);
        var path=discovery.AbsolutePath;
        if(!path.EndsWith(DiscoverySuffix,StringComparison.Ordinal))
            throw new ArgumentException("Nepodržana WordPress adresa.",nameof(site));
        return new UriBuilder(discovery)
        {
            Path=path[..^DiscoverySuffix.Length]+"/wp-json/brendigo-bazna/v1/sync/draft",
            Query="",Fragment=""
        }.Uri;
    }

    public static async Task<DraftSyncSnapshot> PullAsync(
        string site,string username,string applicationPassword,HttpClient? client=null,
        CancellationToken cancellationToken=default)
    {
        var endpoint=Endpoint(site);
        if(client is null)
        {
            using var handler=SecureHandler();
            using var owned=new HttpClient(handler){Timeout=TimeSpan.FromSeconds(15)};
            return await PullAsync(site,username,applicationPassword,owned,cancellationToken).ConfigureAwait(false);
        }
        using var request=NewRequest(HttpMethod.Get,endpoint,username,applicationPassword);
        using var response=await client.SendAsync(request,HttpCompletionOption.ResponseHeadersRead,cancellationToken).ConfigureAwait(false);
        RequireSuccess(response);
        return Parse(await ReadBoundedAsync(response,cancellationToken).ConfigureAwait(false));
    }

    /// <summary>
    /// Change one WordPress draft row's current price with a mandatory expected
    /// revision. The remote server never accepts a verified historical flag.
    /// </summary>
    public static async Task<string> PushCurrentPriceAsync(
        string site,string username,string applicationPassword,string expectedRevision,
        DraftSyncRow existing,decimal price,HttpClient? client=null,
        CancellationToken cancellationToken=default)
    {
        if(!ValidRevision(expectedRevision) || !long.TryParse(existing.Id,out var id) || id<=0 ||
           string.IsNullOrWhiteSpace(existing.Name) || existing.Name.Length>240 ||
           price<0m || price>999999999m || decimal.Round(price,2)!=price)
            throw new ArgumentException("Nevaljana verzija nacrta, redak ili iznos.");
        var endpoint=Endpoint(site);
        if(client is null)
        {
            using var handler=SecureHandler();
            using var owned=new HttpClient(handler){Timeout=TimeSpan.FromSeconds(15)};
            return await PushCurrentPriceAsync(site,username,applicationPassword,expectedRevision,
                existing,price,owned,cancellationToken).ConfigureAwait(false);
        }
        var body=new
        {
            expected_revision=expectedRevision,
            operation="upsert",
            id=existing.Id,
            row=new {naziv=existing.Name,cijena_redovna=price.ToString("0.00",CultureInfo.InvariantCulture)}
        };
        using var request=NewRequest(HttpMethod.Post,endpoint,username,applicationPassword);
        request.Content=new StringContent(JsonSerializer.Serialize(body),Encoding.UTF8,"application/json");
        using var response=await client.SendAsync(request,HttpCompletionOption.ResponseHeadersRead,cancellationToken).ConfigureAwait(false);
        RequireSuccess(response);
        var json=await ReadBoundedAsync(response,cancellationToken).ConfigureAwait(false);
        using var doc=JsonDocument.Parse(json);
        var root=doc.RootElement;
        if(root.ValueKind!=JsonValueKind.Object ||
           !root.TryGetProperty("ok",out var ok) || ok.ValueKind!=JsonValueKind.True ||
           !root.TryGetProperty("published",out var publication) || publication.ValueKind!=JsonValueKind.False ||
           !root.TryGetProperty("revision",out var next) || next.ValueKind!=JsonValueKind.String ||
           !ValidRevision(next.GetString()))
            throw new InvalidDataException("WordPress nije potvrdio privatno spremanje.");
        return next.GetString()!;
    }

    private static SocketsHttpHandler SecureHandler() => new()
    {
        AllowAutoRedirect=false,
        UseProxy=false,
        AutomaticDecompression=DecompressionMethods.GZip|DecompressionMethods.Deflate,
        ConnectTimeout=TimeSpan.FromSeconds(6),
        ConnectCallback=PublicNetworkAddress.ConnectAsync
    };

    private static HttpRequestMessage NewRequest(HttpMethod method,Uri endpoint,string username,string secret)
    {
        if(string.IsNullOrWhiteSpace(username) || username.Length>128 ||
           username.Contains(':') || username.Contains('\r') || username.Contains('\n') ||
           string.IsNullOrWhiteSpace(secret) || secret.Length>128 ||
           secret.Contains('\r') || secret.Contains('\n'))
            throw new ArgumentException("Unesite korisničko ime i zasebnu WordPress aplikacijsku lozinku.");
        if(endpoint.Scheme!=Uri.UriSchemeHttps || !string.IsNullOrEmpty(endpoint.UserInfo))
            throw new InvalidOperationException("Autentificirani pozivi zahtijevaju HTTPS.");
        var request=new HttpRequestMessage(method,endpoint);
        var credentials=Encoding.UTF8.GetBytes(username+":"+secret);
        request.Headers.Authorization=new AuthenticationHeaderValue("Basic",Convert.ToBase64String(credentials));
        request.Headers.CacheControl=new CacheControlHeaderValue{NoStore=true};
        request.Headers.Accept.ParseAdd("application/json");
        return request;
    }

    private static void RequireSuccess(HttpResponseMessage response)
    {
        if(response.StatusCode==HttpStatusCode.Conflict)
            throw new DraftSyncConflictException();
        if((int)response.StatusCode is >=300 and <400)
            throw new HttpRequestException("Preusmjeravanje s aplikacijskom lozinkom nije dopušteno.");
        if(response.StatusCode==HttpStatusCode.Unauthorized || response.StatusCode==HttpStatusCode.Forbidden)
            throw new UnauthorizedAccessException("Provjerite WordPress ovlasti i aplikacijsku lozinku.");
        response.EnsureSuccessStatusCode();
    }

    private static async Task<byte[]> ReadBoundedAsync(HttpResponseMessage response,CancellationToken token)
    {
        if(response.Content.Headers.ContentLength is >MaximumBytes)
            throw new InvalidDataException("Odgovor je prevelik.");
        await using var stream=await response.Content.ReadAsStreamAsync(token).ConfigureAwait(false);
        using var output=new MemoryStream();
        var buffer=new byte[8192];
        int n;
        while((n=await stream.ReadAsync(buffer,token).ConfigureAwait(false))>0)
        {
            if(output.Length+n>MaximumBytes)throw new InvalidDataException("Odgovor je prevelik.");
            output.Write(buffer,0,n);
        }
        return output.ToArray();
    }

    private static bool ValidRevision(string? value) =>
        value is {Length:64} && value.All(c=>(c>='0'&&c<='9')||(c>='a'&&c<='f'));

    public static DraftSyncSnapshot Parse(byte[] input)
    {
        if(input is null || input.Length>MaximumBytes)throw new InvalidDataException("Neispravan odgovor.");
        using var doc=JsonDocument.Parse(input);
        var root=doc.RootElement;
        if(root.ValueKind!=JsonValueKind.Object ||
           !root.TryGetProperty("schema",out var schema) || schema.GetInt32()!=1 ||
           !root.TryGetProperty("mode",out var mode) || mode.GetString()!="standalone_draft" ||
           !root.TryGetProperty("revision",out var revision) || !ValidRevision(revision.GetString()) ||
           !root.TryGetProperty("rows",out var rows) || rows.ValueKind!=JsonValueKind.Array ||
           rows.GetArrayLength()>5000)
            throw new InvalidDataException("Nepodržan BAZNA privatni nacrt.");
        var list=new List<DraftSyncRow>();
        foreach(var row in rows.EnumerateArray())
        {
            if(row.ValueKind!=JsonValueKind.Object)throw new InvalidDataException("Neispravan redak.");
            string Field(string key)
            {
                if(!row.TryGetProperty(key,out var value) || value.ValueKind!=JsonValueKind.String)
                    return "";
                var text=value.GetString()??"";
                if(text.Length>2048)throw new InvalidDataException("Predugačak podatak.");
                return text;
            }
            var id=Field("id");
            if(!long.TryParse(id,out var number) || number<=0 || Field("naziv")=="" )
                throw new InvalidDataException("Neispravan identifikator ili naziv.");
            list.Add(new DraftSyncRow(id,Field("naziv"),Field("sku"),Field("vrsta"),
                Field("poslovnica"),Field("cijena_redovna")));
        }
        return new DraftSyncSnapshot(revision.GetString()!,list);
    }
}
