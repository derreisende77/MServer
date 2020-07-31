/*
 * MediathekView
 * Copyright (C) 2020 A. Finkhaeuser
 */
package mServer.tool;

import de.mediathekview.mlib.tool.Log;

import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.net.SocketTimeoutException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.MediaType;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;

public class StatsUpload {

    private final static StatsUpload instance = new StatsUpload();

    private final OkHttpClient httpClient;

    public static final MediaType MEDIA_TYPE_TEXT = MediaType.parse("text/plain; charset=utf-8");

    private static final String STRING_ENV_KEY_ENABLED = "METRIC_ENABLED";

    public static final String STRING_TAG_KEY_SENDER = "sender";
    public static final String STRING_TAG_KEY_IMPORTLIVESTREAMS = "import_livestream";
    public static final String STRING_TAG_KEY_IMPORT1 = "import_1";
    public static final String STRING_TAG_KEY_IMPORT2 = "import_2";
    public static final String STRING_TAG_KEY_IMPORTAKT = "import_akt";
    public static final String STRING_TAG_KEY_IMPORTOLD = "import_old";

    public static final String STRING_METRIC_SENDERSUCHLAUF = "sendersuchlauf";
    public static final String STRING_METRIC_CRAWLSTAT = "suchlauf";
    public static final String STRING_METRIC_SUCHLAUFAKTIV = "suchlaufaktiv";

    public static final String STRING_METRIC_SENDERSUCHLAUF_KEY_LAUFZEIT = "laufzeit";
    public static final String STRING_METRIC_SENDERSUCHLAUF_KEY_ANZAHLFILME = "anzahlfilme";

    public static final String STRING_METRIC_CRAWLSTAT_KEY_ANZAHLFILMENEU = "anzahl_filmeneu";
    public static final String STRING_METRIC_CRAWLSTAT_KEY_ANZAHLFILMEGESAMT = "anzahl_filmegesamt";
    public static final String STRING_METRIC_CRAWLSTAT_KEY_ANZAHLFILMEDIFF = "anzahl_filmediff";
    public static final String STRING_METRIC_CRAWLSTAT_KEY_LAUFZEIT = "laufzeit";
    public static final String STRING_METRIC_CRAWLSTAT_KEY_ANZAHLLIVESTREAMS = "anzahl_livestreams";
    public static final String STRING_METRIC_CRAWLSTAT_KEY_ANZAHLIMPORT1 = "anzahl_import1";
    public static final String STRING_METRIC_CRAWLSTAT_KEY_ANZAHLIMPORT2 = "anzahl_import2";
    public static final String STRING_METRIC_CRAWLSTAT_KEY_ANZAHLAKTIMPORT = "anzahl_aktimport";
    public static final String STRING_METRIC_CRAWLSTAT_KEY_ANZAHLOLDIMPORT = "anzahl_oldimport";
    public static final String STRING_METRIC_CRAWLSTAT_KEY_ANZAHLFILMEORG = "anzahl_filmeorg";
    public static final String STRING_METRIC_CRAWLSTAT_KEY_ORGALTER = "orgalter";

    public static final String STRING_METRIC_SUCHLAUFAKTIV_KEY_SUCHLAUFAKTIV = "laeuft";


    private String globalTags = "";

    private boolean collectEnabled = false;
    private String telegrafUrl = "http://127.0.0.1:8095/telegraf";

    private long dataCrawlstatFilmeneu;
    private long dataCrawlstatFilmegesamt = 0;
    private long dataCrawlstatFilmediff = 0;
    private long dataCrawlstatLaufzeit;
    private long dataCrawlstatAnzahlLivestreams = 0;
    private long dataCrawlstatAnzahlImport1 = 0;
    private long dataCrawlstatAnzahlImport2 = 0;
    private long dataCrawlstatAnzahlAktImport = 0;
    private long dataCrawlstatAnzahlOldImport = 0;
    private long dataCrawlstatFilmeOrg = 0;
    private long dataCrawlstatOrgAlter = 0;

