package com.telegram.electric.core.tdlib;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import org.drinkless.tdlib.JsonClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Owns the single TDLib JSON client, authorization updates, and the visible group chat cache.
 * Message sending is intentionally not exposed by this class.
 */
public final class TdClientManager {
    public enum State {
        DISCONNECTED,
        CONNECTING,
        NEEDS_API_CREDENTIALS,
        WAIT_PHONE,
        WAIT_CODE,
        WAIT_PASSWORD,
        WAIT_EMAIL_ADDRESS,
        WAIT_EMAIL_CODE,
        WAIT_DEVICE_CONFIRMATION,
        WAIT_REGISTRATION,
        READY,
        CLOSED,
        ERROR
    }

    public interface Listener {
        void onStateChanged(State state, String details);
        void onChatsChanged(List<Chat> chats);
    }

    public static final class Chat {
        public final long id;
        public final String title;
        public final long order;

        Chat(long id, String title, long order) {
            this.id = id;
            this.title = title;
            this.order = order;
        }
    }

    private static final int CHAT_LOAD_LIMIT = 100;
    private static final int MAX_CHAT_LOAD_BATCHES = 20;
    private static volatile TdClientManager instance;

    public static TdClientManager getInstance(Context context) {
        if (instance == null) {
            synchronized (TdClientManager.class) {
                if (instance == null) {
                    instance = new TdClientManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private final Context context;
    private final SecureConfigStore configStore;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicLong nextExtra = new AtomicLong();
    private final Map<Long, MutableChat> chats = new HashMap<>();
    private final Map<String, String> pendingChatLoads = new HashMap<>();
    private final Map<String, Integer> chatLoadPages = new HashMap<>();

    private volatile Listener listener;
    private volatile State state = State.DISCONNECTED;
    private volatile String details = "";
    private volatile int clientId = -1;
    private volatile boolean started;
    private volatile boolean receiving;
    private volatile boolean awaitingParameters;
    private volatile boolean parametersSubmitted;

    private TdClientManager(Context context) {
        this.context = context;
        this.configStore = new SecureConfigStore(context);
    }

    public synchronized void setListener(Listener listener) {
        this.listener = listener;
        if (listener != null) {
            final State currentState = state;
            final String currentDetails = details;
            final List<Chat> currentChats = snapshotChats();
            mainHandler.post(() -> {
                Listener current = this.listener;
                if (current != null) {
                    current.onStateChanged(currentState, currentDetails);
                    current.onChatsChanged(currentChats);
                }
            });
        }
    }

    public State getState() {
        return state;
    }

    public synchronized void start() {
        if (started) {
            return;
        }
        publishState(State.CONNECTING, "Starting TDLib.");
        try {
            clientId = JsonClient.createClientId();
            started = true;
            receiving = true;
            JsonClient.send(clientId, createRequest("getOption").put("name", "version").toString());
            Thread receiver = new Thread(this::receiveLoop, "tdlib-json-receiver");
            receiver.setDaemon(true);
            receiver.start();
        } catch (LinkageError error) {
            started = false;
            receiving = false;
            publishState(State.ERROR, "TDLib native runtime is unavailable: " + safeMessage(error));
        } catch (Exception error) {
            started = false;
            receiving = false;
            publishState(State.ERROR, "Could not start TDLib: " + safeMessage(error));
        }
    }

    public void submitApiCredentials(int apiId, String apiHash) {
        if (apiId <= 0 || apiHash == null || apiHash.trim().isEmpty()) {
            publishState(State.NEEDS_API_CREDENTIALS, "Enter the API ID and API hash from my.telegram.org.");
            return;
        }
        try {
            configStore.save(apiId, apiHash);
            if (awaitingParameters) {
                sendTdlibParameters();
            } else {
                publishState(State.CONNECTING, "Credentials saved securely. Waiting for TDLib authorization state.");
            }
        } catch (Exception error) {
            publishState(State.ERROR, "Could not encrypt the Telegram settings on this device: " + safeMessage(error));
        }
    }

    public void submitPhoneNumber(String phoneNumber) {
        String normalized = phoneNumber == null ? "" : phoneNumber.replaceAll("[\\s()\\-]", "");
        if (!normalized.matches("^\\+[0-9]{5,15}$")) {
            publishState(State.WAIT_PHONE, "Enter the phone number in international format, for example +12025550123.");
            return;
        }
        try {
            JSONObject settings = createRequest("phoneNumberAuthenticationSettings");
            settings.put("allow_flash_call", false);
            settings.put("allow_missed_call", false);
            settings.put("is_current_phone_number", false);
            settings.put("has_unknown_phone_number", false);
            settings.put("allow_sms_retriever_api", false);
            settings.put("firebase_authentication_settings", JSONObject.NULL);
            settings.put("authentication_tokens", new JSONArray());
            JSONObject request = createRequest("setAuthenticationPhoneNumber");
            request.put("phone_number", normalized);
            request.put("settings", settings);
            sendRequest(request);
        } catch (Exception error) {
            publishState(State.ERROR, "Could not submit the phone number: " + safeMessage(error));
        }
    }

    public void submitCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            publishState(State.WAIT_CODE, "Enter the login code sent by Telegram.");
            return;
        }
        sendAuthValue("checkAuthenticationCode", "code", code.trim());
    }

    public void submitPassword(String password) {
        if (password == null || password.isEmpty()) {
            publishState(State.WAIT_PASSWORD, "Enter the Telegram 2-step verification password.");
            return;
        }
        sendAuthValue("checkAuthenticationPassword", "password", password);
    }

    public void submitEmailAddress(String email) {
        if (email == null || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            publishState(State.WAIT_EMAIL_ADDRESS, "Enter a valid email address.");
            return;
        }
        sendAuthValue("setAuthenticationEmailAddress", "email_address", email.trim());
    }

    public void submitEmailCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            publishState(State.WAIT_EMAIL_CODE, "Enter the email verification code sent by Telegram.");
            return;
        }
        try {
            JSONObject codeValue = createRequest("emailAddressAuthenticationCode");
            codeValue.put("code", code.trim());
            JSONObject request = createRequest("checkAuthenticationEmailCode");
            request.put("code", codeValue);
            sendRequest(request);
        } catch (Exception error) {
            publishState(State.ERROR, "Could not submit the email code: " + safeMessage(error));
        }
    }

