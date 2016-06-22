package com.cisco.BlogMicroService;


import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;

import com.cisco.BlogMicroService.dbconn.ServicesFactory;
import com.cisco.BlogMicroService.model.Blog;
import com.cisco.BlogMicroService.model.BlogDTO;
import com.cisco.BlogMicroService.model.Comment;
import com.cisco.BlogMicroService.model.CommentDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class BlogMicroVerticle extends AbstractVerticle{
	
	public static void main(String args[]){
		
		VertxOptions options = new VertxOptions().setWorkerPoolSize(10);
		Vertx vertx = Vertx.vertx(options);
		vertx.deployVerticle(BlogMicroVerticle.class.getName(), stringAsyncResult -> {
			System.out.println(BlogMicroVerticle.class.getName() + "Deployment Completed");
		});
		
//		ClusterManager mgr = new HazelcastClusterManager();
//		VertxOptions options = new VertxOptions().setWorkerPoolSize(10).setClusterManager(mgr);
//		Vertx.clusteredVertx(options, res -> {
//			  if (res.succeeded()) {
//			    Vertx vertx = res.result();
//			    vertx.deployVerticle(BlogMicroVerticle.class.getName());
//			    System.out.println(BlogMicroVerticle.class.getName() + "Deployment Completed");
//			  } else {
//			    // failed!
//				  System.out.println(BlogMicroVerticle.class.getName() + "Deployment failed");
//			  }
//			});
	}
	
	@Override
	public void start(Future<Void> startFuture){
		
		Router router = Router.router(vertx);
		LocalSessionStore sessionStore = LocalSessionStore.create(vertx);
		
		// Handlers to get request bodies and 
		// for cookies and sessions
		
	    router.route().handler(BodyHandler.create());
	    router.route().handler(CookieHandler.create());
	    router.route().handler(SessionHandler.create(sessionStore));
	    
		router.post("/Services/rest/blogs").handler(new BlogPost());
		router.get("/Services/rest/blogs").handler(new BlogGet());
		router.post("/Services/rest/blogs/:blogId/comments").handler(new BlogComment());

//		EventBus eb = vertx.eventBus();
//		eb.consumer("com.cisco.userInfo", message -> {
//			System.out.println("******************************* Received userInfo: "+message.body());
//		});

		// StaticHanlder for loading frontend angular app
		router.route().handler(StaticHandler.create()::handle);
		int port = 8082;
		vertx.createHttpServer().requestHandler(router::accept).listen(port);	
		System.out.println("BlogMicroVerticle verticle started: "+port);
		startFuture.complete();
	}
	
	@Override
	public void stop(Future<Void> stopFuture){
		System.out.println("BlogMicroVerticle stopped");
		stopFuture.complete();
	}
			

	class Credentials{
		String userName;
		String password;
		String first;
		String last;
		String id;
	
		
	}
	
	private boolean GetCredentials(final String authorization, Credentials credObject){
		boolean status = false;
		if (authorization != null && authorization.startsWith("Basic")) {
	        // Authorization: Basic base64credentials
	        String base64Credentials = authorization.substring("Basic".length()).trim();
	        String credentials = new String(Base64.getDecoder().decode(base64Credentials),
	                Charset.forName("UTF-8"));
	        // credentials = username:password
	        final String[] values = credentials.split(":",5);
	        credObject.userName = values[0];
	        credObject.password = values[1];
	        credObject.first = values[2];
	        credObject.last = values[3];
	        credObject.id = values[4];
	        System.out.println("user and password :"+values[0]+" " +values[1]+" "+values[2]+" " +values[3]+" "+values[4]);
	    }
		return status;
	}

class BlogComment implements Handler<RoutingContext> {
	public void handle(RoutingContext routingContext) {
		System.out.println("Thread BlogComment: "
				+ Thread.currentThread().getId());
		HttpServerResponse response = routingContext.response();
		String blogId = routingContext.request().getParam("blogId");
		System.out.println("blogId: " + blogId);

		Datastore dataStore = ServicesFactory.getMongoDB();
		// Get Request Body that contains login details
		HttpServerRequest request = routingContext.request();
		final String authorization = request.getHeader("Authorization");
		Credentials cred = new Credentials();
		GetCredentials(authorization,cred);
		System.out.println("userName :" + cred.userName + " password : " +cred.password);
		
		response.putHeader("content-type", "application/json");
	
		String json = routingContext.getBodyAsString();
		ObjectMapper mapper = new ObjectMapper();
		
		CommentDTO dto = null;
		try {
			dto = mapper.readValue(json, CommentDTO.class);
			
			if (cred.userName == null ) {
				System.out.println("No Valid user logged in");
				response.setStatusCode(401).end("No Valid user logged in");
			} else {
//				User user = dataStore.createQuery(User.class)
//						.field("userName").equal(cred.userName).get();
				dto.setUserFirst(cred.first);
				dto.setUserLast(cred.last);
				dto.setUserId(cred.id);
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
		Datastore dataStore = ServicesFactory.getMongoDB();

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
			BlogDTO dto = null;
			
			
			Datastore dataStore = ServicesFactory.getMongoDB();
			// Get Request Body that contains login details
			HttpServerRequest request = routingContext.request();
			final String authorization = request.getHeader("Authorization");
			Credentials cred = new Credentials();
			GetCredentials(authorization,cred);
			System.out.println("userName :" + cred.userName + " password : " +cred.password);
			
			try {
					dto = mapper.readValue(json, BlogDTO.class);
//					User user = dataStore.createQuery(User.class)
//							.field("userName").equal(cred.userName).get();
//					System.out.println(user);
					dto.setUserFirst(cred.first);
					dto.setUserLast(cred.last);
					dto.setUserId(cred.id);
					dto.setDate(new Date().getTime());
					Blog blog = dto.toModel();
					dataStore.save(blog);
					response.setStatusCode(204).end("Blog saved !!");
				
			} catch (IOException e) {
				e.printStackTrace();
			}		
		}
	}
}