    private String statusCrawlstatImportLivestreams = "false";
    private String statusCrawlstatImport1 = "false";
    private String statusCrawlstatImport2 = "false";
    private String statusCrawlstatImportAkt = "false";
    private String statusCrawlstatImportOld = "false";

    private int tmpIndexImports = 1;

    public enum Data {
        CRAWLSTAT_FILMENEU,
        CRAWLSTAT_FILMEGESAMT,
        CRAWLSTAT_FILMEDIFF,
        CRAWLSTAT_LAUFZEIT,
        CRAWLSTAT_LIVESTREAMS,
        CRAWLSTAT_IMPORT,
        CRAWLSTAT_AKTIMPORT,
        CRAWLSTAT_OLDIMPORT,
        CRAWLSTAT_ORGLISTE,
        CRAWLSTAT_ORGALTER
    }


    private StatsUpload() {
        httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(10, 1, TimeUnit.SECONDS))
            .build();

        EnvManager envmgr = EnvManager.getInstance();

        collectEnabled = envmgr.env_metric_enabled;
        if(!collectEnabled) Log.sysLog("METRIC sammeln ist deaktiviert!!!!!!");
        if(envmgr.env_metric_url != "") {
            telegrafUrl = envmgr.env_metric_url;
        }
    }

    public static StatsUpload getInstance() {
        return instance;
    }

    public void catchSenderStat(
        String sender,
        long laufzeit,
        long anzahlfilme
    ) {
        if(!collectEnabled) return;

        String tags = globalTags
            + "," + STRING_TAG_KEY_SENDER + "=" + sender;
        String postBody = STRING_METRIC_SENDERSUCHLAUF + tags + " "
            + STRING_METRIC_SENDERSUCHLAUF_KEY_LAUFZEIT + "=" + laufzeit + "i,"
            + STRING_METRIC_SENDERSUCHLAUF_KEY_ANZAHLFILME + "=" + anzahlfilme + "i";

        //Log.sysLog("Debug postBody: " + postBody);
        sendData(postBody);
    }

    public void setData(StatsUpload.Data data_key, long value) {
        if(!collectEnabled) return;

        switch(data_key){
            case CRAWLSTAT_FILMENEU:
                dataCrawlstatFilmeneu = value;
                break;
            case CRAWLSTAT_FILMEGESAMT:
                dataCrawlstatFilmegesamt = value;
                break;
            case CRAWLSTAT_FILMEDIFF:
                dataCrawlstatFilmediff = value;
                break;
            case CRAWLSTAT_LAUFZEIT:
                dataCrawlstatLaufzeit = value;
                break;
            case CRAWLSTAT_LIVESTREAMS:
                dataCrawlstatAnzahlLivestreams = value;
                statusCrawlstatImportLivestreams = "true";
                break;
            case CRAWLSTAT_IMPORT:
                if(tmpIndexImports == 1) {
                    dataCrawlstatAnzahlImport1 = value;
                    statusCrawlstatImport1 = "true";
                    tmpIndexImports++;
                    break;
                }
                if(tmpIndexImports == 2) {
                    dataCrawlstatAnzahlImport2 = value;
                    statusCrawlstatImport2 = "true";
                    tmpIndexImports = 1;
                    break;
                }
                break;
            case CRAWLSTAT_AKTIMPORT:
                dataCrawlstatAnzahlAktImport = value;
                statusCrawlstatImportAkt = "true";
                break;
            case CRAWLSTAT_OLDIMPORT:
                dataCrawlstatAnzahlOldImport = value;
                statusCrawlstatImportOld = "true";
                break;
            case CRAWLSTAT_ORGLISTE:
                dataCrawlstatFilmeOrg = value;
                // statusCrawlstatDiffErzeugt = "true";
                break;
            case CRAWLSTAT_ORGALTER:
                dataCrawlstatOrgAlter = value;
                break;
            default:
                Log.sysLog("WARNING: StatsUpload - Unbekannter Key");
        }
    }

    public void sendCrawlStat() {
        if(!collectEnabled) return;

        String tags = globalTags
            + "," + STRING_TAG_KEY_IMPORTLIVESTREAMS + "=" + statusCrawlstatImportLivestreams
            + "," + STRING_TAG_KEY_IMPORT1 + "=" + statusCrawlstatImport1
            + "," + STRING_TAG_KEY_IMPORT2 + "=" + statusCrawlstatImport2
            + "," + STRING_TAG_KEY_IMPORTAKT + "=" + statusCrawlstatImportAkt
            + "," + STRING_TAG_KEY_IMPORTOLD + "=" + statusCrawlstatImportOld
            ;
        String postBody = STRING_METRIC_CRAWLSTAT + tags + " "
            + STRING_METRIC_CRAWLSTAT_KEY_ANZAHLFILMENEU + "=" + dataCrawlstatFilmeneu + "i,"
            + STRING_METRIC_CRAWLSTAT_KEY_ANZAHLFILMEGESAMT + "=" + dataCrawlstatFilmegesamt + "i,"
            + STRING_METRIC_CRAWLSTAT_KEY_ANZAHLFILMEDIFF + "=" + dataCrawlstatFilmediff + "i,"
            + STRING_METRIC_CRAWLSTAT_KEY_LAUFZEIT + "=" + dataCrawlstatLaufzeit + "i,"
            + STRING_METRIC_CRAWLSTAT_KEY_ANZAHLLIVESTREAMS + "=" + dataCrawlstatAnzahlLivestreams + "i,"
            + STRING_METRIC_CRAWLSTAT_KEY_ANZAHLIMPORT1 + "=" + dataCrawlstatAnzahlImport1 + "i,"
            + STRING_METRIC_CRAWLSTAT_KEY_ANZAHLIMPORT2 + "=" + dataCrawlstatAnzahlImport2 + "i,"
            + STRING_METRIC_CRAWLSTAT_KEY_ANZAHLAKTIMPORT + "=" + dataCrawlstatAnzahlAktImport + "i,"
            + STRING_METRIC_CRAWLSTAT_KEY_ANZAHLOLDIMPORT + "=" + dataCrawlstatAnzahlOldImport + "i,"
            + STRING_METRIC_CRAWLSTAT_KEY_ANZAHLFILMEORG + "=" + dataCrawlstatFilmeOrg + "i,"
            + STRING_METRIC_CRAWLSTAT_KEY_ORGALTER + "=" + dataCrawlstatOrgAlter + "i"
            ;

        // Log.sysLog("Debug postBody: " + postBody);
        sendData(postBody);
    }

    public void sendCrawlStart() {
        if(!collectEnabled) return;

        String tags = globalTags;
        String postBody = STRING_METRIC_SUCHLAUFAKTIV + tags + " "
            + STRING_METRIC_SUCHLAUFAKTIV_KEY_SUCHLAUFAKTIV +  "=";

        String postBodyValue = postBody + "false";
        sendData(postBodyValue);
        postBodyValue = postBody + "true";
        sendData(postBodyValue);
    }

    public void sendCrawlStopp() {
        if(!collectEnabled) return;
        String tags = globalTags;
        String postBody = STRING_METRIC_SUCHLAUFAKTIV + tags + " "
            + STRING_METRIC_SUCHLAUFAKTIV_KEY_SUCHLAUFAKTIV +  "=";

        String postBodyValue = postBody + "true";
        sendData(postBodyValue);
        postBodyValue = postBody + "false";
        sendData(postBodyValue);
    }

    private void sendData(String body) {
        Request request = new Request.Builder()
        .url(telegrafUrl)
        .post(RequestBody.create(MEDIA_TYPE_TEXT, body))
        .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.sysLog("WARNING: Fehler aufgetreten beim senden der Stats an den Telegraf Server. " + e.getMessage());
            }

            @Override public void onResponse(Call call, Response response) {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) Log.sysLog("Request zu telegraf fehlgeschlagen. Response: " + response );
                } catch(Exception ex) {
                    Log.sysLog("Exception aufgetreten beim senden der Stats an den Telegraf Server. " + ex.toString() );
                }
            }
        });
    }



}