    public void logOut() {
        try {
            sendRequest(createRequest("logOut"));
            synchronized (chats) {
                chats.clear();
            }
            publishChats();
            publishState(State.CONNECTING, "Logging out of this Telegram account.");
        } catch (Exception error) {
            publishState(State.ERROR, "Could not log out: " + safeMessage(error));
        }
    }

    private void receiveLoop() {
        while (receiving) {
            try {
                String update = JsonClient.receive(0.5);
                if (update != null && !update.isEmpty()) {
                    handleUpdate(new JSONObject(update));
                }
            } catch (LinkageError error) {
                receiving = false;
                publishState(State.ERROR, "TDLib stopped: " + safeMessage(error));
            } catch (Exception error) {
                publishState(State.ERROR, "Could not read a TDLib update: " + safeMessage(error));
            }
        }
    }

    private void handleUpdate(JSONObject update) throws JSONException {
        String type = update.optString("@type", "");
        if ("error".equals(type)) {
            handleError(update);
            return;
        }
        if ("ok".equals(type)) {
            handleSuccess(update);
            return;
        }
        if ("updateAuthorizationState".equals(type)) {
            handleAuthorizationState(update.optJSONObject("authorization_state"));
            return;
        }
        if ("updateNewChat".equals(type)) {
            JSONObject chat = update.optJSONObject("chat");
            if (chat != null) {
                updateChat(chat);
                publishChats();
            }
            return;
        }
        if ("updateChatTitle".equals(type)) {
            long id = parseLong(update.optString("chat_id", "0"));
            synchronized (chats) {
                MutableChat chat = chats.get(id);
                if (chat != null) {
                    chat.title = update.optString("title", chat.title);
                }
            }
            publishChats();
            return;
        }
        if ("updateChatPosition".equals(type)) {
            updatePosition(update);
            publishChats();
        }
    }

