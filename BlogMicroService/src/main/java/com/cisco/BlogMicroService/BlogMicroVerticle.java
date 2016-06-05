package com.cisco.BlogMicroService;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;

import com.cisco.BlogMicroService.dbconn.MongoConnect;
import com.cisco.BlogMicroService.model.Blog;
import com.cisco.BlogMicroService.model.BlogDTO;
import com.cisco.BlogMicroService.model.User;
import com.cisco.BlogMicroService.model.Comment;
import com.cisco.BlogMicroService.model.CommentDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import io.vertx.core.json.EncodeException;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class BlogMicroVerticle extends AbstractVerticle{
	
	public static void main(String args[]){
		
		VertxOptions options = new VertxOptions().setWorkerPoolSize(10);
		Vertx vertx = Vertx.vertx(options);
		vertx.deployVerticle(BlogMicroVerticle.class.getName(), stringAsyncResult -> {
			System.out.println(BlogMicroVerticle.class.getName() + "Deployment Completed");
		});
	}
	// Store the list of logged In Users
	//public static HashMap<String, User> loggedInUsers = new HashMap<String, User>();
	
	@Override
	public void start(Future<Void> startFuture){
		
		Router router = Router.router(vertx);
		LocalSessionStore sessionStore = LocalSessionStore.create(vertx);
		
		// Handlers to get request bodies and 
		// for cookies and sessions
		
	    router.route().handler(BodyHandler.create());
	    router.route().handler(CookieHandler.create());
	    router.route().handler(SessionHandler.create(sessionStore));
	    
	    router.get("/Services/rest/company/:companyId/sites").handler(this::handleGetSitesOfCompany);
		router.get("/Services/rest/company/:companyId/sites/:siteId/departments").handler(this::handleGetDepartmentsOfSite);
		router.post("/Services/rest/blogs").handler(new BlogPost());
		router.get("/Services/rest/blogs").handler(new BlogGet());
		router.post("/Services/rest/blogs/:blogId/comments").handler(new BlogComment());

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
		
		// StaticHanlder for loading frontend angular app
				router.route().handler(StaticHandler.create()::handle);

		vertx.createHttpServer().requestHandler(router::accept).listen(8082);	
		System.out.println("BlogMicroVerticle verticle started");
		startFuture.complete();
	}
	
	@Override
	public void stop(Future<Void> stopFuture){
		System.out.println("BlogMicroVerticle stopped");
		stopFuture.complete();
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
	
	private void handleGetDepartmentsOfSite(RoutingContext routingContext) {
		JsonArray resJson = new JsonArray().add(
				new JsonObject()
				.put("id", "55716669eec5ca2b6ddf5628")
				.put("deptName", "Sales")
				.put("siteId", "55716669eec5ca2b6ddf5627")
			);
		routingContext.response().putHeader("content-type", "application/json").end(resJson.encode());
	}
	

		
}

class BlogComment implements Handler<RoutingContext> {
	public void handle(RoutingContext routingContext) {
		System.out.println("Thread BlogComment: "
				+ Thread.currentThread().getId());
		HttpServerResponse response = routingContext.response();
		String blogId = routingContext.request().getParam("blogId");
		System.out.println("blogId: " + blogId);
		Session session = routingContext.session();
		response.putHeader("content-type", "application/json");
	
		String json = routingContext.getBodyAsString();
		ObjectMapper mapper = new ObjectMapper();
		Datastore dataStore = MongoConnect.getMongoDB();
		CommentDTO dto = null;
		try {
			dto = mapper.readValue(json, CommentDTO.class);
			
			// TBD: get the user from AUTH
			String userName = "sande";//session.get("user");
			System.out.println("userName :" + userName);
			if (userName == null ) {
				System.out.println("No Valid user logged in");
				response.setStatusCode(401).end("No Valid user logged in");
			} else {
				User user = dataStore.createQuery(User.class)
						.field("userName").equal(userName).get();
				dto.setUserFirst(user.getFirst());
				dto.setUserLast(user.getLast());
				dto.setUserId(user.getId().toString());
				dto.setDate(new Date().getTime());
				Comment comment = dto.toModel();

				ObjectId oid = null;
				try {
					oid = new ObjectId(blogId);
				} catch (Exception e) {// Ignore format errors
				}
				Blog blog = dataStore.createQuery(Blog.class).field("id")
						.equal(oid).get();
				List<Comment> comments = blog.getComments();
				System.out.println("comments: " + comments);
				comments.add(comment);
				blog.setComments(comments);
				dataStore.save(blog);

				response.setStatusCode(204).end("Comment saved !!");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class BlogGet implements Handler<RoutingContext> {
	public void handle(RoutingContext routingContext) {
		System.out.println("Thread BlogList: " + Thread.currentThread().getId());
		HttpServerResponse response = routingContext.response();
		response.putHeader("content-type", "application/json");
		Datastore dataStore = MongoConnect.getMongoDB();

		// For tag search
		String tagParam = routingContext.request().query();
		List<Blog> blogs = null;
		if (tagParam != null) {
			String tagValue = tagParam.split("=")[1];
			blogs = dataStore.createQuery(Blog.class).field("tags")
					.contains(tagValue).asList();
		} else {
			blogs = dataStore.createQuery(Blog.class).asList();
		}
		if (blogs.size() != 0) {
			List<BlogDTO> obj = new ArrayList<BlogDTO>();
			for (Blog b : blogs) {
				BlogDTO dto = new BlogDTO().fillFromModel(b);
				obj.add(dto);
			}

			ObjectMapper mapper = new ObjectMapper();
			try {
				response.end(mapper.writeValueAsString(obj));
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			response.setStatusCode(404).end("not found");
		}
	}
}


class BlogPost implements Handler<RoutingContext> {
	public void handle(RoutingContext routingContext) {
		System.out.println("Thread BlogPersister: "
				+ Thread.currentThread().getId());
		HttpServerResponse response = routingContext.response();
		String json = routingContext.getBodyAsString();
		System.out.println("User:" + json);
		ObjectMapper mapper = new ObjectMapper();
		Datastore dataStore = MongoConnect.getMongoDB();
		BlogDTO dto = null;
		try {
				dto = mapper.readValue(json, BlogDTO.class);
				
				// TBD: get the user details from URL
				String userName = "sande";
				User user = dataStore.createQuery(User.class)
						.field("userName").equal(userName).get();
				System.out.println(user);
				dto.setUserFirst(user.getFirst());
				dto.setUserLast(user.getLast());
				dto.setUserId(user.getId().toString());
				dto.setDate(new Date().getTime());
				Blog blog = dto.toModel();
				dataStore.save(blog);
				response.setStatusCode(204).end("Blog saved !!");
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}


