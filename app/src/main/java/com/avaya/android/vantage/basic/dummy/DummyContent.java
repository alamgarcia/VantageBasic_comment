package com.avaya.android.vantage.basic.dummy;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.avaya.android.vantage.basic.model.CallData;
import com.avaya.android.vantage.basic.model.ContactData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class DummyContent {

    /**
     * An array of sample (dummy) items.
     */
    public static final List<CallData> RECENT_CALL_ITEMS = new ArrayList<>();

    /**
     * An array of sample (dummy) items.
     */
    public static final List<ContactData> ITEMS = new ArrayList<>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    public static final Map<String, CallData> RECENT_ITEM_MAP = new HashMap<String, CallData>();
    /**
     * A map of sample (dummy) items, by ID.
     */
    public static final Map<String, ContactData> ITEM_MAP = new HashMap<String, ContactData>();

    private static final int COUNT = 25;

    static String names[];
    static String addresses[];
    static String firstNames[];
    static String lastNames[];

    static {
        names = new String[]{"koby hershkovitz", "david ziff", "moshe bol", "sarit sabo", "donald trump", "aaabb", "abaa", "aacc"};
        firstNames = new String[]{"koby", "david", "moshe", "sarit", "donald", "aaabb", "abaa", "aacc"};
        lastNames = new String[]{"koby", "david", "moshe", "sarit", "donald", "aaabb", "abaa", "aacc"};
        addresses = new String[]{"refael eitan 14", "los angeles", "manhatan NYC", "petah tikva", "asda asdasp", "l1", "l2"};
    }

    public static final ContactData.Category[] CATEGORIES = new ContactData.Category[]{ContactData.Category.ALL, ContactData.Category.LOCAL, ContactData.Category.ENTERPRISE};

    static {
        // Add some sample items.
        for (int i = 1; i <= COUNT; i++) {
            addItem(createDummyItem(i, CATEGORIES[i % CATEGORIES.length]));
        }
    }

    private static void addRecentItem(CallData item) {
        RECENT_CALL_ITEMS.add(item);
        RECENT_ITEM_MAP.put(item.mName, item);
    }

    /**
     * Adding item to map
     *
     * @param item {@link ContactData}
     */
    private static void addItem(ContactData item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.mName, item);
    }

    /**
     * Creating dummy item
     *
     * @param position
     * @param category {@link ContactData.Category}
     * @return {@link ContactData}
     */
    private static ContactData createDummyItem(int position, ContactData.Category category) {
        List<ContactData.PhoneNumber> phones = new ArrayList<>();
        phones.add(new ContactData.PhoneNumber("426144", ContactData.PhoneType.HOME, true, null));
        return new ContactData(names[position % names.length] + Integer.toString(position), "", "", null, position % 2 == 1, addresses[position % addresses.length], "city", "position", "Avaya", phones, category, UUID.randomUUID().toString(), null, null, true, "avaya@avaya.com", "", "", "", "");
    }

    /**
     * Boolean value which shows is application download from store.
     *
     * @param context
     */
    public static boolean isStoreVersion(Context context) {
        boolean result = false;
        try {
            PackageManager pm = context.getPackageManager();
            if (pm != null) {
                String installer = pm.getInstallerPackageName(context.getPackageName());
                result = !TextUtils.isEmpty(installer);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return result;
    }
}
