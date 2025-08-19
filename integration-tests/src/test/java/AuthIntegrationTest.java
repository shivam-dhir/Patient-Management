import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsNull.notNullValue;

public class AuthIntegrationTest {

    @BeforeAll
    static void setUp(){
        // api-gateway
        RestAssured.baseURI = "http://localhost:4004";
    }

    @Test
    public void shouldReturnOKWithValidToken(){
        // Steps of testing
        // 1. Arrange
        // 2. Act
        // 3. Assert

        // arrange
        // creating a json payload
        String loginPayload = """
                {
                    "email": "testuser@test.com",
                    "password": "password123"
                
                }
                """;

        // act
        Response response = given()
                .contentType("application/json") // arrange
                .body(loginPayload) // arrange
                .when()
                .post("/auth/login") // action, make a post request
                .then()
                .statusCode(200) // asserting we get a http status code 200
                .body("token", notNullValue()) // asserting token in not null
                .extract()
                .response();

        System.out.println("Generated Token: " + response.jsonPath().getString("token"));
    }

    @Test
    public void shouldReturnUnauthorizedOnInValidToken(){
        // arrange
        String loginPayload = """
                {
                    "email": "invalid_user@test.com",
                    "password": "invalid_password123"
                
                }
                """;

        // act
        given()
                .contentType("application/json") // arrange
                .body(loginPayload) // arrange
                .when()
                .post("/auth/login") // action, make a post request
                .then()
                .statusCode(401); // assert getting a 401 unauthorized status code
    }

}
