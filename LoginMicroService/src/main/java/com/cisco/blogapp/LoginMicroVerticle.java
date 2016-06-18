package com.cisco.blogapp;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.mongodb.morphia.Datastore;

import com.cisco.blogapp.infra.ServicesFactory;
import com.cisco.blogapp.model.User;
import com.cisco.blogapp.model.UserDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class LoginMicroVerticle extends AbstractVerticle{
	
	public static void main(String args[]){
		
		VertxOptions options = new VertxOptions().setWorkerPoolSize(10);
		Vertx vertx = Vertx.vertx(options);
		vertx.deployVerticle(LoginMicroVerticle.class.getName(), stringAsyncResult -> {
			System.out.println(LoginMicroVerticle.class.getName() + "Deployment Completed");
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
//	    router.route().handler(CookieHandler.create());
//	    router.route().handler(SessionHandler.create(sessionStore));
	    
		router.post("/Services/rest/user/register").handler(new UserRegister());
		router.get("/Services/rest/user").handler(new UserLoader());
		router.post("/Services/rest/user/auth").handler(new UserAuth());
		router.get("/Services/rest/company/:companyId/sites").handler(this::handleGetSitesOfCompany);
		// Using Lambda Function
		router.get("/Services/rest/company").handler( (routingContext) -> {
			System.out.println("GEt comapnies");
			JsonArray resJson = new JsonArray().add(
					new JsonObject().put("id", "55716669eec5ca2b6ddf5626").put("companyName", "Cisco").put("subdomain", "nds")
				).add(
						new JsonObject().put("id", "559e4331c203b4638a00ba1a").put("companyName", "Acme Inc").put("subdomain", "acme")
				);
			System.out.println(resJson.encode());
			
			routingContext.response().putHeader("content-type", "application/json").end(resJson.encode());
		});
		router.get("/Services/rest/company/:companyId/sites/:siteId/departments").handler(this::handleGetDepartmentsOfSite);

		// StaticHanlder for loading frontend angular app
		router.route().handler(StaticHandler.create()::handle);
		int port = 8086;
		vertx.createHttpServer().requestHandler(router::accept).listen(port);	
		System.out.println("BlogAppVerticle verticle started: "+port);
		startFuture.complete();
	}
	
	@Override
	public void stop(Future<Void> stopFuture){
		System.out.println("BlogAppVerticle stopped");
		stopFuture.complete();
	}
	class Credentials{
    	String userName;
    	String password;
    }
	private void handleGetDepartmentsOfSite(RoutingContext routingContext) {
		JsonArray resJson = new JsonArray().add(
				new JsonObject()
				.put("id", "55716669eec5ca2b6ddf5628")
				.put("deptName", "Sales")
				.put("siteId", "55716669eec5ca2b6ddf5627")
			);
		routingContext.response().putHeader("content-type", "application/json").end(resJson.encode());
	}
	
	private void handleGetSitesOfCompany(RoutingContext routingContext) {	
		JsonArray resJson = new JsonArray().add(
				new JsonObject()
				.put("id", "55716669eec5ca2b6ddf5627")
				.put("siteName", "Acme Inc")
				.put("companyId", "55716669eec5ca2b6ddf5626")
				.put("subdomain", "acme")
			);
		routingContext.response().putHeader("content-type", "application/json").end(resJson.encode());
	}
	private boolean GetCredentials(final String authorization, Credentials credObject){
		boolean status = false;
		if (authorization != null && authorization.startsWith("Basic")) {
	        // Authorization: Basic base64credentials
	        String base64Credentials = authorization.substring("Basic".length()).trim();
	        String credentials = new String(Base64.getDecoder().decode(base64Credentials),
	                Charset.forName("UTF-8"));
	        // credentials = username:password
	        final String[] values = credentials.split(":",2);
	        credObject.userName = values[0];
	        credObject.password = values[1];
	        System.out.println("user and password :"+values[0]+" " +values[1]);
	    }
		return status;
	}

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
			String userName = u.getUserName();
			Datastore dataStore = ServicesFactory.getMongoDB();
			List<User> users = dataStore.createQuery(User.class)
					.field("userName").equal(userName).asList();
			if (users.size() == 0) {
				routingContext.vertx().executeBlocking((future) -> {
					System.out.println("Inside Execute Blocking!!!");
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
			else{
				response.setStatusCode(500).end("UserName taken already");
			}
			
			
			
		}
	}
    
	class UserAuth implements Handler<RoutingContext> {
	
		public void handle(RoutingContext routingContext) {
			System.out.println("Thread UserAuth: " + Thread.currentThread().getId());
			try {
				
				HttpServerResponse response = routingContext.response();
				
//				Session session = routingContext.session();
		
				Datastore dataStore = ServicesFactory.getMongoDB();
				// Get Request Body that contains login details
				HttpServerRequest request = routingContext.request();
				final String authorization = request.getHeader("Authorization");
				Credentials cred = new Credentials();
				GetCredentials(authorization,cred);
				System.out.println("userName :" + cred.userName + " password : " +cred.password);
				
				// Query DB for the User matching with the given userName
				List<User> users = dataStore.createQuery(User.class)
						.field("userName").equal(cred.userName).asList();
				if (users.size() != 0) {
					for (User u : users) {
						// See if user's password matched
						if (u.getPassword().equals(cred.password) && u.getUserName().equals(cred.userName)) {
							System.out.println(cred.userName +" User Authentication Success !!!");
							// Add to the list of LoggedInUsers hashmap
							LoginMicroVerticle.loggedInUsers.put(u.getUserName(), u);
							response.setStatusCode(204).end("User Authentication Success !!!");
							break;
						}
					}
				} else {
					response.setStatusCode(404).end("not found");
				}

			}
			catch(Exception e){
				
				e.printStackTrace();
			}
					
		}
	}


	class UserLoader implements Handler<RoutingContext> {
		public void handle(RoutingContext routingContext) {
			System.out.println("Thread UserLoader: "
					+ Thread.currentThread().getId());
			// This handler will be called for every request
			HttpServerResponse response = routingContext.response();
			Datastore dataStore = ServicesFactory.getMongoDB();
			MultiMap params = routingContext.request().params();
	
			if (params.size() > 0) {
				if (params.contains("signedIn")) {
					HttpServerRequest request = routingContext.request();
					final String authorization = request.getHeader("Authorization");
					Credentials cred = new Credentials();
					GetCredentials(authorization,cred);
					System.out.println("userName :" + cred.userName + " password : " +cred.password);
					
					// Query DB for the User matching with the given userName
					List<User> users = dataStore.createQuery(User.class)
							.field("userName").equal(cred.userName).asList();
					ArrayList<User> userList = new ArrayList<User>();
						for (User u : users) {
							
							// See if user's password matched
							if (u.getPassword().equals(cred.password) && u.getUserName().equals(cred.userName)) {
								System.out.println(cred.userName +" User Authentication Success !!!");
								// Add to the list of LoggedInUsers hashmap
								
								for(Map.Entry<String, User> m: LoginMicroVerticle.loggedInUsers.entrySet()){  
									userList.add(m.getValue());  
								}  
								break;
							}
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
