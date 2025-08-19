import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsNull.notNullValue;

public class PatientIntegrationTest {

    @BeforeAll
    static void setUp(){
        RestAssured.baseURI = "http://localhost:4004";
    }

    @Test
    public void shouldReturnPatientsWithValidToken(){
        // arrange
        // creating a json payload
        String loginPayload = """
                {
                    "email": "testuser@test.com",
                    "password": "password123"
                
                }
                """;

        // act
        String token = given()
                .contentType("application/json") // arrange
                .body(loginPayload) // arrange
                .when()
                .post("/auth/login") // action, make a post request
                .then()
                .statusCode(200) // asserting we get a http status code 200
                .extract()
                .jsonPath()
                .get("token");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/patients/all")
                .then()
                .statusCode(200)
                .body("patients", notNullValue()); // gets a list of patients in 'patients' variable
    }

}
