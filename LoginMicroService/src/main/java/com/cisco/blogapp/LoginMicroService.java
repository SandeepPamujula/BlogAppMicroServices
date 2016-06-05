package com.cisco.blogapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import io.vertx.core.json.EncodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.mongodb.morphia.Datastore;

//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.Reader;
//import java.net.URL;
//import java.nio.charset.Charset;

import com.cisco.blogapp.infra.ServicesFactory;
import com.cisco.blogapp.model.User;
import com.cisco.blogapp.model.UserDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class LoginMicroService extends AbstractVerticle{
	
	public static void main(String args[]){
		
		VertxOptions options = new VertxOptions().setWorkerPoolSize(10);
		Vertx vertx = Vertx.vertx(options);
		vertx.deployVerticle(LoginMicroService.class.getName(), stringAsyncResult -> {
			System.out.println(LoginMicroService.class.getName() + "Deployment Completed");
		});
	}
	// Store the list of logged In Users
	public static HashMap<String, User> loggedInUsers = new HashMap<String, User>();
	
	@Override
	public void start(Future<Void> startFuture){
		
		Router router = Router.router(vertx);
		LocalSessionStore sessionStore = LocalSessionStore.create(vertx);
		
		// Handlers to get request bodies and 
		// for cookies and sessions
		
	    router.route().handler(BodyHandler.create());
	    router.route().handler(CookieHandler.create());
	    router.route().handler(SessionHandler.create(sessionStore));
	    
		router.post("/Services/rest/user/register").handler(new UserRegister());
		router.get("/Services/rest/user").handler(new UserLoader());
		router.post("/Services/rest/user/auth").handler(new UserAuth());
//		router.get("/Services/rest/blogs").handler(new BlogGet());
//		router.post("/Services/rest/blogs").handler(new BlogPost());
//		router.post("/Services/rest/blogs/:blogId/comments").handler(new BlogComment());
//	    router.get("/Services/rest/company/:companyId/sites").handler(this::handleGetSitesOfCompany);
//		router.get("/Services/rest/company/:companyId/sites/:siteId/departments").handler(this::handleGetDepartmentsOfSite);
		
		
		
//		// Using Lambda Function
//		router.get("/Services/rest/company").handler( (routingContext) -> {
//			System.out.println("GEt comapnies");
//			
//		//sande
//			if (false){
//				
//				JsonArray resJson = new JsonArray().add(
//						new JsonObject().put("id", "55716669eec5ca2b6ddf5626").put("companyName", "Cisco").put("subdomain", "nds")
//					).add(
//							new JsonObject().put("id", "559e4331c203b4638a00ba1a").put("companyName", "Acme Inc").put("subdomain", "acme")
//					);
//				System.out.println(resJson.encode());
//				routingContext.response().putHeader("content-type", "application/json").end(resJson.encode());
//				
//			}else{
//				JsonArray companyList = new JsonArray();
//				try {
//					
//					companyList = readJsonFromUrl("http://localhost:8082/Services/rest/company");
//					System.out.println("companies "+companyList.toString());
//				} catch (Exception e) {
//					// TODO Auto-generated catch block
//					System.out.println("exception ---------------------------------- ");
//					e.printStackTrace();
//				}
//				routingContext.response().putHeader("content-type", "application/json").end(companyList.encode());
//			}
//			
//		});
//		
//		// StaticHanlder for loading frontend angular app
//				router.route().handler(StaticHandler.create()::handle);
//
		vertx.createHttpServer().requestHandler(router::accept).listen(8083);	
		System.out.println("BlogAppVerticle verticle started");
		startFuture.complete();
	}
	
	@Override
	public void stop(Future<Void> stopFuture){
		System.out.println("BlogAppVerticle stopped");
		stopFuture.complete();
	}
	
//	private static String readAll(Reader rd) throws IOException {
//	    StringBuilder sb = new StringBuilder();
//	    int cp;
//	    while ((cp = rd.read()) != -1) {
//	      sb.append((char) cp);
//	    }
//	    return sb.toString();
//	  }

//	private JsonArray readJsonFromUrl(String url) throws IOException, EncodeException {
//	    InputStream is = new URL(url).openStream();
//	    try {
//	      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
//	      String jsonText = readAll(rd);
//	      JsonArray json = new JsonArray(jsonText);
//	      System.out.println(jsonText);
//	      System.out.println(json);
//	      return json;
//	    } finally {
//	      is.close();
//	    }
//	  }

	class UserRegister implements Handler<RoutingContext> {
		public void handle(RoutingContext routingContext) {
			System.out.println("Thread UserRegister: "	+ Thread.currentThread().getId());
			HttpServerResponse response = routingContext.response();
			// Get request Body
			String json = routingContext.getBodyAsString();
			ObjectMapper mapper = new ObjectMapper();
			UserDTO dto = null;
			try {
				// Map Json to UserDTO 
				dto = mapper.readValue(json, UserDTO.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
			// Map UserDTO to User Model
			User u = dto.toModel();
			routingContext.vertx().executeBlocking((future) -> {
				System.out.println("Inside Execute Blocking!!!");
				Datastore dataStore = ServicesFactory.getMongoDB();
				// Store User into MongoDB
				dataStore.save(u);
				future.complete();
			}, res -> {
				if(res.succeeded()) {
					response.setStatusCode(204).end("Data saved");
				} else {
					response.setStatusCode(500).end("Data Not Saved");
				}
			});
			
			
		}
	}

	class UserAuth implements Handler<RoutingContext> {
	
		public void handle(RoutingContext routingContext) {
			System.out.println("Thread UserAuth: " + Thread.currentThread().getId());
	
			HttpServerResponse response = routingContext.response();
			Session session = routingContext.session();
	
			Datastore dataStore = ServicesFactory.getMongoDB();
			// Get Request Body that contains login details
			String json = routingContext.getBodyAsString();
			System.out.println("User login details" + json);
			JsonObject jsonObj = new JsonObject(json);
			// Get userName and password from jsonObj
			String user = jsonObj.getString("userName");
			String passwd = jsonObj.getString("password");
			System.out.println("userName :" + user + " password : " +passwd);
			
			// Query DB for the User matching with the given userName
			List<User> users = dataStore.createQuery(User.class)
					.field("userName").equal(user).asList();
			if (users.size() != 0) {
				for (User u : users) {
					// See if user's password matched
					if (u.getPassword().equals(passwd)) {
						if(session != null) {
							session.put("user", u.getUserName());
						}
						// Add to the list of LoggedInUsers hashmap
						LoginMicroService.loggedInUsers.put(u.getUserName(), u);
						response.setStatusCode(204).end("User Authentication Success !!!");
						break;
					}
				}
			} else {
				response.setStatusCode(404).end("not found");
			}
		}
		
	}


	class UserLoader implements Handler<RoutingContext> {
		public void handle(RoutingContext routingContext) {
			System.out.println("Thread UserLoader: "
					+ Thread.currentThread().getId());
			// This handler will be called for every request
			HttpServerResponse response = routingContext.response();
			MultiMap params = routingContext.request().params();
	
			if (params.size() > 0) {
				if (params.contains("signedIn")) {
					ArrayList<User> userList = new ArrayList<User>();
					for(Map.Entry<String, User> m: LoginMicroService.loggedInUsers.entrySet()){  
						userList.add(m.getValue());  
					}  
					ObjectMapper mapper = new ObjectMapper();
					JsonNode node = mapper.valueToTree(userList);
					System.out.println("Logged in users List: " + node.toString());
					response.putHeader("content-type", "application/json");
					String json = node.toString();
					response.setStatusCode(200).end(json);
				}
			}
		}
	}
}
