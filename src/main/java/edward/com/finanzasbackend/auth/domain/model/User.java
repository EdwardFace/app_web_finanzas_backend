package edward.com.finanzasbackend.auth.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false)
    private String email;
    @Column(nullable = true)
    private String password;
    @Column(nullable = false, length = 30)
    private String status;
    private String
}
