/*
 * MediathekView
 * Copyright (C) 2008 W. Xaver
 * W.Xaver[at]googlemail.com
 * http://zdfmediathk.sourceforge.net/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mediathekServer.search;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import mediathek.MediathekNoGui;
import mediathek.tool.DatumZeit;
import mediathek.tool.GuiFunktionen;
import mediathekServer.daten.MS_DatenSuchen;
import mediathekServer.tool.MS_Daten;
import mediathekServer.tool.MS_DatumZeit;
import mediathekServer.tool.MS_Konstanten;
import mediathekServer.tool.MS_Log;

public class MS_FilmeSuchen {

    public void filmeSuchen(MS_DatenSuchen aktDatenSuchen) {
        String filmDateiName = aktDatenSuchen.getZielDateiName();
        String filmDateiPfad = MS_Daten.getBasisVerzeichnis();
        String sender[] = arrLesen(aktDatenSuchen.arr[MS_Konstanten.SUCHEN_SENDER_NR].trim());
        try {
            String importUrl = MS_Daten.system[MS_Konstanten.SYSTEM_IMPORT_URL_NR].toString();
            new MediathekNoGui(MS_Daten.getBasisVerzeichnis(), aktDatenSuchen.allesLaden(), GuiFunktionen.addsPfad(filmDateiPfad, filmDateiName),
                    importUrl, MS_Daten.getUserAgent(), getLogDatei()).serverStarten(sender);
            MS_Log.systemMeldung("Filme suchen Ok");
            return;
        } catch (Exception ex) {
            MS_Log.fehlerMeldung(636987308, MS_FilmeSuchen.class.getName(), "filmeSuchen", ex);
        }
        // war wohl nix
        MS_Log.fehlerMeldung(830469743, MS_FilmeSuchen.class.getName(), "filmeSuchen");
    }

    private File getLogDatei() {
        File logfile = null;
        String logPfad, logFileName;
        try {
            logPfad = MS_Daten.getLogVerzeichnis();
            // prüfen obs geht
            logFileName = GuiFunktionen.addsPfad(logPfad, MS_DatumZeit.getJetztLogDatei() + MS_Konstanten.LOG_FILE_NAME_MV);
            logfile = new File(logFileName);
            if (!logfile.exists()) {
                boolean b = new File(logPfad).mkdirs();
                if (!logfile.createNewFile()) {
                    logfile = null;
                }
            }
            return logfile;
        } catch (Exception ex) {
            System.out.println("Logfile MV anlegen: " + ex.getMessage()); // hier muss direkt geschrieben werden
            return null;
        }
    }

    private String[] arrLesen(String s) {
        ArrayList<String> arr = new ArrayList<String>();
        String tmp = "";
        s = s.trim();
        if (s.equals("")) {
            return null;
        }
        for (int i = 0; i < s.length(); ++i) {
            if (s.charAt(i) == ',') {
                if (!tmp.equals("")) {
                    arr.add(tmp);
                }
                tmp = "";
            } else {
                tmp += s.charAt(i);
            }
        }
        if (!tmp.equals("")) {
            arr.add(tmp);
        }
        return arr.toArray(new String[]{});
    }
}
