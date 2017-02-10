package example.nosql;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Set;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CloudantClientMgr {

	private static CloudantClient cloudant = null;
	private static Database db = null;

	private static String databaseName = "bookmarks";

	private static String user = null;
	private static String password = null;
	private static String url = null;

	private static void initClient() throws MalformedURLException {
		if (cloudant == null) {
			synchronized (CloudantClientMgr.class) {
				if (cloudant != null) {
					return;
				}
				cloudant = createClient();

			} // end synchronized
		}
	}

	private static CloudantClient createClient() throws MalformedURLException {
		// VCAP_SERVICES is a system environment variable
		// Parse it to obtain the NoSQL DB connection info
		String VCAP_SERVICES = System.getenv("VCAP_SERVICES");
		String serviceName = null;

		if (VCAP_SERVICES != null) {
			// parse the VCAP JSON structure
			JsonObject obj = (JsonObject) new JsonParser().parse(VCAP_SERVICES);
			Entry<String, JsonElement> dbEntry = null;
			Set<Entry<String, JsonElement>> entries = obj.entrySet();
			// Look for the VCAP key that holds the cloudant no sql db information
			for (Entry<String, JsonElement> eachEntry : entries) {
				if (eachEntry.getKey().toLowerCase().contains("cloudant")) {
					dbEntry = eachEntry;
					break;
				}
			}
			if (dbEntry == null) {
				throw new RuntimeException("Could not find cloudantNoSQLDB key in VCAP_SERVICES env variable");
			}

			obj = (JsonObject) ((JsonArray) dbEntry.getValue()).get(0);
			serviceName = (String) dbEntry.getKey();
			System.out.println("Service Name - " + serviceName);

			obj = (JsonObject) obj.get("credentials");

			user = obj.get("username").getAsString();
			password = obj.get("password").getAsString();
			url = obj.get("url").getAsString();

		} else {
			connectBMXService();
		}

		try {
			ClientBuilder.account(user)
                    .username(user)
                    .password(password);
			CloudantClient client = ClientBuilder
                    .url(new URL(url))
                    .build();
			return client;
		} catch (CouchDbException e) {
			throw new RuntimeException("Unable to connect to repository", e);
		}
	}

	public static Database getDB() throws MalformedURLException {
		if (cloudant == null) {
			initClient();
		}

		if (db == null) {
			try {
				db = cloudant.database(databaseName, true);
			} catch (Exception e) {
				throw new RuntimeException("DB Not found", e);
			}
		}
		return db;
	}

	private CloudantClientMgr() {
	}
	
	private static void connectBMXService() {
        user = "ac9b2b46-c5fc-497e-bbe9-8d1c685187b8-bluemix";
        password = "8f1a44c1e34278eb300b05e2a958ba06f6dfc21e628d6a2964da8365d76cbd46";
        url = "https://ac9b2b46-c5fc-497e-bbe9-8d1c685187b8-bluemix:8f1a44c1e34278eb300b05e2a958ba06f6dfc21e628d6a2964da8365d76cbd46@ac9b2b46-c5fc-497e-bbe9-8d1c685187b8-bluemix.cloudant.com";
    }
}
