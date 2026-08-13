package user_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "jwt.secret=my-super-secret-key-my-super-secret-key",
    "spring.datasource.url=jdbc:mysql://localhost:3306/userdb_test?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.datasource.username=root",
    "spring.datasource.password=Balu@1998",  // <-- CHANGE THIS
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false"
})
class UserServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}