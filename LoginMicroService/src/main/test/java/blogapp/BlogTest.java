package blogapp;

import org.junit.Test;

import com.jayway.restassured.filter.session.SessionFilter;

import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;


//import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.given;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
//import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;

public class BlogTest {
	
	String baseURL = "http://localhost:8080";
	String userName = "cmad";
	String password = "password";

	SessionFilter sessionFilter = new SessionFilter();
	


	public void validateregister() {
		String url = baseURL + "/Services/rest/user/register";
		
		given()
		.filter(sessionFilter)
			.body("{\"id\":\"55716669eec5ca2b6ddf5629\", \"userName\":\"cmad\",\"password\":\"abc123\",\"email\":\"vinay@gmail.com\",\"first\":\"Vinay\",\"last\":\"Prasad\",\"companyId\":\"55716669eec5ca2b6ddf5626\",\"siteId\":\"55716669eec5ca2b6ddf5627\",\"deptId\":\"55716669eec5ca2b6ddf5628\"}")
		.when()
			.post(url)
		.then()
			.statusCode(204);
	}
	
	
	public void validatelogin() {
		String url = baseURL +"/Services/rest/user/auth";
		given()
		.filter(sessionFilter)
			.body("{\"userName\":\"cmad\",\"password\":\"abc123\"}")
		.when()
			.post(url)
		.then()
			.statusCode(204);

	
	}
	
	public void validatesignin() {
		
		String url = baseURL +"/Services/rest/user/auth";
		given()
		.filter(sessionFilter)
			.body("{\"userName\":\"cmad\",\"password\":\"abc123\"}")
		.when()
			.post(url)
		.then()
			.statusCode(204);

		
		 url = baseURL +"/Services/rest/blogs";
		
		

	}
	@Test
	public void test001_Auth(){
		
		validateregister();
		validatelogin();
		validatesignin();
		
		
	}
	@Test
	public void test002_LoginNegativeTest() {
		String url = baseURL +"/Services/rest/user/auth";
		given()
			.body("{\"userName\":\"invalidUser\",\"password\":\"abc123\"}")
		.when()
			.post(url)
		.then()
			.statusCode(404);
	}
	
//	@Test
//	public void test004_validatepostBlog() {
//		String url = baseURL +"/Services/rest/blogs";
//		
//		
//	
//		given()
////		.filter(new ResponseLoggingFilter())
//		.filter(sessionFilter)
////			.sessionId(sessionFilter.getSessionId())
//			.body("{\"content\":\"cmad Blog Content here....\",\"tags\":\"cmad\",\"title\":\"CMAD heading\"}")
//		.when()
//			.post(url)
//		.then()
//			.statusCode(204);
//	}
	@Test
	public void test003_validatecompanies() {
		
		String url = baseURL + "/Services/rest/company";
		
		given().
		when().
			get(url).
		then().
			statusCode(200).
			body("[0].id", equalTo("55716669eec5ca2b6ddf5626")).
			body("[1].id", equalTo("559e4331c203b4638a00ba1a"));
	}
}
