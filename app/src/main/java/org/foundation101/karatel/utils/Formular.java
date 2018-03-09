package org.foundation101.karatel.utils;

import com.google.android.gms.location.places.PlaceLikelihoodBuffer;

import org.foundation101.karatel.manager.GoogleApiManager;

/**
 * Created by Dima on 19.08.2017.
 */

public interface Formular {
    /*int getMode();
    void setMode(int mode);*/

    boolean changesMade();
    void setChangesMade(boolean changesMade);

    void onEvidenceRemoved();

    void validatePunishButton();
    void validateSaveButton();

    GoogleApiManager getGoogleManager();
    void onLocationChanged(double lat, double lon);
    void onAddressesReady(PlaceLikelihoodBuffer places);
}
