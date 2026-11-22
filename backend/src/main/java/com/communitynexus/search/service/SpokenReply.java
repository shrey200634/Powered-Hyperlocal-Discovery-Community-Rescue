package com.communitynexus.search.service;

import com.communitynexus.common.Language;
import com.communitynexus.inventory.dto.NearbyStoreResult;
import org.springframework.stereotype.Component;


@Component
public class SpokenReply {

    public String found(NearbyStoreResult top, Language lang) {
        String product = top.products().isEmpty() ? "" : top.products().get(0).name();
        String distance = distance(top.distanceMeters(), lang);

        if (lang == Language.EN) {
            return top.storeName() + ", " + distance + " away, has " + product + "."
                    + (top.openNow() ? "" : " It is closed right now.");
        }
        return top.storeName() + ", " + distance + " door, " + product + " hai."
                + (top.openNow() ? "" : " Abhi dukaan band hai.");
    }

    public String notFound(Language lang) {
        return lang == Language.EN
                ? "No shop nearby has listed this. Shall I ask the bazaar?"
                : "Paas ki kisi dukaan ne yeh list nahi kiya. Kya main bazaar se poochhun?";
    }

    public String didNotUnderstand(Language lang) {
        return lang == Language.EN
                ? "Sorry, I did not understand. Please say it again."
                : "Maaf kijiye, samajh nahi aaya. Phir se boliye.";
    }

    private static String distance(int meters, Language lang) {
        if (meters >= 1000) {
            String km = String.format("%.1f", meters / 1000.0);
            return km + " km";
        }
        int rounded = Math.max(50, (int) (Math.round(meters / 50.0) * 50));   // "300 metre", not "287 metre"
        return rounded + (lang == Language.EN ? " metres" : " metre");
    }
}