    private void handleAuthorizationState(JSONObject authorizationState) {
        if (authorizationState == null) {
            publishState(State.ERROR, "TDLib returned an empty authorization state.");
            return;
        }
        String authType = authorizationState.optString("@type", "");
        switch (authType) {
            case "authorizationStateWaitTdlibParameters":
                awaitingParameters = true;
                try {
                    if (configStore.load() == null) {
                        publishState(State.NEEDS_API_CREDENTIALS, "Enter the Telegram API ID and API hash from my.telegram.org.");
                    } else {
                        sendTdlibParameters();
                    }
                } catch (Exception error) {
                    publishState(State.ERROR, "Could not read encrypted Telegram settings: " + safeMessage(error));
                }
                break;
            case "authorizationStateWaitPhoneNumber":
                publishState(State.WAIT_PHONE, "Enter the phone number for the Telegram account you want to connect.");
                break;
            case "authorizationStateWaitCode":
                publishState(State.WAIT_CODE, "Enter the login code Telegram sent in the app or by SMS.");
                break;
            case "authorizationStateWaitPassword":
                publishState(State.WAIT_PASSWORD, "This account uses 2-step verification.");
                break;
            case "authorizationStateWaitEmailAddress":
                publishState(State.WAIT_EMAIL_ADDRESS, "Telegram requires an email address to continue authorization.");
                break;
            case "authorizationStateWaitEmailCode":
                publishState(State.WAIT_EMAIL_CODE, "Enter the verification code Telegram sent to your email.");
                break;
            case "authorizationStateWaitOtherDeviceConfirmation":
                publishState(State.WAIT_DEVICE_CONFIRMATION, authorizationState.optString("link", "Confirm the login in Telegram on another device."));
                break;
            case "authorizationStateWaitRegistration":
                publishState(State.WAIT_REGISTRATION, "This phone number is not registered. This alpha connects existing Telegram accounts and will not create an account.");
                break;
            case "authorizationStateWaitPremiumPurchase":
                publishState(State.WAIT_REGISTRATION, "Telegram requires an account action before this login can continue.");
                break;
            case "authorizationStateReady":
                awaitingParameters = false;
                parametersSubmitted = false;
                publishState(State.READY, "Connected. Loading groups visible to this account.");
                loadChatLists();
                break;
            case "authorizationStateClosing":
            case "authorizationStateLoggingOut":
                publishState(State.CONNECTING, "Closing the Telegram session.");
                break;
            case "authorizationStateClosed":
                publishState(State.CLOSED, "Telegram session closed.");
                break;
            default:
                publishState(State.CONNECTING, "TDLib authorization state: " + authType);
                break;
        }
    }

