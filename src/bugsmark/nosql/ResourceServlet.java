package example.nosql;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.cloudant.client.api.Database;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Path("/favorites")
/**
 * CRUD service of todo list table. It uses REST style.
 */
public class ResourceServlet {

	public ResourceServlet() {
	}

	@POST
	public Response create(@QueryParam("id") Long id, @FormParam("name") String name, @FormParam("value") String value, @FormParam("group") String group)
			throws Exception {

		Database db = null;
		try {
			db = getDB();
		} catch (Exception re) {
			re.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}

		String idString = id == null ? null : id.toString();
		JsonObject resultObject = create(db, idString, name, value, group);

		System.out.println("Create Successful.");

		return Response.ok(resultObject.toString()).build();
	}

	protected JsonObject create(Database db, String id, String name, String value, String group)
			throws IOException {

		// check if document exist
		HashMap<String, Object> obj = (id == null) ? null : db.find(HashMap.class, id);

		//printDataLog("create", group, name, value);

		if (obj == null) {
			// if new document

			id = String.valueOf(System.currentTimeMillis());

			// create a new document
			System.out.println("Creating new document with id : " + id);
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("name", name);
			data.put("_id", id);
			data.put("value", value);
			data.put("group", group);
			data.put("creation_date", new Date().toString());
			db.save(data);

		} else {

			// update other fields in the document
			obj = db.find(HashMap.class, id);
			obj.put("name", name);
			obj.put("value", value);
			obj.put("gruop", group);
			db.update(obj);
		}

		obj = db.find(HashMap.class, id);

		JsonObject resultObject = toJsonObject(obj);

		return resultObject;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@QueryParam("id") Long id, @QueryParam("cmd") String cmd) throws Exception {

		Database db = null;
		try {
			db = getDB();
		} catch (Exception re) {
			re.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}

		JsonObject resultObject = new JsonObject();
		JsonArray jsonArray = new JsonArray();

		if (id == null) {
			try {
				// get all the document present in database
				List<HashMap> allDocs = db.getAllDocsRequestBuilder().includeDocs(true).build().getResponse()
						.getDocsAs(HashMap.class);

				if (allDocs.size() == 0) {
					allDocs = initializeSampleData(db);
				}

				for (HashMap doc : allDocs) {
					HashMap<String, Object> obj = db.find(HashMap.class, doc.get("_id") + "");
					JsonObject jsonObject = new JsonObject();

					jsonObject.addProperty("id", obj.get("_id") + "");
					jsonObject.addProperty("name", obj.get("name") + "");
					jsonObject.addProperty("value", obj.get("value") + "");
					jsonObject.addProperty("group", obj.get("group") + "");

					jsonArray.add(jsonObject);
				}
			} catch (Exception dnfe) {
				System.out.println("Exception thrown : " + dnfe.getMessage());
			}

			resultObject.addProperty("id", "all");
			resultObject.add("body", jsonArray);

			return Response.ok(resultObject.toString()).build();
		}

		// check if document exists
		HashMap<String, Object> obj = db.find(HashMap.class, id + "");
		if (obj != null) {
			JsonObject jsonObject = toJsonObject(obj);
			return Response.ok(jsonObject.toString()).build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@DELETE
	public Response delete(@QueryParam("id") long id) {

		Database db = null;
		try {
			db = getDB();
		} catch (Exception re) {
			re.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}

		// check if document exist
		HashMap<String, Object> obj = db.find(HashMap.class, id + "");

		if (obj == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		} else {
			db.remove(obj);

			System.out.println("Delete Successful.");

			return Response.ok("OK").build();
		}
	}

	@PUT
	public Response update(@QueryParam("id") long id, @QueryParam("name") String name,
			@QueryParam("value") String value, @QueryParam("group") String group) {

		Database db = null;
		try {
			db = getDB();
		} catch (Exception re) {
			re.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}

		// check if document exist
		HashMap<String, Object> obj = db.find(HashMap.class, id + "");

		if (obj == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		} else {
			obj.put("name", name);
			obj.put("value", value);
			obj.put("group", group);
	
			//printDataLog("update", group, name, value);
			
			db.update(obj);
			System.out.println("Update Successful.");

			return Response.ok("OK").build();
		}
	}

	private JsonObject toJsonObject(HashMap<String, Object> obj) {
		JsonObject jsonObject = new JsonObject();

		jsonObject.addProperty("id", obj.get("_id") + "");
		jsonObject.addProperty("name", obj.get("name") + "");
		jsonObject.addProperty("value", obj.get("value") + "");
		jsonObject.addProperty("group", obj.get("group") + "");
		
		return jsonObject;
	}

	/*
	 * Create a document and Initialize with sample data
	 */
	private List<HashMap> initializeSampleData(Database db) throws Exception {

		long id = System.currentTimeMillis();
		String name = "Sample bookmark";
		String value = "http://www.ibm.com";
		String group = "Business";

		// create a new document
		System.out.println("Creating new bookmark with id : " + id);
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("name", name);
		data.put("_id", id + "");
		data.put("value", value);
		data.put("group", group);
		data.put("creation_date", new Date().toString());
		db.save(data);

		return db.getAllDocsRequestBuilder().includeDocs(true).build().getResponse().getDocsAs(HashMap.class);

	}

	private Database getDB() throws MalformedURLException {
		return CloudantClientMgr.getDB();
	}

	private void printDataLog(String place, String group, String name, String value) {
		System.out.println("----------" + place + "------------------");
		System.out.println("group: " + group);
		System.out.println("name:  " + name);
		System.out.println("value: " + value);
		System.out.println("----------------------------------");		
	}
}
