package layout;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.example.dnk.punisher.Globals;
import com.example.dnk.punisher.PunisherUser;
import com.example.dnk.punisher.R;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class ProfileFragment extends Fragment {

    ViewGroup memberEmail;
    SharedPreferences preferences;
    PunisherUser user;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = getUser();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);
        memberEmail = (ViewGroup) v.findViewById(R.id.profile_email);
        ((EditText)memberEmail.getChildAt(2)).setText(user.email);

        return v;
    }

    public PunisherUser getUser(){
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
        boolean b = preferences.contains(Globals.USER_SURNAME);
        PunisherUser result = new PunisherUser(
                preferences.getString(Globals.USER_EMAIL, ""),
                preferences.getString(Globals.USER_PASSWORD, ""),
                preferences.getString(Globals.USER_SURNAME, ""),
                preferences.getString(Globals.USER_NAME, ""),
                preferences.getString(Globals.USER_SECOND_NAME, ""),
                preferences.getString(Globals.USER_PHONE, "")
        );
        return result;
    }

    public void saveUser(PunisherUser user){
        preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        preferences.edit()
                .putString(Globals.USER_EMAIL, user.email)
                .putString(Globals.USER_PASSWORD, user.password)
                .putString(Globals.USER_SURNAME, user.surname)
                .putString(Globals.USER_NAME, user.name)
                .putString(Globals.USER_SECOND_NAME, user.secondName)
                .putString(Globals.USER_PHONE, user.phone).apply();

    }
}