    private synchronized void sendTdlibParameters() {
        if (!awaitingParameters || parametersSubmitted || clientId < 0) {
            return;
        }
        try {
            SecureConfigStore.Config config = configStore.load();
            if (config == null) {
                publishState(State.NEEDS_API_CREDENTIALS, "Enter the Telegram API ID and API hash from my.telegram.org.");
                return;
            }

            File root = new File(context.getNoBackupFilesDir(), "tdlib");
            File database = new File(root, "database");
            File files = new File(root, "files");
            if (!database.mkdirs() && !database.isDirectory()) {
                throw new IllegalStateException("Could not create the private TDLib database directory.");
            }
            if (!files.mkdirs() && !files.isDirectory()) {
                throw new IllegalStateException("Could not create the private TDLib files directory.");
            }

            JSONObject parameters = createRequest("tdlibParameters");
            parameters.put("use_test_dc", false);
            parameters.put("database_directory", database.getAbsolutePath());
            parameters.put("files_directory", files.getAbsolutePath());
            parameters.put("database_encryption_key", config.databaseKey);
            parameters.put("use_file_database", true);
            parameters.put("use_chat_info_database", true);
            parameters.put("use_message_database", true);
            parameters.put("use_secret_chats", false);
            parameters.put("api_id", config.apiId);
            parameters.put("api_hash", config.apiHash);
            parameters.put("system_language_code", systemLanguage());
            parameters.put("device_model", deviceModel());
            parameters.put("system_version", Build.VERSION.RELEASE == null ? "" : Build.VERSION.RELEASE);
            parameters.put("application_version", "Alpha-1.1.0");

            JSONObject request = createRequest("setTdlibParameters");
            request.put("parameters", parameters);
            parametersSubmitted = true;
            awaitingParameters = false;
            publishState(State.CONNECTING, "Connecting to Telegram.");
            sendRequest(request);
        } catch (Exception error) {
            publishState(State.ERROR, "Could not initialize TDLib: " + safeMessage(error));
        }
    }

    private void loadChatLists() {
        synchronized (pendingChatLoads) {
            pendingChatLoads.clear();
            chatLoadPages.clear();
        }
        synchronized (chats) {
            chats.clear();
        }
        publishChats();
        requestMoreChats("chatListMain");
        requestMoreChats("chatListArchive");
    }

    private void requestMoreChats(String listType) {
        try {
            synchronized (pendingChatLoads) {
                int pages = chatLoadPages.containsKey(listType) ? chatLoadPages.get(listType) : 0;
                if (pages >= MAX_CHAT_LOAD_BATCHES) {
                    return;
                }
                chatLoadPages.put(listType, pages + 1);
            }

            JSONObject list = createRequest(listType);
            JSONObject request = createRequest("loadChats");
            request.put("chat_list", list);
            request.put("limit", CHAT_LOAD_LIMIT);
            String extra = Long.toString(nextExtra.incrementAndGet());
            request.put("@extra", extra);
            synchronized (pendingChatLoads) {
                pendingChatLoads.put(extra, listType);
            }
            JsonClient.send(clientId, request.toString());
        } catch (Exception error) {
            publishState(State.READY, "Connected, but could not load more chats: " + safeMessage(error));
        }
    }

    private void handleSuccess(JSONObject response) {
        String extra = response.optString("@extra", "");
        String listType;
        synchronized (pendingChatLoads) {
            listType = pendingChatLoads.remove(extra);
        }
        if (listType != null) {
            requestMoreChats(listType);
        }
    }

    private void updateChat(JSONObject source) throws JSONException {
        long id = parseLong(source.optString("id", "0"));
        if (id == 0) {
            return;
        }
        JSONObject type = source.optJSONObject("type");
        String typeName = type == null ? "" : type.optString("@type", "");
        boolean group = "chatTypeBasicGroup".equals(typeName)
                || ("chatTypeSupergroup".equals(typeName) && !type.optBoolean("is_channel", false));
        synchronized (chats) {
            if (!group) {
                chats.remove(id);
                return;
            }

            MutableChat chat = chats.get(id);
            if (chat == null) {
                chat = new MutableChat(id);
                chats.put(id, chat);
            }
            chat.title = source.optString("title", "Telegram group");
            JSONArray positions = source.optJSONArray("positions");
            if (positions != null) {
                for (int i = 0; i < positions.length(); i++) {
                    JSONObject position = positions.optJSONObject(i);
                    if (position != null) {
                        applyPosition(chat, position);
                    }
                }
            }
        }
    }

    private void updatePosition(JSONObject update) {
        long id = parseLong(update.optString("chat_id", "0"));
        JSONObject position = update.optJSONObject("position");
        if (id == 0 || position == null) {
            return;
        }
        synchronized (chats) {
            MutableChat chat = chats.get(id);
            if (chat != null) {
                applyPosition(chat, position);
            }
        }
    }

    private void applyPosition(MutableChat chat, JSONObject position) {
        JSONObject list = position.optJSONObject("list");
        String listType = list == null ? "" : list.optString("@type", "");
        long order = parseLong(position.optString("order", "0"));
        if ("chatListMain".equals(listType)) {
            chat.mainOrder = order;
            chat.positionKnown = true;
        } else if ("chatListArchive".equals(listType)) {
            chat.archiveOrder = order;
            chat.positionKnown = true;
        }
    }

    private List<Chat> snapshotChats() {
        List<Chat> result = new ArrayList<>();
        synchronized (chats) {
            for (MutableChat chat : chats.values()) {
                if (chat.positionKnown && chat.mainOrder == 0 && chat.archiveOrder == 0) {
                    continue;
                }
                long order = Math.max(chat.mainOrder, chat.archiveOrder);
                result.add(new Chat(chat.id, chat.title, order));
            }
        }
        Collections.sort(result, new Comparator<Chat>() {
            @Override
            public int compare(Chat left, Chat right) {
                int byPosition = Long.compare(right.order, left.order);
                if (byPosition != 0) {
                    return byPosition;
                }
                return left.title.compareToIgnoreCase(right.title);
            }
        });
        return result;
    }

    private void publishChats() {
        final List<Chat> snapshot = snapshotChats();
        mainHandler.post(() -> {
            Listener current = listener;
            if (current != null) {
                current.onChatsChanged(snapshot);
            }
        });
    }

    private void handleError(JSONObject error) {
        int code = error.optInt("code", 0);
        String message = error.optString("message", "Unknown Telegram error.");
        String extra = error.optString("@extra", "");
        String loadList;
        synchronized (pendingChatLoads) {
            loadList = pendingChatLoads.remove(extra);
        }
        if (loadList != null) {
            if (code != 404) {
                publishState(State.READY, "Could not load the " + loadList + " chat list (" + code + "): " + message);
            }
            return;
        }
        if (parametersSubmitted && state == State.CONNECTING) {
            parametersSubmitted = false;
            awaitingParameters = true;
            publishState(State.NEEDS_API_CREDENTIALS, "Telegram rejected the API settings (" + code + "): " + message);
            return;
        }
        publishState(state, "Telegram error (" + code + "): " + message);
    }

    private void sendAuthValue(String type, String key, String value) {
        try {
            JSONObject request = createRequest(type);
            request.put(key, value);
            sendRequest(request);
        } catch (Exception error) {
            publishState(State.ERROR, "Could not submit the Telegram authorization value: " + safeMessage(error));
        }
    }

    private void sendRequest(JSONObject request) throws JSONException {
        if (clientId < 0) {
            throw new IllegalStateException("TDLib is not running.");
        }
        request.put("@extra", Long.toString(nextExtra.incrementAndGet()));
        JsonClient.send(clientId, request.toString());
    }

    private static JSONObject createRequest(String type) throws JSONException {
        return new JSONObject().put("@type", type);
    }

    private void publishState(State newState, String newDetails) {
        state = newState;
        details = newDetails == null ? "" : newDetails;
        final String currentDetails = details;
        mainHandler.post(() -> {
            Listener current = listener;
            if (current != null) {
                current.onStateChanged(newState, currentDetails);
            }
        });
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static String safeMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isEmpty() ? error.getClass().getSimpleName() : message;
    }

    private static String systemLanguage() {
        String language = Locale.getDefault().toLanguageTag();
        return language == null || language.isEmpty() ? "en" : language;
    }

    private static String deviceModel() {
        String model = (Build.MANUFACTURER + " " + Build.MODEL).trim();
        return model.isEmpty() ? "Android" : model;
    }

    private static final class MutableChat {
        final long id;
        String title = "Telegram group";
        long mainOrder;
        long archiveOrder;
        boolean positionKnown;

        MutableChat(long id) {
            this.id = id;
        }
    }
}
